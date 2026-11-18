package com.onehandkeyboard.spell

/**
 * The word right before the cursor and what surrounds it.
 *
 * @property word letters (and inner apostrophes) immediately before the cursor; empty if none.
 * @property mayAutoCorrect false when the word looks like something the user meant literally:
 *   glued to a digit or symbol (user@mail, #tag, 3er), or capitalized in the middle of a
 *   sentence (probably a name).
 */
data class WordContext(
    val word: String,
    val mayAutoCorrect: Boolean,
) {
    companion object {
        private const val OPENING = "¿¡(\"'«“‘"
        private const val QUOTES = "(\"'«“‘"
        private const val SENTENCE_END = ".!?¿¡\n"

        /**
         * @param before text before the cursor.
         * @param after first character after the cursor, if any (a letter means the cursor is
         *   inside a word, which is never corrected).
         */
        fun of(
            before: CharSequence,
            after: CharSequence?,
        ): WordContext? {
            if (!after.isNullOrEmpty() && after[0].isLetterOrDigit()) return null
            val start = wordStart(before)
            val word = before.substring(start)
            val previous = before.getOrNull(start - 1)
            val standalone = previous == null || previous.isWhitespace() || previous in OPENING
            val looksLikeName = isCapitalized(word) && !isSentenceStart(before.substring(0, start))
            return WordContext(word, word.isNotEmpty() && standalone && !looksLikeName)
        }

        private fun wordStart(before: CharSequence): Int {
            var start = before.length
            while (start > 0 && isWordChar(before[start - 1])) start--
            while (start < before.length && before[start] == '\'') start++
            return start
        }

        /** "Gonzalo" but not "GONZALO" (all caps is emphasis, not a name). */
        private fun isCapitalized(word: String) =
            word.firstOrNull()?.isUpperCase() == true && word.any { it.isLetter() && it.isLowerCase() }

        private fun isSentenceStart(textBefore: String): Boolean {
            val trimmed = textBefore.trimEnd { it.isWhitespace() || it in QUOTES }
            return trimmed.isEmpty() || trimmed.last() in SENTENCE_END
        }

        private fun isWordChar(c: Char) = c.isLetter() || c == '\''
    }
}
