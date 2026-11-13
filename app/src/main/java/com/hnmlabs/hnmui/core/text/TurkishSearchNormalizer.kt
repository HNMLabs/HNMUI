package com.hnmlabs.hnmui.core.text

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

    fun containsMatch(target: String, rawQuery: String): Boolean {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) return true
        val qFolded = foldTurkishI(trimmed)
        return if (hasExplicitTurkishDiacritics(qFolded)) {
            foldTurkishI(target).contains(qFolded)
        } else {
            foldAscii(target).contains(foldAscii(trimmed))
        }
    }

    fun startsWithMatch(target: String, rawQuery: String): Boolean {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) return true
        val qFolded = foldTurkishI(trimmed)
        return if (hasExplicitTurkishDiacritics(qFolded)) {
            foldTurkishI(target).startsWith(qFolded)
        } else {
            foldAscii(target).startsWith(foldAscii(trimmed))
        }
    }
}
