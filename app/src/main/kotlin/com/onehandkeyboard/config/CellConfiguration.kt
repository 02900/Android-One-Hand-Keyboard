package com.onehandkeyboard.config

import com.onehandkeyboard.actions.ActionConfig
import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.GestureType
import com.onehandkeyboard.gestures.GestureAction

/**
 * Configuración de una celda individual
 */
data class CellConfiguration(
    val cellIndex: Int,
    val label: String,
    val actions: Map<GestureAction, ActionConfig>,
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
        val swipeDirections =
            actions.keys
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
    fun getActionFor(
        gestureType: GestureType,
        direction: Direction,
    ): ActionConfig? {
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
