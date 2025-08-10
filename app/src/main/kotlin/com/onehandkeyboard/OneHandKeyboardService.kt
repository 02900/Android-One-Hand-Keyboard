package com.onehandkeyboard

import android.inputmethodservice.InputMethodService
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.onehandkeyboard.ui.SimpleKeyboardGrid
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main InputMethodService for OneHandKeyboard
 * 
 * This service integrates Jetpack Compose with Android's IME framework,
 * providing a modern UI for the 3x3 gesture keyboard.
 */
@AndroidEntryPoint
class OneHandKeyboardService : InputMethodService() {

    private var composeView: ComposeView? = null

    override fun onCreateInputView(): View {
        // Create ComposeView for our keyboard UI
        composeView = ComposeView(this).apply {
            // Use DisposeOnViewTreeLifecycleDestroyed to properly handle composition lifecycle
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            
            setContent {
                OneHandKeyboardTheme {
                    KeyboardContent(
                        onTextInput = { text ->
                            // Handle text input from keyboard
                            currentInputConnection?.commitText(text, 1)
                        },
                        onBackspace = {
                            // Handle backspace
                            currentInputConnection?.deleteSurroundingText(1, 0)
                        },
                        onAction = { action ->
                            // Handle IME actions (Done, Next, etc.)
                            currentInputConnection?.performEditorAction(action)
                        }
                    )
                }
            }
        }
        
        return composeView!!
    }

    override fun onDestroy() {
        composeView = null
        super.onDestroy()
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // TODO: Configure keyboard based on input type and IME options
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        // TODO: Clean up any ongoing operations
    }
}

/**
 * Main theme wrapper for the keyboard
 */
@Composable
private fun OneHandKeyboardTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
            content = content
        )
    }
}

/**
 * Main keyboard content composable
 */
@Composable
private fun KeyboardContent(
    onTextInput: (String) -> Unit,
    onBackspace: () -> Unit,
    onAction: (Int) -> Unit
) {
    // For now, we'll create a simple 3x3 grid placeholder
    // This will be replaced with the actual gesture keyboard in later stages
    SimpleKeyboardGrid(
        onTextInput = onTextInput,
        onBackspace = onBackspace,
        onAction = onAction
    )
}
