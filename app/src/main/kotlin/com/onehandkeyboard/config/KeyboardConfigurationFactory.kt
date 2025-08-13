package com.onehandkeyboard.config

import com.onehandkeyboard.actions.ActionConfig
import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.GestureType
import com.onehandkeyboard.core.domain.model.KeyAction
import com.onehandkeyboard.enums.Language
import com.onehandkeyboard.enums.LayoutMode
import com.onehandkeyboard.gestures.GestureAction

/**
 * Factory para crear configuraciones de teclado específicas por idioma y modo
 */
object KeyboardConfigurationFactory {
    /**
     * Obtiene la configuración para un idioma y modo específicos
     */
    fun getConfiguration(
        language: Language,
        mode: LayoutMode,
    ): KeyboardConfiguration {
        return when (language) {
            Language.SPANISH ->
                when (mode) {
                    LayoutMode.LETTERS -> createSpanishLetters()
                    LayoutMode.SYMBOLS -> createSpanishSymbols()
                }
            Language.ENGLISH ->
                when (mode) {
                    LayoutMode.LETTERS -> createEnglishLetters()
                    LayoutMode.SYMBOLS -> createEnglishSymbols()
                }
        }
    }

    /**
     * Configuración por defecto para layout español (mantener compatibilidad)
     */
    fun createSpanishDefault(): KeyboardConfiguration {
        return createSpanishLetters()
    }

    /**
     * Configuración española - modo letras
     */
    private fun createSpanishLetters(): KeyboardConfiguration {
        return KeyboardConfiguration(
            name = "Español - Letras",
            description = "Layout español con letras y acentos",
            language = Language.SPANISH,
            mode = LayoutMode.LETTERS,
            cells =
                listOf(
                    CellConfiguration(
                        cellIndex = 0,
                        actions =
                            mapOf(
                                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('a'),
                                GestureAction(GestureType.SWIPE, Direction.SE) to ActionConfig.Character('v'),
                            ),
                    ),
                    CellConfiguration(
                        cellIndex = 1,
                        actions =
                            mapOf(
                                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('n'),
                                GestureAction(GestureType.SWIPE, Direction.S) to ActionConfig.Character('l'),
                            ),
                    ),
                    CellConfiguration(
                        cellIndex = 2,
                        actions =
                            mapOf(
                                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('i'),
                                GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Character('?'),
                                GestureAction(GestureType.SWIPE, Direction.SW) to ActionConfig.Character('x'),
                            ),
                    ),
                    CellConfiguration(
                        cellIndex = 3,
                        actions =
                            mapOf(
                                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('d'),
                                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('k'),
                            ),
                    ),
                    CellConfiguration(
                        cellIndex = 4,
                        actions =
                            mapOf(
                                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('o'),
                                GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('u'),
                                GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('p'),
                                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('b'),
                                GestureAction(GestureType.SWIPE, Direction.SE) to ActionConfig.Character('j'),
                                GestureAction(GestureType.SWIPE, Direction.S) to ActionConfig.Character('h'),
                                GestureAction(GestureType.SWIPE, Direction.SW) to ActionConfig.Character('g'),
                                GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Character('c'),
                                GestureAction(GestureType.SWIPE, Direction.NW) to ActionConfig.Character('q'),
                            ),
                    ),
                    CellConfiguration(
                        cellIndex = 5,
                        actions =
                            mapOf(
                                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('r'),
                                GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Character('m'),
                            ),
                    ),
                    CellConfiguration(
                        cellIndex = 6,
                        actions =
                            mapOf(
                                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('t'),
                                GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('y'),
                            ),
                    ),
                    CellConfiguration(
                        cellIndex = 7,
                        actions =
                            mapOf(
                                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('e'),
                                GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('w'),
                                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('z'),
                            ),
                    ),
                    CellConfiguration(
                        cellIndex = 8,
                        actions =
                            mapOf(
                                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('s'),
                                GestureAction(GestureType.SWIPE, Direction.NW) to ActionConfig.Character('f'),
                            ),
                    ),
                ),
        )
    }

