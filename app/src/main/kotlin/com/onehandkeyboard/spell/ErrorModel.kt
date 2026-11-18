package com.onehandkeyboard.spell

/**
 * Cost of typing one letter instead of another.
 *
 * On this keyboard each cell holds several letters reached by swiping in different
 * directions, so a swipe in the wrong direction (another letter of the same cell) is a much
 * more likely mistake than hitting an unrelated letter.
 */
class ErrorModel(
    /** Folded letter → index of the cell that types it. */
    private val cellOf: Map<Char, Int>,
) {
    fun substitution(
        typed: Char,
        intended: Char,
    ): Float {
        if (typed == intended) return 0f
        val cell = cellOf[typed]
        return if (cell != null && cell == cellOf[intended]) SAME_CELL_COST else 1f
    }

    companion object {
        const val SAME_CELL_COST = 0.5f

        /** Every substitution costs 1 (no keyboard knowledge). */
        val UNIFORM = ErrorModel(emptyMap())

        /** Builds the model from the letters of each cell. */
        fun fromCells(cells: Map<Int, Collection<Char>>): ErrorModel {
            val cellOf = HashMap<Char, Int>()
            cells.forEach { (cell, chars) ->
                chars.map { WordDictionary.fold(it.toString()) }
                    .filter { it.length == 1 && it[0].isLetter() }
                    .forEach { cellOf[it[0]] = cell }
            }
            return ErrorModel(cellOf)
        }
    }
}
