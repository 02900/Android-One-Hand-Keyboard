package com.onehandkeyboard.core.domain.model

/**
 * Represents actions that can be performed by the keyboard
 */
sealed class KeyAction {
    /**
     * Delete the character before the cursor
     */
    object Backspace : KeyAction()

    /**
     * Insert a space character
     */
    object Space : KeyAction()

    /**
     * Insert a newline/enter
     */
    object Enter : KeyAction()

    /**
     * Switch to uppercase/lowercase mode
     */
    object ToggleCase : KeyAction()

    /**
     * Switch to numbers layout
     */
    object SwitchToNumbers : KeyAction()

    /**
     * Switch to symbols layout
     */
    object SwitchToSymbols : KeyAction()

    /**
     * Switch to letters layout
     */
    object SwitchToLetters : KeyAction()

    /**
     * Move cursor left
     */
    object CursorLeft : KeyAction()

    /**
     * Move cursor right
     */
    object CursorRight : KeyAction()

    /**
     * Select all text
     */
    object SelectAll : KeyAction()

    /**
     * Cut selected text
     */
    object Cut : KeyAction()

    /**
     * Copy selected text
     */
    object Copy : KeyAction()

    /**
     * Paste text from clipboard
     */
    object Paste : KeyAction()

    /**
     * Copy all text to clipboard
     */
    object CopyAll : KeyAction()

    /**
     * Delete all text in the input field
     */
    object DeleteAll : KeyAction()

    /**
     * Increase cell height by 1dp
     */
    object IncreaseCellHeight : KeyAction()

    /**
     * Decrease cell height by 1dp
     */
    object DecreaseCellHeight : KeyAction()

    /**
     * Reset cell height to default value (60dp)
     */
    object ResetCellHeight : KeyAction()

    /**
     * Custom action with a specific function
     */
    data class Custom(val action: () -> Unit) : KeyAction()

    /**
     * Insert a specific string
     */
    data class InsertText(val text: String) : KeyAction()
}
