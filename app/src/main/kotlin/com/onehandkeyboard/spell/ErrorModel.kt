package com.onehandkeyboard.spell

import com.onehandkeyboard.actions.ActionConfig
import com.onehandkeyboard.config.CellConfiguration
import com.onehandkeyboard.config.KeyboardConfiguration
import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.GestureType

/**
 * Cost of typing one letter instead of another.
 *
 * On this keyboard each cell holds several letters reached by swiping in different
 * directions, so a swipe in the wrong direction (another letter of the same cell) is a much
 * more likely mistake than hitting an unrelated letter. Likewise, a tap that misses the space
 * bar lands on a cell right above it and types that cell's letter instead of a space.
 */
class ErrorModel(
    /** Folded letter → index of the cell that types it. */
    private val cellOf: Map<Char, Int>,
    /** Folded letters typed by tapping the cells right above the space bar. */
    private val spaceNeighbors: Set<Char> = emptySet(),
) {
    fun substitution(
        typed: Char,
        intended: Char,
    ): Float {
        if (typed == intended) return 0f
        val cell = cellOf[typed]
        return if (cell != null && cell == cellOf[intended]) SAME_CELL_COST else 1f
    }

    /** Cost of [typed] having been a missed space, or null if it cannot be one. */
    fun missedSpace(typed: Char): Float? = if (typed.lowercaseChar() in spaceNeighbors) MISSED_SPACE_COST else null

    companion object {
        const val SAME_CELL_COST = 0.5f
        const val MISSED_SPACE_COST = 0.5f

        /** Every substitution costs 1 (no keyboard knowledge). */
        val UNIFORM = ErrorModel(emptyMap())

        /** Builds the model from the letters of each cell. */
        fun fromCells(
            cells: Map<Int, Collection<Char>>,
            spaceNeighbors: Collection<Char> = emptyList(),
        ): ErrorModel {
            val cellOf = HashMap<Char, Int>()
            cells.forEach { (cell, chars) ->
                chars.mapNotNull(::foldedLetter).forEach { cellOf[it] = cell }
            }
            return ErrorModel(cellOf, spaceNeighbors.mapNotNull(::foldedLetter).toSet())
        }

        /** Builds the model from a letters layout: its cells and the letters above the space bar. */
        fun fromLayout(config: KeyboardConfiguration): ErrorModel {
            val cells =
                config.cells.associate { cell ->
                    cell.cellIndex to cell.actions.values.filterIsInstance<ActionConfig.Character>().map { it.char }
                }
            val space = config.cells.firstOrNull { it.tapAction() == ActionConfig.Space }
            val aboveSpace =
                config.cells
                    .filter { space != null && it.row == space.row - 1 && it.col - space.col in 0 until space.colSpan }
                    .mapNotNull { (it.tapAction() as? ActionConfig.Character)?.char }
            return fromCells(cells, aboveSpace)
        }

        private fun CellConfiguration.tapAction() = getActionFor(GestureType.TAP, Direction.CENTER)

        private fun foldedLetter(char: Char): Char? =
            WordDictionary.fold(char.toString()).singleOrNull()?.takeIf { it.isLetter() }
    }
}
