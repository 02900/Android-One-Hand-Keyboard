package com.onehandkeyboard.input

import android.os.SystemClock
import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection

/**
 * All direct operations against the target field's [InputConnection].
 *
 * Kept free of UI and service state so it can be unit tested with a mocked connection.
 * Never logs the user's text.
 */
class InputCommitter {
    /** Text removed by the last bulk delete, so it can be restored with [undoDelete]. */
    private var lastDeleted: CharSequence? = null
    private var lastSpaceTime = Long.MIN_VALUE / 2

    fun commitText(
        ic: InputConnection?,
        text: CharSequence,
    ) {
        ic?.commitText(text, 1)
    }

    /**
     * Deletes the current selection, or one character before the cursor.
     *
     * Uses a DEL key event instead of `deleteSurroundingText(1, 0)` so that surrogate pairs
     * (emoji) are removed whole and apps that only listen to key events still work.
     */
    fun backspace(ic: InputConnection?) {
        ic ?: return
        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            ic.commitText("", 1)
        } else {
            sendKey(ic, KeyEvent.KEYCODE_DEL)
        }
    }

    /**
     * Enter key: inserts a newline in multi-line fields; otherwise runs the field's IME action
     * (search, go, send, next, done...) or falls back to a plain ENTER key event.
     */
    fun enter(
        ic: InputConnection?,
        info: EditorInfo?,
    ) {
        ic ?: return
        when {
            wantsNewline(info) -> ic.commitText("\n", 1)
            hasEditorAction(info) -> ic.performEditorAction(editorAction(info))
            else -> sendKey(ic, KeyEvent.KEYCODE_ENTER)
        }
    }

    /**
     * Submit: always runs the field's IME action when it declares one (even in multi-line
     * fields such as chats), otherwise sends an ENTER key event so web forms still submit.
     */
    fun submit(
        ic: InputConnection?,
        info: EditorInfo?,
    ) {
        ic ?: return
        if (hasEditorAction(info)) {
            ic.performEditorAction(editorAction(info))
        } else {
            sendKey(ic, KeyEvent.KEYCODE_ENTER)
        }
    }

    /**
     * Space. A second space typed quickly after a word becomes ". " (the period shortcut),
     * unless [allowPeriodShortcut] is false (passwords, URLs, emails...).
     *
     * @param now event time in ms, used for the double-space window.
     */
    fun space(
        ic: InputConnection?,
        now: Long,
        allowPeriodShortcut: Boolean = true,
    ) {
        ic ?: return
        val isDoubleSpace = allowPeriodShortcut && now - lastSpaceTime <= DOUBLE_SPACE_MS
        lastSpaceTime = now
        if (isDoubleSpace) {
            val before = ic.getTextBeforeCursor(2, 0)
            if (endsWithWordAndSpace(before)) {
                ic.beginBatchEdit()
                ic.deleteSurroundingText(1, 0)
                ic.commitText(". ", 1)
                ic.endBatchEdit()
                lastSpaceTime = 0L
                return
            }
        }
        ic.commitText(" ", 1)
    }

    /** Any key other than space breaks the double-space shortcut. */
    fun resetSpaceShortcut() {
        lastSpaceTime = Long.MIN_VALUE / 2
    }

    /**
     * Deletes the word before the cursor together with the spaces that follow it, and
     * remembers it for [undoDelete]. With a selection, deletes the selection.
     */
    fun deleteWord(ic: InputConnection?) {
        ic ?: return
        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            lastDeleted = selected
            ic.commitText("", 1)
            return
        }
        val before = ic.getTextBeforeCursor(WORD_LOOKBACK, 0)?.toString().orEmpty()
        val length = wordDeletionLength(before)
        if (length == 0) return
        lastDeleted = before.takeLast(length)
        ic.deleteSurroundingText(length, 0)
    }

    /** Moves the cursor [steps] characters (negative = left) with DPAD key events. */
    fun moveCursor(
        ic: InputConnection?,
        steps: Int,
    ) {
        ic ?: return
        val keyCode = if (steps < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        repeat(kotlin.math.abs(steps)) { sendKey(ic, keyCode) }
    }

    /** Deletes every character of the field and remembers it for [undoDelete]. */
    fun deleteAll(ic: InputConnection?) {
        ic ?: return
        val text = fullText(ic)
        if (text.isNullOrEmpty()) return
        lastDeleted = text
        ic.beginBatchEdit()
        ic.setSelection(0, text.length)
        ic.commitText("", 1)
        ic.endBatchEdit()
    }

    /** Restores the text removed by the last [deleteAll] or [deleteWord]. Returns false if there was nothing. */
    fun undoDelete(ic: InputConnection?): Boolean {
        ic ?: return false
        val text = lastDeleted ?: return false
        ic.commitText(text, 1)
        lastDeleted = null
        return true
    }

    private fun sendKey(
        ic: InputConnection,
        keyCode: Int,
    ) {
        val now = SystemClock.uptimeMillis()
        val flags = KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, 0, -1, 0, flags))
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, 0, -1, 0, flags))
    }

    companion object {
        private const val MAX_FALLBACK_CHARS = 100_000
        private const val WORD_LOOKBACK = 64
        const val DOUBLE_SPACE_MS = 600L

        /** Length of the trailing "word + spaces" of [before] that a delete-word removes. */
        fun wordDeletionLength(before: String): Int {
            var index = before.length
            while (index > 0 && before[index - 1].isWhitespace()) index--
            if (index > 0 && !before[index - 1].isLetterOrDigit()) {
                // Punctuation run (e.g. "..." or "?!") counts as one word.
                while (index > 0 && !before[index - 1].isLetterOrDigit() && !before[index - 1].isWhitespace()) index--
            } else {
                while (index > 0 && before[index - 1].isLetterOrDigit()) index--
            }
            return before.length - index
        }

        fun editorAction(info: EditorInfo?): Int = (info?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION

        fun hasEditorAction(info: EditorInfo?): Boolean {
            val action = editorAction(info)
            return action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED
        }

        fun isPassword(info: EditorInfo?): Boolean {
            val type = info?.inputType ?: return false
            val variation = type and InputType.TYPE_MASK_VARIATION
            return when (type and InputType.TYPE_MASK_CLASS) {
                InputType.TYPE_CLASS_TEXT ->
                    variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
                InputType.TYPE_CLASS_NUMBER -> variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
                else -> false
            }
        }

        /** The ". " double-space shortcut only makes sense in prose fields. */
        fun allowsPeriodShortcut(info: EditorInfo?): Boolean {
            val type = info?.inputType ?: return true
            if ((type and InputType.TYPE_MASK_CLASS) != InputType.TYPE_CLASS_TEXT) return false
            if (isPassword(info)) return false
            val variation = type and InputType.TYPE_MASK_VARIATION
            return variation != InputType.TYPE_TEXT_VARIATION_URI &&
                variation != InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS &&
                variation != InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS &&
                variation != InputType.TYPE_TEXT_VARIATION_FILTER
        }

        /** Returns the full text of the field, or null if the app does not expose it. */
        fun fullText(ic: InputConnection?): CharSequence? {
            ic ?: return null
            ic.getExtractedText(ExtractedTextRequest(), 0)?.text?.let { return it }
            val before = ic.getTextBeforeCursor(MAX_FALLBACK_CHARS, 0) ?: ""
            val after = ic.getTextAfterCursor(MAX_FALLBACK_CHARS, 0) ?: ""
            return before.toString() + after.toString()
        }

        /** "word " — a letter or digit followed by a single space. */
        internal fun endsWithWordAndSpace(before: CharSequence?): Boolean =
            before != null && before.length == 2 && before[1] == ' ' && before[0].isLetterOrDigit()

        fun wantsNewline(info: EditorInfo?): Boolean {
            info ?: return true
            val noEnterAction = (info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
            val multiLine =
                (info.inputType and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT &&
                    (info.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
            return noEnterAction || multiLine
        }
    }
}
