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
     * Submit form/send action (alternative to Enter)
     */
    object Submit : KeyAction()

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
     * Restore the text removed by the last bulk delete (e.g. DeleteAll)
     */
    object UndoDelete : KeyAction()

    /**
     * Delete the word (and trailing spaces) before the cursor
     */
    object DeleteWord : KeyAction()

    /**
     * Switch between the available keyboard languages
     */
    object SwitchLanguage : KeyAction()

    /**
     * Open the meme library panel (GIFs and stickers from the user's folder)
     */
    object ShowMemes : KeyAction()

    /**
     * Open the emoji panel (tapping two emojis offers their Emoji Kitchen mix)
     */
    object ShowEmojis : KeyAction()

    /**
     * Voice typing: speak and the recognized text is typed
     */
    object VoiceInput : KeyAction()

    /**
     * Dead key: the next vowel is combined with [accent] (e.g. ´ + e = é)
     */
    data class DeadKey(val accent: Char) : KeyAction()

    /**
     * Custom action with a specific function
     */
    data class Custom(val action: () -> Unit) : KeyAction()

    /**
     * Insert a specific string
     */
    data class InsertText(val text: String) : KeyAction()
}
