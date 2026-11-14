package com.hnmlabs.hnmui.core.text

import androidx.compose.runtime.Immutable
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow

@Immutable
data class SpotlightMathResult(
    val normalizedExpression: String,
    val value: Double,
    val formattedResult: String,
    val copyableValue: String
)

/**
 * Spotlight Arama için %100 çevrimdışı, sıfır gecikmeli matematik ve yüzde hesaplama motoru.
 * - Dört işlem (+, -, *, ×, /, ÷), üs alma (^), parantez önceliği ve KDV/indirim yüzdelerini
 *   (hem Türkçe `%20` önek hem `20%` sonek yazımı: `1250 + %20` -> `1.500`, `800 - 15%` -> `680`)
 *   destekler.
 * - Yalnızca gerçek bir işlem operatörü içeren geçerli ifadelerde sonuç döndürür;
 *   tekil sayılar (`2026`, `120`) veya uygulama adları için `null` döner.
 */
object SpotlightMathEngine {

    private const val MAX_QUERY_LENGTH = 96
    private const val MAX_PAREN_DEPTH = 24

    fun evaluate(rawQuery: String): SpotlightMathResult? {
        val trimmed = rawQuery.trim().removeSuffix("=").trim()
        if (trimmed.length < 2 || trimmed.length > MAX_QUERY_LENGTH) return null

        val tokens = tokenize(trimmed) ?: return null
        if (!containsEffectiveCalculation(tokens)) return null

        val parser = Parser(tokens)
        val result = parser.parseExpression() ?: return null
        if (!parser.isAtEnd || !result.isFinite()) return null

        val cleanZero = if (abs(result) < 1e-11) 0.0 else result
        return SpotlightMathResult(
            normalizedExpression = formatExpressionPreview(tokens),
            value = cleanZero,
            formattedResult = formatTurkishNumber(cleanZero),
            copyableValue = formatPlainCopyValue(cleanZero)
        )
    }

    private sealed interface Token {
        data class NumberVal(val value: Double, val isPercent: Boolean, val rawText: String) : Token
        data class Op(val symbol: Char) : Token
        data object LParen : Token
        data object RParen : Token
    }

    private fun containsEffectiveCalculation(tokens: List<Token>): Boolean {
        val numberCount = tokens.count { it is Token.NumberVal }
        if (numberCount == 0) return false
        val hasPercent = tokens.any { it is Token.NumberVal && it.isPercent }
        if (numberCount >= 2 && tokens.any { it is Token.Op }) return true
        // Tek başına "+90" veya "-42" telefon kodu/sayıdır; hesaplama kartı açma
        if (numberCount == 1 && hasPercent && tokens.size == 1) return false
        return numberCount >= 2 && hasPercent
    }