    /**
     * Configuración española - modo símbolos
     */
    private fun createSpanishSymbols(): KeyboardConfiguration {
        return KeyboardConfiguration(
            name = "Español - Símbolos",
            description = "Layout español con números y símbolos",
            language = Language.SPANISH,
            mode = LayoutMode.SYMBOLS,
            cells =
                listOf(
                    CellConfiguration(
                        cellIndex = 0,
                        actions =
                            mapOf(
                                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('1'),
                                GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('2'),
                                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('3'),
                                GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('!'),
                                GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Character('@'),
                                GestureAction(GestureType.SWIPE, Direction.S) to ActionConfig.Character('#'),
                            ),
                    ),
                    CellConfiguration(
                        cellIndex = 1,
                        actions =
                            mapOf(
                                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('4'),
                                GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('5'),
                                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('6'),
                                GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('$'),
                                GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Character('%'),
                                GestureAction(GestureType.SWIPE, Direction.S) to ActionConfig.Character('^'),
                            ),
                    ),
                    CellConfiguration(
                        cellIndex = 2,
                        actions =
                            mapOf(
                                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('7'),
                                GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('8'),
                                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('9'),
                                GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('&'),
                                GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Character('*'),
                                GestureAction(GestureType.SWIPE, Direction.S) to ActionConfig.Character('('),
                            ),
                    ),
                    CellConfiguration(
                        cellIndex = 3,
                        actions =
                            mapOf(
                                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('+'),
                                GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('-'),
                                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('='),
                                GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('_'),
                                GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Character('['),
                                GestureAction(GestureType.SWIPE, Direction.S) to ActionConfig.Character(']'),
                            ),
                    ),
                    CellConfiguration(
                        cellIndex = 4,
                        actions =
                            mapOf(
                                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('0'),
                                GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character(')'),
                                GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('{'),
                                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('}'),
                                GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Character('|'),
                                GestureAction(GestureType.SWIPE, Direction.S) to ActionConfig.Character('\\'),
                                GestureAction(GestureType.SWIPE, Direction.SW) to ActionConfig.Character('~'),
                            ),
                    ),
                    CellConfiguration(
                        cellIndex = 5,
                        actions =
                            mapOf(
                                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('.'),
                                GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character(','),
                                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('?'),
                                GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('¿'),
                                GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Character('¡'),
                                GestureAction(GestureType.SWIPE, Direction.S) to ActionConfig.Character(';'),
                            ),
                    ),
                    CellConfiguration(
                        cellIndex = 6,
                        actions =
                            mapOf(
                                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('"'),
                                GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('\''),
                                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character(':'),
                                GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('<'),
                                GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Character('>'),
                                GestureAction(GestureType.SWIPE, Direction.S) to ActionConfig.Character('/'),
                            ),
                    ),
                    CellConfiguration(
                        cellIndex = 7,
                        actions =
                            mapOf(
                                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Action(KeyAction.Space),
                                GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('`'),
                                GestureAction(GestureType.SWIPE, Direction.S) to ActionConfig.Action(KeyAction.Space),
                                GestureAction(GestureType.LONG_PRESS, Direction.CENTER) to ActionConfig.Action(KeyAction.Space),
                            ),
                    ),
                    CellConfiguration(
                        cellIndex = 8,
                        actions =
                            mapOf(
                                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Action(KeyAction.Backspace),
                                GestureAction(GestureType.SWIPE, Direction.S) to ActionConfig.Action(KeyAction.Enter),
                                GestureAction(GestureType.LONG_PRESS, Direction.CENTER) to ActionConfig.Action(KeyAction.Backspace),
                            ),
                    ),
                ),
        )
    }

    /**
     * Configuración inglesa - modo letras
     */
    private fun createEnglishLetters(): KeyboardConfiguration {
        return KeyboardConfiguration(
            name = "English - Letters",
            description = "English layout with letters",
            language = Language.ENGLISH,
            mode = LayoutMode.LETTERS,
            cells =
                listOf(
                    CellConfiguration(
                        cellIndex = 0,
                        actions =
                            mapOf(
                                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('a'),
                                GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('b'),
                                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('c'),
                            ),
                    ),
                    CellConfiguration(
                        cellIndex = 1,
                        actions =
                            mapOf(
                                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('d'),
                                GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('e'),
                                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('f'),
                            ),
                    ),
                    CellConfiguration(
                        cellIndex = 2,
                        actions =
                            mapOf(
                                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('g'),
                                GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('h'),
                                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('i'),
                            ),
                    ),
                    CellConfiguration(
                        cellIndex = 3,
                        actions =
                            mapOf(
                                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('j'),
                                GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('k'),
                                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('l'),
                            ),
                    ),
                    CellConfiguration(
                        cellIndex = 4,
                        actions =
                            mapOf(
                                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('m'),
                                GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('m'),
                                GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('n'),
                                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('o'),
                            ),
                    ),
                    CellConfiguration(
                        cellIndex = 5,
                        actions =
                            mapOf(
                                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('p'),
                                GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('q'),
                                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('r'),
                            ),
                    ),
                    CellConfiguration(
                        cellIndex = 6,
                        actions =
                            mapOf(
                                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('s'),
                                GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('t'),
                                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('u'),
                            ),
                    ),
                    CellConfiguration(
                        cellIndex = 7,
                        actions =
                            mapOf(
                                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('v'),
                                GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('w'),
                                GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('x'),
                            ),
                    ),
                    CellConfiguration(
                        cellIndex = 8,
                        actions =
                            mapOf(
                                GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('y'),
                                GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('z'),
                                GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Action(KeyAction.Backspace),
                                GestureAction(GestureType.LONG_PRESS, Direction.CENTER) to ActionConfig.Action(KeyAction.Backspace),
                            ),
                    ),
                ),
        )
    }

    /**
     * Configuración inglesa - modo símbolos (reutiliza la española)
     */
    private fun createEnglishSymbols(): KeyboardConfiguration {
        return createSpanishSymbols().copy(
            name = "English - Symbols",
            description = "English layout with numbers and symbols",
            language = Language.ENGLISH,
        )
    }

    /**
     * Configuración personalizable vacía
     */
    fun createEmpty(): KeyboardConfiguration {
        return KeyboardConfiguration(
            name = "Personalizado",
            description = "Configuración personalizable",
            language = Language.SPANISH,
            mode = LayoutMode.LETTERS,
            cells =
                (0..8).map { index ->
                    CellConfiguration(
                        cellIndex = index,
                        actions = emptyMap(),
                    )
                },
        )
    }
}
