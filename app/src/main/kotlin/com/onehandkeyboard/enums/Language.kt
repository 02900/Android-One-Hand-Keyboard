package com.onehandkeyboard.enums

/**
 * Idiomas soportados por el teclado
 */
enum class Language(
    /** Short label shown on the keyboard. */
    val label: String,
) {
    SPANISH("ES"),
    ENGLISH("EN"),
    ;

    /** The other language (the space bar swipe toggles between both). */
    fun other(): Language = if (this == SPANISH) ENGLISH else SPANISH
}