    private fun tokenize(input: String): List<Token>? {
        val out = ArrayList<Token>(16)
        var i = 0
        val n = input.length

        while (i < n) {
            val ch = input[i]
            when {
                ch.isWhitespace() -> i++

                ch == '(' -> {
                    // Örtük çarpım: `2(3+4)` veya `)(` -> `*` ekle
                    val prev = out.lastOrNull()
                    if (prev is Token.NumberVal || prev is Token.RParen) {
                        out += Token.Op('*')
                    }
                    out += Token.LParen
                    i++
                }

                ch == ')' -> {
                    out += Token.RParen
                    i++
                }

                ch == '+' || ch == '-' || ch == '*' || ch == '×' || ch == '/' || ch == '÷' || ch == '^' -> {
                    val norm = when (ch) {
                        '×' -> '*'
                        '÷' -> '/'
                        else -> ch
                    }
                    out += Token.Op(norm)
                    i++
                }

                (ch == 'x' || ch == 'X') -> {
                    // Yalnızca sayı/parantez arasında çarpma operatörü olarak kabul et (`7x7`, `(2+3)x4`)
                    val prev = out.lastOrNull()
                    val prevValid = prev is Token.NumberVal || prev is Token.RParen
                    var lookahead = i + 1
                    while (lookahead < n && input[lookahead].isWhitespace()) lookahead++
                    val nextChar = input.getOrNull(lookahead)
                    val nextValid = nextChar != null && (
                        nextChar.isDigit() || nextChar == '(' || nextChar == '%' || nextChar == '-' || nextChar == '+'
                        )
                    if (!prevValid || !nextValid) return null
                    out += Token.Op('*')
                    i++
                }

                ch == '%' -> {
                    // Türkçe önek yüzde yazımı: `%20`
                    i++
                    while (i < n && input[i].isWhitespace()) i++
                    val numStart = i
                    var dotCount = 0
                    var commaCount = 0
                    while (i < n && (input[i].isDigit() || input[i] == '.' || input[i] == ',')) {
                        if (input[i] == '.') dotCount++
                        if (input[i] == ',') commaCount++
                        i++
                    }
                    if (i == numStart || dotCount + commaCount > 1) return null
                    val rawNum = input.substring(numStart, i)
                    val parsed = rawNum.replace(',', '.').toDoubleOrNull() ?: return null
                    out += Token.NumberVal(value = parsed, isPercent = true, rawText = "%$rawNum")
                }

                ch.isDigit() || ch == '.' || ch == ',' -> {
                    val prev = out.lastOrNull()
                    if (prev is Token.RParen) {
                        out += Token.Op('*')
                    }
                    val numStart = i
                    var dotCount = 0
                    var commaCount = 0
                    while (i < n && (input[i].isDigit() || input[i] == '.' || input[i] == ',')) {
                        if (input[i] == '.') dotCount++
                        if (input[i] == ',') commaCount++
                        i++
                    }
                    if (dotCount + commaCount > 1) return null
                    val rawNum = input.substring(numStart, i)
                    val parsed = rawNum.replace(',', '.').toDoubleOrNull() ?: return null

                    var wsIdx = i
                    while (wsIdx < n && input[wsIdx].isWhitespace()) wsIdx++
                    val hasTrailingPercent = wsIdx < n && input[wsIdx] == '%'
                    if (hasTrailingPercent) {
                        i = wsIdx + 1
                    }
                    out += Token.NumberVal(
                        value = parsed,
                        isPercent = hasTrailingPercent,
                        rawText = if (hasTrailingPercent) "$rawNum%" else rawNum
                    )
                }

                else -> return null
            }
        }
        return out
    }

    private data class EvalNode(val value: Double, val isStandalonePercent: Boolean)

    private class Parser(private val tokens: List<Token>) {
        private var pos = 0
        private var depth = 0

        val isAtEnd: Boolean get() = pos >= tokens.size

        fun parseExpression(): Double? = parseAdditive()?.let { node ->
            if (node.isStandalonePercent) node.value / 100.0 else node.value
        }

        private fun parseAdditive(): EvalNode? {
            var leftNode = parseMultiplicative() ?: return null
            while (pos < tokens.size) {
                val op = (tokens[pos] as? Token.Op)?.symbol ?: break
                if (op != '+' && op != '-') break
                pos++
                val rightNode = parseMultiplicative() ?: return null
                val leftVal = if (leftNode.isStandalonePercent) {
                    leftNode.value / 100.0
                } else {
                    leftNode.value
                }
                val combined = if (rightNode.isStandalonePercent) {
                    // KDV / İndirim semantiği: `1000 + %20` -> `1000 + (1000 * 0.20)`
                    val delta = leftVal * (rightNode.value / 100.0)
                    if (op == '+') leftVal + delta else leftVal - delta
                } else {
                    if (op == '+') leftVal + rightNode.value else leftVal - rightNode.value
                }
                if (!combined.isFinite()) return null
                leftNode = EvalNode(combined, isStandalonePercent = false)
            }
            return leftNode
        }

        private fun parseMultiplicative(): EvalNode? {
            var leftNode = parsePower() ?: return null
            while (pos < tokens.size) {
                val op = (tokens[pos] as? Token.Op)?.symbol ?: break
                if (op != '*' && op != '/') break
                pos++
                val rightNode = parsePower() ?: return null
                val lhs = if (leftNode.isStandalonePercent) leftNode.value / 100.0 else leftNode.value
                val rhs = if (rightNode.isStandalonePercent) rightNode.value / 100.0 else rightNode.value
                if (op == '/' && abs(rhs) < 1e-15) return null
                val combined = if (op == '*') lhs * rhs else lhs / rhs
                if (!combined.isFinite()) return null
                leftNode = EvalNode(combined, isStandalonePercent = false)
            }
            return leftNode
        }

