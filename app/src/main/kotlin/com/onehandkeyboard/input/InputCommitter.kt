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

    /** Returns the full text of the field, or null if the app does not expose it. */
    fun fullText(ic: InputConnection?): CharSequence? {
        ic ?: return null
        ic.getExtractedText(ExtractedTextRequest(), 0)?.text?.let { return it }
        val before = ic.getTextBeforeCursor(MAX_FALLBACK_CHARS, 0) ?: ""
        val after = ic.getTextAfterCursor(MAX_FALLBACK_CHARS, 0) ?: ""
        return before.toString() + after.toString()
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

    /** Restores the text removed by the last [deleteAll]. Returns false if there was nothing. */
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

        fun editorAction(info: EditorInfo?): Int = (info?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION

        fun hasEditorAction(info: EditorInfo?): Boolean {
            val action = editorAction(info)
            return action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED
        }

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
