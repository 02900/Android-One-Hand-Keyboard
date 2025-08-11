package com.onehandkeyboard.config

import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.GestureType
import com.onehandkeyboard.core.domain.model.KeyAction

/**
 * Configuración externa y flexible para las acciones de cada celda del teclado
 */
data class KeyboardConfiguration(
    val name: String,
    val description: String,
    val cells: List<CellConfiguration>
) {
    companion object {
        /**
         * Configuración por defecto para layout español
         */
        fun createSpanishDefault(): KeyboardConfiguration {
            return KeyboardConfiguration(
                name = "Español Básico",
                description = "Layout español con gestos básicos",
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
         * Configuración personalizable vacía
         */
        fun createEmpty(): KeyboardConfiguration {
            return KeyboardConfiguration(
                name = "Personalizado",
                description = "Configuración personalizable",
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
)

/**
 * Configuración de acción que puede ser carácter o acción del teclado
 */
sealed class ActionConfig {
    data class Character(val char: Char) : ActionConfig()
    data class Action(val keyAction: KeyAction) : ActionConfig()
    data class Text(val text: String) : ActionConfig() // Para texto múltiple
}
