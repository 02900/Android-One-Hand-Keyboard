package com.onehandkeyboard.gestures

import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.GestureType

/**
 * Identifica un gesto específico (tipo + dirección)
 */
data class GestureAction(
    val gestureType: GestureType,
    val direction: Direction,
) {
    init {
        // Validar que la combinación de gesto y dirección sea válida
        if (!isValidGestureAction(gestureType, direction)) {
            throw IllegalArgumentException(
                "Invalid gesture action: $gestureType cannot be performed in direction $direction. " +
                    "TAP and LONG_PRESS are only allowed in CENTER direction. " +
                    "SWIPE is only allowed in non-CENTER directions.",
            )
        }
    }

    companion object {
        /**
         * Valida si una combinación de tipo de gesto y dirección es válida
         */
        fun isValidGestureAction(
            gestureType: GestureType,
            direction: Direction,
        ): Boolean {
            return when (gestureType) {
                GestureType.TAP -> direction == Direction.CENTER
                GestureType.SWIPE, GestureType.DRAG, GestureType.FLICK -> direction != Direction.CENTER
            }
        }

        /**
         * Obtiene todas las direcciones válidas para un tipo de gesto
         */
        fun getValidDirectionsFor(gestureType: GestureType): List<Direction> {
            return when (gestureType) {
                GestureType.TAP -> listOf(Direction.CENTER)
                GestureType.SWIPE, GestureType.DRAG, GestureType.FLICK ->
                    Direction.values().filter { it != Direction.CENTER }
            }
        }

        /**
         * Obtiene todos los tipos de gesto válidos para una dirección
         */
        fun getValidGestureTypesFor(direction: Direction): List<GestureType> {
            return when (direction) {
                Direction.CENTER -> listOf(GestureType.TAP)
                else -> listOf(GestureType.SWIPE, GestureType.DRAG, GestureType.FLICK)
            }
        }
    }
}
