package com.aichat.core.common.util

object TextCleaner {

    private val separatorRegex = Regex("^[\\s*#@=\\-_~‾\\.]+$")

    /**
     * Cleans text for API consumption:
     * - Removes lines that are only separators (---, ***, ####, etc.)
     * - Removes empty lines
     * - Replaces all \n with spaces
     * - Collapses multiple spaces into one
     * - Trims
     */
    fun clean(text: String): String {
        if (text.isBlank()) return ""

        // 1. Split by lines, filter separators and blanks
        val cleanedLines = text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !separatorRegex.matches(it) }

        // 2. Join with space, collapse multiple spaces
        return cleanedLines.joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Returns true if after cleaning the text is empty.
     */
    fun isEmptyAfterClean(text: String): Boolean = clean(text).isEmpty()
}