package com.onehandkeyboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.util.Log
import com.onehandkeyboard.core.domain.model.KeyAction
import com.onehandkeyboard.core.domain.model.KeyboardOutput
import com.onehandkeyboard.enums.LayoutMode
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
    private val onToggleOneHand: () -> Unit = {},
    private val onSwitchInputMethod: () -> Unit = {},
    private val committer: InputCommitter = InputCommitter(),
) {
    private val ic get() = service.currentInputConnection
    private val editorInfo get() = service.currentInputEditorInfo

    fun processKeyboardOutput(output: KeyboardOutput) {
        when (output) {
            is KeyboardOutput.Character -> {
                committer.resetSpaceShortcut()
                committer.commitText(ic, output.char)
                if (configManager.onCharacterWritten()) onCaseSwitch()
            }
            is KeyboardOutput.Action -> {
                if (output.action != KeyAction.Space) committer.resetSpaceShortcut()
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
        committer.space(ic, SystemClock.uptimeMillis(), InputCommitter.allowsPeriodShortcut(editorInfo))
    }

    private fun executeBackspace() {
        // Backspace right after a dead key just cancels it.
        if (configManager.clearDeadKey()) {
            onCaseSwitch()
            return
        }
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
            is KeyAction.SwitchLanguage -> {
                configManager.setLanguage(configManager.currentLanguage.other())
                onLayoutSwitch()
            }
            is KeyAction.ToggleOneHand -> onToggleOneHand()
            is KeyAction.SwitchInputMethod -> onSwitchInputMethod()
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
    }
}
