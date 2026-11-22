package com.onehandkeyboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import com.onehandkeyboard.core.domain.model.KeyAction
import com.onehandkeyboard.core.domain.model.KeyboardOutput
import com.onehandkeyboard.enums.LayoutMode
import com.onehandkeyboard.input.InputCommitter
import com.onehandkeyboard.spell.AutoCorrector
import com.onehandkeyboard.spell.Suggestion

/**
 * Processes keyboard outputs (characters and actions) against the current input field.
 *
 * Text operations are delegated to [InputCommitter]. Nothing typed, pasted or copied by
 * the user is ever logged.
 */
class KeyboardActionProcessor(
    private val service: InputMethodService,
    private val configManager: KeyboardLayoutManager,
    private val onLayoutSwitch: () -> Unit,
    private val onCaseSwitch: () -> Unit,
    private val committer: InputCommitter = InputCommitter(),
    private val corrector: AutoCorrector? = null,
    private val onShowMemes: () -> Unit = {},
    private val onVoiceInput: () -> Unit = {},
    /** Where the keys write: the app's field, or the keyboard's own meme search box. */
    private val connection: () -> InputConnection? = { service.currentInputConnection },
    private val editor: () -> EditorInfo? = { service.currentInputEditorInfo },
) {
    private val ic get() = connection()
    private val editorInfo get() = editor()

    fun processKeyboardOutput(output: KeyboardOutput) {
        when (output) {
            is KeyboardOutput.Character -> {
                committer.resetSpaceShortcut()
                if (output.char in SEPARATORS) corrector?.beforeSeparator(ic, output.char) else corrector?.invalidate()
                committer.commitText(ic, output.char)
                if (configManager.onCharacterWritten()) onCaseSwitch()
            }
            is KeyboardOutput.Action -> {
                if (output.action != KeyAction.Space) committer.resetSpaceShortcut()
                if (output.action != KeyAction.Space && output.action != KeyAction.Backspace) corrector?.invalidate()
                executeKeyAction(output.action)
            }
        }
    }

    private fun executeSpace() {
        val accent = configManager.pendingDeadKey
        if (accent != null) {
            // Dead key followed by space types the accent itself.
            committer.commitText(ic, accent.toString())
            configManager.clearDeadKey()
            onCaseSwitch()
            return
        }
        val connection = ic
        connection?.beginBatchEdit()
        corrector?.beforeSeparator(connection, " ")
        committer.space(connection, SystemClock.uptimeMillis(), InputCommitter.allowsPeriodShortcut(editorInfo))
        connection?.endBatchEdit()
    }

    /** A word picked in the suggestion strip. */
    fun pickSuggestion(suggestion: Suggestion) {
        committer.resetSpaceShortcut()
        corrector?.pick(ic, suggestion)
        if (configManager.onCharacterWritten()) onCaseSwitch()
    }

    private fun executeBackspace() {
        // Backspace right after a dead key just cancels it.
        if (configManager.clearDeadKey()) {
            onCaseSwitch()
            return
        }
        // Right after an autocorrection, backspace gives back what the user typed.
        if (corrector?.onBackspace(ic) == true) return
        corrector?.invalidate()
        committer.backspace(ic)
    }

    private fun executeKeyAction(action: KeyAction) {
        when (action) {
            is KeyAction.Backspace -> executeBackspace()
            is KeyAction.Space -> executeSpace()
            is KeyAction.Enter -> committer.enter(ic, editorInfo)
            is KeyAction.Submit -> committer.submit(ic, editorInfo)
            is KeyAction.ToggleCase -> {
                configManager.switchCaseMode()
                onCaseSwitch()
            }
            is KeyAction.SwitchToNumbers -> {
                configManager.setMode(LayoutMode.SYMBOLS)
                onLayoutSwitch()
            }
            is KeyAction.SwitchToLetters -> {
                configManager.setMode(LayoutMode.LETTERS)
                onLayoutSwitch()
            }
            is KeyAction.InsertText -> committer.commitText(ic, action.text)
            is KeyAction.Paste -> executePaste()
            is KeyAction.CopyAll -> executeCopyAll()
            is KeyAction.DeleteAll -> committer.deleteAll(ic)
            is KeyAction.UndoDelete -> committer.undoDelete(ic)
            is KeyAction.DeleteWord -> committer.deleteWord(ic)
            is KeyAction.CursorLeft -> committer.moveCursor(ic, -1)
            is KeyAction.CursorRight -> committer.moveCursor(ic, 1)
            is KeyAction.DeadKey -> if (configManager.setDeadKey(action.accent)) onCaseSwitch()
            is KeyAction.SwitchLanguage ->
                if (configManager.setLanguage(
                        configManager.currentLanguage.other(),
                    )
                ) {
                    onLayoutSwitch()
                }
            is KeyAction.ShowMemes -> onShowMemes()
            is KeyAction.VoiceInput -> onVoiceInput()
            else -> Log.d(TAG, "Action not implemented: ${action::class.simpleName}")
        }
    }

    private fun executePaste() {
        val clipboard = service.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clipText = clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(service)
        if (!clipText.isNullOrEmpty()) {
            committer.commitText(ic, clipText)
            if (configManager.onCharacterWritten()) onCaseSwitch()
        }
    }

    private fun executeCopyAll() {
        val clipboard = service.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val text = InputCommitter.fullText(ic)
        if (!text.isNullOrEmpty()) {
            clipboard.setPrimaryClip(ClipData.newPlainText("OneHandKeyboard", text))
        }
    }

    private companion object {
        const val TAG = "OneHandKeyboard"

        /** Characters that close a word and trigger autocorrection (besides space). */
        val SEPARATORS = setOf(".", ",", ";", ":", "?", "!")
    }
}
