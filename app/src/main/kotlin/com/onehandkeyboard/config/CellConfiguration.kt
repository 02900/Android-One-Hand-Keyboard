package com.onehandkeyboard.config

import com.onehandkeyboard.actions.ActionConfig
import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.GestureType
import com.onehandkeyboard.gestures.GestureAction

/**
 * Configuración de una celda individual en grid 4x4
 */
data class CellConfiguration(
    val cellIndex: Int,
    val actions: Map<GestureAction, ActionConfig>,
    val row: Int = cellIndex / GRID_SIZE,
    val col: Int = cellIndex % GRID_SIZE,
    // Cuántas columnas ocupa esta celda
    val colSpan: Int = 1,
    // Cuántas filas ocupa esta celda
    val rowSpan: Int = 1,
) {
    init {
        // Validar que el índice de celda esté en el rango válido (0-15 para grid 4x4)
        require(cellIndex in 0 until GRID_SIZE * GRID_SIZE) {
            "Cell index must be between 0 and ${GRID_SIZE * GRID_SIZE - 1}, got: $cellIndex"
        }

        // Validar que no haya más de un TAP para CENTER
        val centerActions = actions.keys.filter { it.direction == Direction.CENTER }
        val tapActions = centerActions.filter { it.gestureType == GestureType.TAP }

        require(tapActions.size <= 1) { "Cell $cellIndex has multiple TAP actions for CENTER direction" }

        // Validar que no haya múltiples SWIPE para la misma dirección
        val swipeDirections =
            actions.keys
                .filter { it.gestureType == GestureType.SWIPE }
                .groupBy { it.direction }

        swipeDirections.forEach { (direction, gestureActions) ->
            require(gestureActions.size <= 1) { "Cell $cellIndex has multiple SWIPE actions for direction $direction" }
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

    companion object {
        /** The keyboard is a 4×4 grid. */
        const val GRID_SIZE = 4
    }
}
