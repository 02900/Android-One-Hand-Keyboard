package com.onehandkeyboard.enums

/**
 * Idiomas soportados por el teclado
 */
enum class Language(
    /** Short label shown on the keyboard. */
    val label: String,
    /** Offered to the user. A disabled language keeps its layout and dictionary, just unreachable. */
    val enabled: Boolean = true,
) {
    SPANISH("ES"),

    // Disabled until its letter layout is final (also remove it from res/xml/method.xml).
    ENGLISH("EN", enabled = false),
    ;

    /** The next enabled language (the space bar swipe cycles through them); itself if it is the only one. */
    fun other(): Language {
        val available = available()
        return available[(available.indexOf(this) + 1) % available.size]
    }

    companion object {
        fun available(): List<Language> = entries.filter { it.enabled }

        /** The space bar swipe only switches language when there is more than one. */
        val canSwitch: Boolean get() = available().size > 1
    }
}
