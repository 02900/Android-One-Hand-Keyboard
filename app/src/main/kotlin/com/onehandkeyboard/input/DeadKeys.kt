package com.onehandkeyboard.input

/**
 * Dead-key composition for Spanish (and other Latin) accents.
 */
object DeadKeys {
    const val ACUTE = '´'
    const val DIAERESIS = '¨'

    private val table: Map<Char, Map<Char, Char>> =
        mapOf(
            ACUTE to mapOf('a' to 'á', 'e' to 'é', 'i' to 'í', 'o' to 'ó', 'u' to 'ú'),
            DIAERESIS to mapOf('a' to 'ä', 'e' to 'ë', 'i' to 'ï', 'o' to 'ö', 'u' to 'ü'),
        )

    /**
     * Returns the text produced by typing [input] after the dead key [accent]:
     * the accented letter when it exists (keeping the case of [input]), otherwise the accent
     * followed by [input].
     */
    fun compose(
        accent: Char,
        input: String,
    ): String {
        val char = input.singleOrNull() ?: return accent + input
        val composed = table[accent]?.get(char.lowercaseChar()) ?: return accent + input
        return if (char.isUpperCase()) composed.uppercase() else composed.toString()
    }

    fun canCompose(
        accent: Char,
        char: Char,
    ): Boolean = table[accent]?.containsKey(char.lowercaseChar()) == true
}
