package com.onehandkeyboard.config

import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.GestureType
import com.onehandkeyboard.core.domain.model.KeyAction

/**
 * Idiomas soportados por el teclado
 */
enum class Language {
    SPANISH,
    ENGLISH
}

/**
 * Modos de layout para cada idioma
 */
enum class LayoutMode {
    LETTERS,
    SYMBOLS
}

/**
 * Configuración externa y flexible para las acciones de cada celda del teclado
 */
data class KeyboardConfiguration(
    val name: String,
    val description: String,
    val language: Language,
    val mode: LayoutMode,
    val cells: List<CellConfiguration>
) {
    companion object {
        /**
         * Obtiene la configuración para un idioma y modo específicos
         */
        fun getConfiguration(language: Language, mode: LayoutMode): KeyboardConfiguration {
            return when (language) {
                Language.SPANISH -> when (mode) {
                    LayoutMode.LETTERS -> createSpanishLetters()
                    LayoutMode.SYMBOLS -> createSpanishSymbols()
                }
                Language.ENGLISH -> when (mode) {
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
                cells = listOf(
                    // Celda 0 (ABC)
                    CellConfiguration(
                        cellIndex = 0,
                        label = "ABC",
                        actions = mapOf(
                            GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('a'),
                            GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('á'),
                            GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('b'),
                            GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('c'),
                            GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Character('à'),
                            GestureAction(GestureType.SWIPE, Direction.S) to ActionConfig.Character('â')
                        )
                    ),
                    
                    // Celda 1 (DEF)
                    CellConfiguration(
                        cellIndex = 1,
                        label = "DEF",
                        actions = mapOf(
                            GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('d'),
                            GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('é'),
                            GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('e'),
                            GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('f'),
                            GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Character('è'),
                            GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('ê')
                        )
                    ),
                    
                    // Celda 2 (GHI)
                    CellConfiguration(
                        cellIndex = 2,
                        label = "GHI",
                        actions = mapOf(
                            GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('g'),
                            GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('í'),
                            GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('h'),
                            GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('i'),
                            GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Character('ì'),
                            GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('î')
                        )
                    ),
                    
                    // Celda 3 (JKL)
                    CellConfiguration(
                        cellIndex = 3,
                        label = "JKL",
                        actions = mapOf(
                            GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('j'),
                            GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('k'),
                            GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('l')
                        )
                    ),
                    
                    // Celda 4 (MNÑ) - Centro con espacio y enter
                    CellConfiguration(
                        cellIndex = 4,
                        label = "MNÑ",
                        actions = mapOf(
                            GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character(' '),
                            GestureAction(GestureType.LONG_PRESS, Direction.CENTER) to ActionConfig.Action(KeyAction.Enter),
                            GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('ñ'),
                            GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('m'),
                            GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('n')
                        )
                    ),
                    
                    // Celda 5 (OPQ)
                    CellConfiguration(
                        cellIndex = 5,
                        label = "OPQ",
                        actions = mapOf(
                            GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('o'),
                            GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('ó'),
                            GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('p'),
                            GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('q'),
                            GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Character('ò'),
                            GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('ô')
                        )
                    ),
                    
                    // Celda 6 (RST)
                    CellConfiguration(
                        cellIndex = 6,
                        label = "RST",
                        actions = mapOf(
                            GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('r'),
                            GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('s'),
                            GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('t')
                        )
                    ),
                    
                    // Celda 7 (UVW)
                    CellConfiguration(
                        cellIndex = 7,
                        label = "UVW",
                        actions = mapOf(
                            GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('u'),
                            GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('ú'),
                            GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('v'),
                            GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('w'),
                            GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Character('ù'),
                            GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('û')
                        )
                    ),
                    
                    // Celda 8 (XYZ) - Con backspace
                    CellConfiguration(
                        cellIndex = 8,
                        label = "XYZ",
                        actions = mapOf(
                            GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('x'),
                            GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('y'),
                            GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('z'),
                            GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Action(KeyAction.Backspace),
                            GestureAction(GestureType.LONG_PRESS, Direction.CENTER) to ActionConfig.Action(KeyAction.Backspace)
                        )
                    )
                )
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
                cells = listOf(
                    // Celda 0 (123)
                    CellConfiguration(
                        cellIndex = 0,
                        label = "123",
                        actions = mapOf(
                            GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('1'),
                            GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('2'),
                            GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('3'),
                            GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('!'),
                            GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Character('@')
                        )
                    ),
                    
                    // Celda 1 (456)
                    CellConfiguration(
                        cellIndex = 1,
                        label = "456",
                        actions = mapOf(
                            GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('4'),
                            GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('5'),
                            GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('6'),
                            GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('#'),
                            GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Character('$')
                        )
                    ),
                    
                    // Celda 2 (789)
                    CellConfiguration(
                        cellIndex = 2,
                        label = "789",
                        actions = mapOf(
                            GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('7'),
                            GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('8'),
                            GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('9'),
                            GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('%'),
                            GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Character('&')
                        )
                    ),
                    
                    // Celda 3 (Símbolos básicos)
                    CellConfiguration(
                        cellIndex = 3,
                        label = "+-*",
                        actions = mapOf(
                            GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('+'),
                            GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('-'),
                            GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('*'),
                            GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('='),
                            GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Character('/')
                        )
                    ),
                    
                    // Celda 4 (0 y espacio)
                    CellConfiguration(
                        cellIndex = 4,
                        label = "0",
                        actions = mapOf(
                            GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character(' '),
                            GestureAction(GestureType.LONG_PRESS, Direction.CENTER) to ActionConfig.Action(KeyAction.Enter),
                            GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('0'),
                            GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('.'),
                            GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character(',')
                        )
                    ),
                    
                    // Celda 5 (Puntuación)
                    CellConfiguration(
                        cellIndex = 5,
                        label = ".,?",
                        actions = mapOf(
                            GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('.'),
                            GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character(','),
                            GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('?'),
                            GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('!'),
                            GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Character(';'),
                            GestureAction(GestureType.SWIPE, Direction.S) to ActionConfig.Character(':')
                        )
                    ),
                    
                    // Celda 6 (Paréntesis y corchetes)
                    CellConfiguration(
                        cellIndex = 6,
                        label = "()[]",
                        actions = mapOf(
                            GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('('),
                            GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character(')'),
                            GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('['),
                            GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character(']'),
                            GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Character('{'),
                            GestureAction(GestureType.SWIPE, Direction.S) to ActionConfig.Character('}')
                        )
                    ),
                    
                    // Celda 7 (Comillas y símbolos)
                    CellConfiguration(
                        cellIndex = 7,
                        label = "\"'_",
                        actions = mapOf(
                            GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('"'),
                            GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('\''),
                            GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('_'),
                            GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('-'),
                            GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Character('|'),
                            GestureAction(GestureType.SWIPE, Direction.S) to ActionConfig.Character('\\')
                        )
                    ),
                    
                    // Celda 8 (Backspace)
                    CellConfiguration(
                        cellIndex = 8,
                        label = "⌫",
                        actions = mapOf(
                            GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Action(KeyAction.Backspace),
                            GestureAction(GestureType.LONG_PRESS, Direction.CENTER) to ActionConfig.Action(KeyAction.Backspace),
                            GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('<'),
                            GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('>'),
                            GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('~')
                        )
                    )
                )
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
                cells = listOf(
                    // Celda 0 (ABC)
                    CellConfiguration(
                        cellIndex = 0,
                        label = "ABC",
                        actions = mapOf(
                            GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('a'),
                            GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('b'),
                            GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('c')
                        )
                    ),
                    
                    // Celda 1 (DEF)
                    CellConfiguration(
                        cellIndex = 1,
                        label = "DEF",
                        actions = mapOf(
                            GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('d'),
                            GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('e'),
                            GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('f')
                        )
                    ),
                    
                    // Celda 2 (GHI)
                    CellConfiguration(
                        cellIndex = 2,
                        label = "GHI",
                        actions = mapOf(
                            GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('g'),
                            GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('h'),
                            GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('i')
                        )
                    ),
                    
                    // Celda 3 (JKL)
                    CellConfiguration(
                        cellIndex = 3,
                        label = "JKL",
                        actions = mapOf(
                            GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('j'),
                            GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('k'),
                            GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('l')
                        )
                    ),
                    
                    // Celda 4 (MNO) - Con espacio y enter
                    CellConfiguration(
                        cellIndex = 4,
                        label = "MNO",
                        actions = mapOf(
                            GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character(' '),
                            GestureAction(GestureType.LONG_PRESS, Direction.CENTER) to ActionConfig.Action(KeyAction.Enter),
                            GestureAction(GestureType.SWIPE, Direction.N) to ActionConfig.Character('m'),
                            GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('n'),
                            GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('o')
                        )
                    ),
                    
                    // Celda 5 (PQR)
                    CellConfiguration(
                        cellIndex = 5,
                        label = "PQR",
                        actions = mapOf(
                            GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('p'),
                            GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('q'),
                            GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('r')
                        )
                    ),
                    
                    // Celda 6 (STU)
                    CellConfiguration(
                        cellIndex = 6,
                        label = "STU",
                        actions = mapOf(
                            GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('s'),
                            GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('t'),
                            GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('u')
                        )
                    ),
                    
                    // Celda 7 (VWX)
                    CellConfiguration(
                        cellIndex = 7,
                        label = "VWX",
                        actions = mapOf(
                            GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('v'),
                            GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('w'),
                            GestureAction(GestureType.SWIPE, Direction.E) to ActionConfig.Character('x')
                        )
                    ),
                    
                    // Celda 8 (YZ) - Con backspace
                    CellConfiguration(
                        cellIndex = 8,
                        label = "YZ",
                        actions = mapOf(
                            GestureAction(GestureType.TAP, Direction.CENTER) to ActionConfig.Character('y'),
                            GestureAction(GestureType.SWIPE, Direction.NE) to ActionConfig.Character('z'),
                            GestureAction(GestureType.SWIPE, Direction.W) to ActionConfig.Action(KeyAction.Backspace),
                            GestureAction(GestureType.LONG_PRESS, Direction.CENTER) to ActionConfig.Action(KeyAction.Backspace)
                        )
                    )
                )
            )
        }

        /**
         * Configuración inglesa - modo símbolos (reutiliza la española)
         */
        private fun createEnglishSymbols(): KeyboardConfiguration {
            return createSpanishSymbols().copy(
                name = "English - Symbols",
                description = "English layout with numbers and symbols",
                language = Language.ENGLISH
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
                cells = (0..8).map { index ->
                    CellConfiguration(
                        cellIndex = index,
                        label = "Cell $index",
                        actions = emptyMap()
                    )
                }
            )
        }
    }
}

/**
 * Configuración de una celda individual
 */
data class CellConfiguration(
    val cellIndex: Int,
    val label: String,
    val actions: Map<GestureAction, ActionConfig>
) {
    init {
        // Validar que el índice de celda esté en el rango válido (0-8 para grid 3x3)
        if (cellIndex !in 0..8) {
            throw IllegalArgumentException("Cell index must be between 0 and 8, got: $cellIndex")
        }
        
        // Validar que no haya más de un TAP o LONG_PRESS para CENTER
        val centerActions = actions.keys.filter { it.direction == Direction.CENTER }
        val tapActions = centerActions.filter { it.gestureType == GestureType.TAP }
        val longPressActions = centerActions.filter { it.gestureType == GestureType.LONG_PRESS }
        
        if (tapActions.size > 1) {
            throw IllegalArgumentException("Cell $cellIndex has multiple TAP actions for CENTER direction")
        }
        
        if (longPressActions.size > 1) {
            throw IllegalArgumentException("Cell $cellIndex has multiple LONG_PRESS actions for CENTER direction")
        }
        
        // Validar que no haya múltiples SWIPE para la misma dirección
        val swipeDirections = actions.keys
            .filter { it.gestureType == GestureType.SWIPE }
            .groupBy { it.direction }
        
        swipeDirections.forEach { (direction, gestureActions) ->
            if (gestureActions.size > 1) {
                throw IllegalArgumentException("Cell $cellIndex has multiple SWIPE actions for direction $direction")
            }
        }
    }
    /**
     * Obtiene la acción configurada para un gesto específico
     */
    fun getActionFor(gestureType: GestureType, direction: Direction): ActionConfig? {
        return actions[GestureAction(gestureType, direction)]
    }
    
    /**
     * Verifica si tiene alguna acción configurada
     */
    fun hasActions(): Boolean = actions.isNotEmpty()
    
    /**
     * Obtiene todas las direcciones configuradas para un tipo de gesto
     */
    fun getDirectionsFor(gestureType: GestureType): List<Direction> {
        return actions.keys.filter { it.gestureType == gestureType }.map { it.direction }
    }
}

/**
 * Identifica un gesto específico (tipo + dirección)
 */
data class GestureAction(
    val gestureType: GestureType,
    val direction: Direction
) {
    init {
        // Validar que la combinación de gesto y dirección sea válida
        if (!isValidGestureAction(gestureType, direction)) {
            throw IllegalArgumentException(
                "Invalid gesture action: $gestureType cannot be performed in direction $direction. " +
                "TAP and LONG_PRESS are only allowed in CENTER direction. " +
                "SWIPE is only allowed in non-CENTER directions."
            )
        }
    }
    
    companion object {
        /**
         * Valida si una combinación de tipo de gesto y dirección es válida
         */
        fun isValidGestureAction(gestureType: GestureType, direction: Direction): Boolean {
            return when (gestureType) {
                GestureType.TAP, GestureType.LONG_PRESS -> direction == Direction.CENTER
                GestureType.SWIPE, GestureType.DRAG, GestureType.FLICK -> direction != Direction.CENTER
            }
        }
        
        /**
         * Obtiene todas las direcciones válidas para un tipo de gesto
         */
        fun getValidDirectionsFor(gestureType: GestureType): List<Direction> {
            return when (gestureType) {
                GestureType.TAP, GestureType.LONG_PRESS -> listOf(Direction.CENTER)
                GestureType.SWIPE, GestureType.DRAG, GestureType.FLICK -> Direction.values().filter { it != Direction.CENTER }
            }
        }
        
        /**
         * Obtiene todos los tipos de gesto válidos para una dirección
         */
        fun getValidGestureTypesFor(direction: Direction): List<GestureType> {
            return when (direction) {
                Direction.CENTER -> listOf(GestureType.TAP, GestureType.LONG_PRESS)
                else -> listOf(GestureType.SWIPE, GestureType.DRAG, GestureType.FLICK)
            }
        }
    }
}

/**
 * Configuración de acción que puede ser carácter o acción del teclado
 */
sealed class ActionConfig {
    data class Character(val char: Char) : ActionConfig()
    data class Action(val keyAction: KeyAction) : ActionConfig()
    data class Text(val text: String) : ActionConfig() // Para texto múltiple
}
