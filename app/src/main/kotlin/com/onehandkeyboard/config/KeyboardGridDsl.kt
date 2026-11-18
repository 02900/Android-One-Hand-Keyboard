package com.onehandkeyboard.config

import com.onehandkeyboard.actions.ActionConfig
import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.GestureType
import com.onehandkeyboard.gestures.GestureAction

@DslMarker
annotation class KeyboardGridDsl

/**
 * Builds the gesture map of one cell. Declaring the same gesture twice fails fast instead of
 * silently overwriting the previous action (a plain `mapOf` keeps only the last entry).
 */
@KeyboardGridDsl
class CellBuilder internal constructor(private val cellIndex: Int) {
    private val actions = LinkedHashMap<GestureAction, ActionConfig>()

    fun tap(char: Char) = tap(ActionConfig.Character(char))

    fun tap(action: ActionConfig) = put(GestureAction(GestureType.TAP, Direction.CENTER), action)

    fun swipe(
        direction: Direction,
        char: Char,
    ) = swipe(direction, ActionConfig.Character(char))

    fun swipe(
        direction: Direction,
        action: ActionConfig,
    ) = put(GestureAction(GestureType.SWIPE, direction), action)

    private fun put(
        gesture: GestureAction,
        action: ActionConfig,
    ) {
        require(gesture !in actions) {
            "Cell $cellIndex declares ${gesture.gestureType} ${gesture.direction} twice " +
                "(${actions[gesture]} and $action)"
        }
        actions[gesture] = action
    }

    internal fun build(colSpan: Int) = CellConfiguration(cellIndex, actions.toMap(), colSpan = colSpan)
}

@KeyboardGridDsl
class GridBuilder internal constructor() {
    private val cells = mutableListOf<CellConfiguration>()

    fun cell(
        index: Int,
        colSpan: Int = 1,
        block: CellBuilder.() -> Unit,
    ) {
        require(cells.none { it.cellIndex == index }) { "Cell $index declared twice" }
        cells += CellBuilder(index).apply(block).build(colSpan)
    }

    internal fun build(): List<CellConfiguration> = cells.toList()
}

fun keyboardGrid(block: GridBuilder.() -> Unit): List<CellConfiguration> = GridBuilder().apply(block).build()
