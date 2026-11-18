package com.onehandkeyboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.util.Log
import com.onehandkeyboard.core.domain.model.KeyAction
import com.onehandkeyboard.core.domain.model.KeyboardOutput
import com.onehandkeyboard.input.InputCommitter
import com.onehandkeyboard.ui.LayoutCalculator

/**
 * Processes keyboard outputs (characters and actions) against the current input field.
 *
 * Text operations are delegated to [InputCommitter]. Nothing typed, pasted or copied by
 * the user is ever logged.
 */
class KeyboardActionProcessor(
    private val service: InputMethodService,
    private val configManager: KeyboardLayoutManager,
    private val layoutCalculator: LayoutCalculator,
    private val onLayoutSwitch: () -> Unit,
    private val onCaseSwitch: () -> Unit,
    private val onHeightChanged: () -> Unit,
    private val committer: InputCommitter = InputCommitter(),
) {
    private val ic get() = service.currentInputConnection
    private val editorInfo get() = service.currentInputEditorInfo

    fun processKeyboardOutput(output: KeyboardOutput) {
        when (output) {
            is KeyboardOutput.Character -> {
                committer.commitText(ic, output.char)
                configManager.onCharacterWritten()
            }
            is KeyboardOutput.Action -> executeKeyAction(output.action)
        }
    }

    private fun executeKeyAction(action: KeyAction) {
        when (action) {
            is KeyAction.Backspace -> committer.backspace(ic)
            is KeyAction.Space -> committer.commitText(ic, " ")
            is KeyAction.Enter -> committer.enter(ic, editorInfo)
            is KeyAction.Submit -> committer.submit(ic, editorInfo)
            is KeyAction.ToggleCase -> {
                configManager.switchCaseMode()
                onCaseSwitch()
            }
            is KeyAction.SwitchToNumbers -> {
                configManager.switchToSymbols()
                onLayoutSwitch()
            }
            is KeyAction.SwitchToLetters -> {
                configManager.switchToLetters()
                onLayoutSwitch()
            }
            is KeyAction.InsertText -> committer.commitText(ic, action.text)
            is KeyAction.Paste -> executePaste()
            is KeyAction.CopyAll -> executeCopyAll()
            is KeyAction.DeleteAll -> committer.deleteAll(ic)
            is KeyAction.UndoDelete -> committer.undoDelete(ic)
            is KeyAction.IncreaseCellHeight -> if (layoutCalculator.increaseCellHeight()) onHeightChanged()
            is KeyAction.DecreaseCellHeight -> if (layoutCalculator.decreaseCellHeight()) onHeightChanged()
            is KeyAction.ResetCellHeight -> if (layoutCalculator.resetCellHeight()) onHeightChanged()
            else -> Log.d(TAG, "Action not implemented: ${action::class.simpleName}")
        }
    }

    private fun executePaste() {
        val clipboard = service.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clipText = clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(service)
        if (!clipText.isNullOrEmpty()) {
            committer.commitText(ic, clipText)
            configManager.onCharacterWritten()
        }
    }

    private fun executeCopyAll() {
        val clipboard = service.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val text = committer.fullText(ic)
        if (!text.isNullOrEmpty()) {
            clipboard.setPrimaryClip(ClipData.newPlainText("OneHandKeyboard", text))
        }
    }

    private companion object {
        const val TAG = "OneHandKeyboard"
    }
}
