package com.shafii.encyclopedia.util

/**
 * Arabic text normalization & FTS5 query builder.
 * Mirrors database/build_database.py so offline FTS5 search handles:
 * - Optional diacritics (تشكيل) and Tatweel (ـ)
 * - Alef / Hamzah variations (أ إ آ ٱ -> ا)
 * - Ya / Alef Maqsura & Ta Marbuta variations (ى ئ -> ي, ة -> ه, ؤ -> و)
 * - Partial word prefix matching via FTS5 '*' wildcard
 */
object ArabicNormalizer {

    private val diacriticsRegex = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]")
    private val nonWordRegex = Regex("[^\\p{L}\\p{N}\\s]")
    private val multiSpaceRegex = Regex("\\s+")

    fun normalize(input: String): String {
        if (input.isBlank()) return ""
        return input
            .replace(diacriticsRegex, "")
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ٱ', 'ا')
            .replace('ى', 'ي')
            .replace('ئ', 'ي')
            .replace('ؤ', 'و')
            .replace('ة', 'ه')
            .replace(nonWordRegex, " ")
            .replace(multiSpaceRegex, " ")
            .trim()
    }

    /**
     * Builds a safe FTS5 MATCH query supporting partial words and optional column scoping.
     */
    fun buildFts5Query(rawQuery: String, columnPrefix: String? = null): String {
        val normalized = normalize(rawQuery)
        if (normalized.isBlank()) return ""
        val tokens = normalized.split(" ").filter { it.isNotBlank() }
        if (tokens.isEmpty()) return ""
        return tokens.joinToString(" AND ") { token ->
            val safe = token.replace("\"", "")
            if (columnPrefix.isNullOrBlank()) "$safe*" else "$columnPrefix:$safe*"
        }
    }
}
