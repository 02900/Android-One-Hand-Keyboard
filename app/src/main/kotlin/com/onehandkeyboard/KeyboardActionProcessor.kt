package com.onehandkeyboard

import android.inputmethodservice.InputMethodService
import android.util.Log
import com.onehandkeyboard.core.domain.model.KeyAction
import com.onehandkeyboard.core.domain.model.KeyboardOutput

/**
 * Processes keyboard actions and text input for the OneHandKeyboard.
 *
 * This class centralizes all keyboard action processing logic including:
 * - Processing keyboard output (characters and actions)
 * - Executing specific actions (backspace, space, enter, etc.)
 * - Managing input connections with target applications
 * - Integrating with keyboard configuration and case modes
 * - Handling layout and case mode switching callbacks
 *
 * Extracted from OneHandKeyboardService for better separation of concerns.
 * Uses manual initialization due to InputMethodService dependency requirements.
 */
class KeyboardActionProcessor(
    private val service: InputMethodService,
    private val configManager: KeyboardConfigManager,
    private val onLayoutSwitch: () -> Unit,
    private val onCaseSwitch: () -> Unit,
) {
    // ============================================================================
    // REGION: KEYBOARD OUTPUT PROCESSING
    // ============================================================================

    /**
     * Procesa la salida del teclado (caracteres o acciones)
     */
    fun processKeyboardOutput(output: KeyboardOutput) {
        when (output) {
            is KeyboardOutput.Character -> {
                processCharacterOutput(output.char)
            }
            is KeyboardOutput.Action -> {
                processActionOutput(output.action)
            }
        }
    }

    /**
     * Procesa la salida de un carácter
     */
    private fun processCharacterOutput(char: String) {
        Log.d("OneHandKeyboard", "🎯 SENDING CHARACTER: '$char'")

        val inputConnection = service.currentInputConnection
        if (inputConnection == null) {
            Log.e("OneHandKeyboard", "🎯 ERROR: currentInputConnection is NULL!")
            return
        }

        val result = inputConnection.commitText(char, 1)
        Log.d("OneHandKeyboard", "🎯 commitText result: $result")

        // Manejar modo capitalize
        configManager.onCharacterWritten()
    }

    /**
     * Procesa la salida de una acción
     */
    private fun processActionOutput(action: KeyAction) {
        Log.d("OneHandKeyboard", "🎯 EXECUTING ACTION: $action")
        executeKeyAction(action)
    }

    // ============================================================================
    // REGION: ACTION EXECUTION
    // ============================================================================

    /**
     * Ejecuta una acción de teclado específica
     */
    private fun executeKeyAction(action: KeyAction) {
        when (action) {
            is KeyAction.Backspace -> {
                executeBackspace()
            }
            is KeyAction.Space -> {
                executeSpace()
            }
            is KeyAction.Enter -> {
                executeEnter()
            }
            is KeyAction.ToggleCase -> {
                executeToggleCase()
            }
            is KeyAction.SwitchToNumbers -> {
                executeSwitchToNumbers()
            }
            is KeyAction.SwitchToLetters -> {
                executeSwitchToLetters()
            }
            is KeyAction.InsertText -> {
                executeInsertText(action.text)
            }
            else -> {
                Log.d("OneHandKeyboard", "Action not implemented: $action")
            }
        }
    }

    // ============================================================================
    // REGION: SPECIFIC ACTION IMPLEMENTATIONS
    // ============================================================================

    /**
     * Ejecuta acción de backspace - borra selección actual o un carácter hacia atrás
     */
    private fun executeBackspace() {
        val inputConnection = service.currentInputConnection
        if (inputConnection != null) {
            // Obtener información sobre la selección actual
            val selectedText = inputConnection.getSelectedText(0)
            
            if (selectedText != null && selectedText.isNotEmpty()) {
                // Si hay texto seleccionado, borrarlo completamente
                inputConnection.commitText("", 1)
                Log.d("OneHandKeyboard", "🎯 Backspace executed - deleted selection: '${selectedText}'")
            } else {
                // Si no hay selección, borrar un carácter hacia atrás (comportamiento estándar)
                inputConnection.deleteSurroundingText(1, 0)
                Log.d("OneHandKeyboard", "🎯 Backspace executed - deleted one character")
            }
        } else {
            Log.e("OneHandKeyboard", "🎯 ERROR: Cannot execute backspace - no input connection")
        }
    }

    /**
     * Ejecuta acción de espacio
     */
    private fun executeSpace() {
        val inputConnection = service.currentInputConnection
        if (inputConnection != null) {
            inputConnection.commitText(" ", 1)
            Log.d("OneHandKeyboard", "🎯 Space executed")
        } else {
            Log.e("OneHandKeyboard", "🎯 ERROR: Cannot execute space - no input connection")
        }
    }

    /**
     * Ejecuta acción de enter
     */
    private fun executeEnter() {
        val inputConnection = service.currentInputConnection
        if (inputConnection != null) {
            inputConnection.commitText("\n", 1)
            Log.d("OneHandKeyboard", "🎯 Enter executed")
        } else {
            Log.e("OneHandKeyboard", "🎯 ERROR: Cannot execute enter - no input connection")
        }
    }

    /**
     * Ejecuta acción de cambio de modo de mayúsculas
     */
    private fun executeToggleCase() {
        onCaseSwitch()
        Log.d("OneHandKeyboard", "🎯 Case mode toggled")
    }

    /**
     * Ejecuta acción de cambio a números
     */
    private fun executeSwitchToNumbers() {
        configManager.switchLayout()
        onLayoutSwitch()
        Log.d("OneHandKeyboard", "🎯 Switched to numbers layout")
    }

    /**
     * Ejecuta acción de cambio a letras
     */
    private fun executeSwitchToLetters() {
        configManager.switchLayout()
        onLayoutSwitch()
        Log.d("OneHandKeyboard", "🎯 Switched to letters layout")
    }

    /**
     * Ejecuta acción de insertar texto
     */
    private fun executeInsertText(text: String) {
        val inputConnection = service.currentInputConnection
        if (inputConnection != null) {
            inputConnection.commitText(text, 1)
            Log.d("OneHandKeyboard", "🎯 Text inserted: '$text'")
        } else {
            Log.e("OneHandKeyboard", "🎯 ERROR: Cannot insert text - no input connection")
        }
    }

    // ============================================================================
    // REGION: UTILITY METHODS
    // ============================================================================

    /**
     * Verifica si hay una conexión de entrada válida
     */
    fun hasValidInputConnection(): Boolean {
        return service.currentInputConnection != null
    }

    /**
     * Obtiene información sobre la conexión de entrada actual
     */
    fun getInputConnectionInfo(): String {
        val inputConnection = service.currentInputConnection
        return if (inputConnection != null) {
            "InputConnection available: ${inputConnection.javaClass.simpleName}"
        } else {
            "No InputConnection available"
        }
    }
}

// KeyboardOutput moved to core/domain/model/KeyboardOutput.kt
