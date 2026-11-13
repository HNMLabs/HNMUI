package com.hnmlabs.hnmui.core.text

import androidx.compose.runtime.Immutable

/**
 * Her tuş vuruşunda sorguyu yalnızca 1 kez normalize ederek yüzlerce uygulama üzerinde
 * sıfır StringBuilder tahsisiyle (zero-allocation) O(N) eşleşme sağlayan hazır sorgu nesnesi.
 */
@Immutable
data class PreparedSearchQuery(
    val isEmpty: Boolean,
    val useTurkishDiacritics: Boolean,
    val queryKey: String
)

/**
 * Türkçe ve İngilizce uygulama adları / paket isimleri arasında 'I-ı / İ-i' noktalı-noktasız
 * harf uyuşmazlığını (ör. "Instagram" -> "ınstagram", "InShot" -> "ınshot", "İşCep" -> "i̇şcep")
 * ve hızlı ASCII klavye aramalarını ("iscep" -> "İşCep", "izgara" -> "ızgara") çözen saf Kotlin arama motoru.
 */
object TurkishSearchNormalizer {

    /**
     * 'I', 'İ', 'ı', 'i' ve birleştirici nokta (\u0307) karakterlerini tekil 'i' harfinde birleştirir;
     * Türkçe karakterleri (ş, ğ, ü, ö, ç) koruyarak küçük harfe çevirir.
     */
    fun foldTurkishI(input: String): String {
        if (input.isEmpty()) return ""
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch == '\u0307') continue
            val mapped = when (ch) {
                'I', 'İ', 'ı', 'i', 'Î', 'î' -> 'i'
                'Ş', 'ş' -> 'ş'
                'Ğ', 'ğ' -> 'ğ'
                'Ü', 'ü', 'Û', 'û' -> 'ü'
                'Ö', 'ö' -> 'ö'
                'Ç', 'ç' -> 'ç'
                'Â', 'â' -> 'a'
                else -> ch.lowercaseChar()
            }
            sb.append(mapped)
        }
        return sb.toString()
    }

    /**
     * Tüm Türkçe aksanlı harfleri ASCII karşılıklarına indirger ("İşCep" -> "iscep", "Kağıdı" -> "kagidi").
     */
    fun foldAscii(input: String): String {
        if (input.isEmpty()) return ""
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch == '\u0307') continue
            val mapped = when (ch) {
                'I', 'İ', 'ı', 'i', 'Î', 'î' -> 'i'
                'Ş', 'ş' -> 's'
                'Ğ', 'ğ' -> 'g'
                'Ü', 'ü', 'Û', 'û' -> 'u'
                'Ö', 'ö' -> 'o'
                'Ç', 'ç' -> 'c'
                'Â', 'â' -> 'a'
                else -> ch.lowercaseChar()
            }
            sb.append(mapped)
        }
        return sb.toString()
    }

    private fun hasExplicitTurkishDiacritics(foldedQuery: String): Boolean {
        for (ch in foldedQuery) {
            if (ch == 'ş' || ch == 'ğ' || ch == 'ü' || ch == 'ö' || ch == 'ç') return true
        }
        return false
    }

    fun prepareQuery(rawQuery: String): PreparedSearchQuery {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) {
            return EmptyPreparedQuery
        }
        val qFolded = foldTurkishI(trimmed)
        val hasDiacritics = hasExplicitTurkishDiacritics(qFolded)
        return PreparedSearchQuery(
            isEmpty = false,
            useTurkishDiacritics = hasDiacritics,
            queryKey = if (hasDiacritics) qFolded else foldAscii(trimmed)
        )
    }

    fun containsPreFolded(
        foldedTarget: String,
        asciiTarget: String,
        prepared: PreparedSearchQuery
    ): Boolean {
        if (prepared.isEmpty) return true
        return if (prepared.useTurkishDiacritics) {
            foldedTarget.contains(prepared.queryKey)
        } else {
            asciiTarget.contains(prepared.queryKey)
        }
    }

    fun startsWithPreFolded(
        foldedTarget: String,
        asciiTarget: String,
        prepared: PreparedSearchQuery
    ): Boolean {
        if (prepared.isEmpty) return true
        return if (prepared.useTurkishDiacritics) {
            foldedTarget.startsWith(prepared.queryKey)
        } else {
            asciiTarget.startsWith(prepared.queryKey)
        }
    }

    fun containsMatch(target: String, rawQuery: String): Boolean {
        val prepared = prepareQuery(rawQuery)
        if (prepared.isEmpty) return true
        return if (prepared.useTurkishDiacritics) {
            foldTurkishI(target).contains(prepared.queryKey)
        } else {
            foldAscii(target).contains(prepared.queryKey)
        }
    }

    fun startsWithMatch(target: String, rawQuery: String): Boolean {
        val prepared = prepareQuery(rawQuery)
        if (prepared.isEmpty) return true
        return if (prepared.useTurkishDiacritics) {
            foldTurkishI(target).startsWith(prepared.queryKey)
        } else {
            foldAscii(target).startsWith(prepared.queryKey)
        }
    }

    private val EmptyPreparedQuery = PreparedSearchQuery(
        isEmpty = true,
        useTurkishDiacritics = false,
        queryKey = ""
    )
}