        private fun parsePower(): EvalNode? {
            val baseNode = parseUnary() ?: return null
            val op = (tokens.getOrNull(pos) as? Token.Op)?.symbol
            if (op == '^') {
                pos++
                val expNode = parsePower() ?: return null
                val baseVal = if (baseNode.isStandalonePercent) baseNode.value / 100.0 else baseNode.value
                val expVal = if (expNode.isStandalonePercent) expNode.value / 100.0 else expNode.value
                if (abs(expVal) > 1024.0) return null
                val powered = baseVal.pow(expVal)
                if (!powered.isFinite()) return null
                return EvalNode(powered, isStandalonePercent = false)
            }
            return baseNode
        }

        private fun parseUnary(): EvalNode? {
            val tok = tokens.getOrNull(pos) ?: return null
            if (tok is Token.Op && (tok.symbol == '+' || tok.symbol == '-')) {
                pos++
                val inner = parseUnary() ?: return null
                val signed = if (tok.symbol == '-') -inner.value else inner.value
                return EvalNode(signed, isStandalonePercent = inner.isStandalonePercent)
            }
            return parsePrimary()
        }

        private fun parsePrimary(): EvalNode? {
            val tok = tokens.getOrNull(pos) ?: return null
            return when (tok) {
                is Token.NumberVal -> {
                    pos++
                    EvalNode(value = tok.value, isStandalonePercent = tok.isPercent)
                }

                is Token.LParen -> {
                    if (depth >= MAX_PAREN_DEPTH) return null
                    depth++
                    pos++
                    val inside = parseExpression() ?: return null
                    if (tokens.getOrNull(pos) !is Token.RParen) return null
                    pos++
                    depth--
                    EvalNode(value = inside, isStandalonePercent = false)
                }

                else -> null
            }
        }
    }

    private fun formatExpressionPreview(tokens: List<Token>): String {
        val sb = StringBuilder()
        tokens.forEachIndexed { idx, token ->
            when (token) {
                is Token.NumberVal -> sb.append(token.rawText)
                is Token.LParen -> sb.append('(')
                is Token.RParen -> sb.append(')')
                is Token.Op -> {
                    val prev = tokens.getOrNull(idx - 1)
                    val isUnary = (token.symbol == '-' || token.symbol == '+') &&
                        (prev == null || prev is Token.Op || prev is Token.LParen)
                    val displayOp = when (token.symbol) {
                        '*' -> "×"
                        '/' -> "÷"
                        '-' -> if (isUnary) "-" else "−"
                        else -> token.symbol.toString()
                    }
                    if (isUnary) {
                        sb.append(displayOp)
                    } else {
                        sb.append(' ').append(displayOp).append(' ')
                    }
                }
            }
        }
        return sb.toString().trim()
    }

    private fun formatTurkishNumber(value: Double): String {
        if (!value.isFinite()) return "0"
        val absVal = abs(value)
        if (absVal >= 1e12 || (absVal > 0.0 && absVal < 1e-6)) {
            return String.format(Locale("tr", "TR"), "%.4E", value)
        }
        val rounded = BigDecimal.valueOf(value)
            .setScale(6, RoundingMode.HALF_UP)
            .stripTrailingZeros()
        val symbols = DecimalFormatSymbols(Locale("tr", "TR")).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        val df = DecimalFormat("#,##0.######", symbols)
        return df.format(rounded)
    }

    private fun formatPlainCopyValue(value: Double): String {
        if (!value.isFinite()) return "0"
        val absVal = abs(value)
        if (absVal >= 1e12 || (absVal > 0.0 && absVal < 1e-6)) {
            return value.toString()
        }
        return BigDecimal.valueOf(value)
            .setScale(6, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    }
}
