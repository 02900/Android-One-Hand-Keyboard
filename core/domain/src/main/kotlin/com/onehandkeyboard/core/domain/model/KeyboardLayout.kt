package com.onehandkeyboard.core.domain.model

/**
 * Represents a complete keyboard layout with 9 cells arranged in a 3×3 grid
 */
data class KeyboardLayout(
    /**
     * Unique identifier for this layout
     */
    val id: String,
    
    /**
     * Display name for this layout
     */
    val name: String,
    
    /**
     * Description of this layout
     */
    val description: String,
    
    /**
     * The 9 cells that make up this layout (indexed 0-8)
     */
    val cells: List<Cell>,
    
    /**
     * Layout type (letters, numbers, symbols, etc.)
     */
    val type: LayoutType,
    
    /**
     * Language/locale this layout is designed for
     */
    val locale: String = "en-US",
    
    /**
     * Whether this layout supports case switching
     */
    val supportsCaseSwitch: Boolean = true,
    
    /**
     * Default case mode for this layout
     */
    val defaultCaseMode: CaseMode = CaseMode.LOWERCASE
) {
    init {
        require(cells.size == 9) { "Layout must have exactly 9 cells" }
        cells.forEachIndexed { index, cell ->
            require(cell.index == index) { "Cell at position $index must have index $index" }
        }
    }
    
    /**
     * Gets a cell by its index
     */
    fun getCell(index: Int): Cell? {
        return cells.getOrNull(index)
    }
    
    /**
     * Gets a cell by row and column coordinates
     */
    fun getCell(row: Int, column: Int): Cell? {
        val index = Cell.coordinatesToIndex(row, column)
        return getCell(index)
    }
    
    /**
     * Processes a gesture and returns the resulting character or action
     */
    fun processGesture(gesture: Gesture): KeyboardOutput? {
        val cell = getCell(gesture.startCell) ?: return null
        
        // Handle character output
        val character = cell.getCharacterForDirection(gesture.direction)
        if (character != null) {
            return KeyboardOutput.Character(character)
        }
        
        // Handle action output
        val action = cell.getActionForDirection(gesture.direction)
        if (action != null) {
            return KeyboardOutput.Action(action)
        }
        
        return null
    }
    
    /**
     * Creates a copy of this layout with modified case mode
     */
    fun withCaseMode(caseMode: CaseMode): KeyboardLayout {
        if (!supportsCaseSwitch || caseMode == defaultCaseMode) {
            return this
        }
        
        val modifiedCells = cells.map { cell ->
            val modifiedPrimaryChars = when (caseMode) {
                CaseMode.UPPERCASE -> cell.primaryChars.map { it.uppercaseChar() }
                CaseMode.LOWERCASE -> cell.primaryChars.map { it.lowercaseChar() }
                CaseMode.CAPITALIZE -> cell.primaryChars.mapIndexed { index, char ->
                    if (index == 0) char.uppercaseChar() else char.lowercaseChar()
                }
            }
            
            val modifiedSecondaryChars = cell.secondaryChars.mapValues { (_, char) ->
                when (caseMode) {
                    CaseMode.UPPERCASE -> char.uppercaseChar()
                    CaseMode.LOWERCASE -> char.lowercaseChar()
                    CaseMode.CAPITALIZE -> char.uppercaseChar() // Secondary chars are typically uppercase
                }
            }
            
            cell.copy(
                primaryChars = modifiedPrimaryChars,
                secondaryChars = modifiedSecondaryChars
            )
        }
        
        return copy(cells = modifiedCells, defaultCaseMode = caseMode)
    }
}

/**
 * Types of keyboard layouts
 */
enum class LayoutType {
    LETTERS,
    NUMBERS,
    SYMBOLS,
    PUNCTUATION,
    EMOJI,
    CUSTOM
}

/**
 * Case modes for text input
 */
enum class CaseMode {
    LOWERCASE,
    UPPERCASE,
    CAPITALIZE
}

/**
 * Output from processing a keyboard gesture
 */
sealed class KeyboardOutput {
    /**
     * A character to be inserted
     */
    data class Character(val char: Char) : KeyboardOutput()
    
    /**
     * An action to be performed
     */
    data class Action(val action: KeyAction) : KeyboardOutput()
}
