package com.onehandkeyboard.core.domain.model

/**
 * Represents a cell in the 3×3 keyboard grid
 * 
 * The keyboard is laid out as:
 * ```
 * 0 | 1 | 2
 * --+---+--
 * 3 | 4 | 5
 * --+---+--
 * 6 | 7 | 8
 * ```
 */
data class Cell(
    /**
     * Cell index (0-8) in the 3×3 grid
     */
    val index: Int,
    
    /**
     * Row position (0-2) in the grid
     */
    val row: Int,
    
    /**
     * Column position (0-2) in the grid
     */
    val column: Int,
    
    /**
     * Display label for the cell (e.g., "ABC", "DEF", etc.)
     */
    val label: String,
    
    /**
     * Primary characters available in this cell
     */
    val primaryChars: List<Char>,
    
    /**
     * Secondary characters available via gestures
     */
    val secondaryChars: Map<Direction, Char> = emptyMap(),
    
    /**
     * Actions available via gestures (backspace, space, etc.)
     */
    val actions: Map<Direction, KeyAction> = emptyMap()
) {
    companion object {
        /**
         * Converts a cell index (0-8) to row and column coordinates
         */
        fun indexToCoordinates(index: Int): Pair<Int, Int> {
            require(index in 0..8) { "Cell index must be between 0 and 8" }
            return Pair(index / 3, index % 3)
        }
        
        /**
         * Converts row and column coordinates to cell index
         */
        fun coordinatesToIndex(row: Int, column: Int): Int {
            require(row in 0..2) { "Row must be between 0 and 2" }
            require(column in 0..2) { "Column must be between 0 and 2" }
            return row * 3 + column
        }
        
        /**
         * Creates a cell with the given index and automatically calculates coordinates
         */
        fun create(
            index: Int,
            label: String,
            primaryChars: List<Char>,
            secondaryChars: Map<Direction, Char> = emptyMap(),
            actions: Map<Direction, KeyAction> = emptyMap()
        ): Cell {
            val (row, column) = indexToCoordinates(index)
            return Cell(
                index = index,
                row = row,
                column = column,
                label = label,
                primaryChars = primaryChars,
                secondaryChars = secondaryChars,
                actions = actions
            )
        }
    }
    
    /**
     * Gets the character or action for a given direction
     */
    fun getCharacterForDirection(direction: Direction): Char? {
        return when (direction) {
            Direction.CENTER -> primaryChars.firstOrNull()
            else -> secondaryChars[direction]
        }
    }
    
    /**
     * Gets the action for a given direction
     */
    fun getActionForDirection(direction: Direction): KeyAction? {
        return actions[direction]
    }
    
    /**
     * Checks if this cell has content for the given direction
     */
    fun hasContentForDirection(direction: Direction): Boolean {
        return getCharacterForDirection(direction) != null || getActionForDirection(direction) != null
    }
}
