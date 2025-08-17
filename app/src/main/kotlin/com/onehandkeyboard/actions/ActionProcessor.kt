package com.onehandkeyboard.actions

import android.util.Log
import com.onehandkeyboard.KeyboardActionProcessor
import com.onehandkeyboard.KeyboardLayoutManager
import com.onehandkeyboard.core.domain.model.KeyAction
import com.onehandkeyboard.core.domain.model.KeyboardOutput

/**
 * Procesador de acciones del teclado que maneja ActionConfig
 */
class ActionProcessor(
    private val actionProcessor: KeyboardActionProcessor,
    private val layoutManager: KeyboardLayoutManager
) {
    
    /**
     * Procesa una ActionConfig y la convierte en KeyboardOutput
     */
    fun processAction(actionConfig: ActionConfig): KeyboardOutput? {
        return when (actionConfig) {
            is ActionConfig.Character -> {
                KeyboardOutput.Character(actionConfig.char.toString())
            }
            
            is ActionConfig.Text -> {
                KeyboardOutput.Character(actionConfig.text)
            }
            
            is ActionConfig.Action -> {
                KeyboardOutput.Action(actionConfig.keyAction)
            }
            
            is ActionConfig.SwitchLayout -> {
                layoutManager.switchLayout()
                null // No output, just internal state change
            }
            
            is ActionConfig.SwitchCase -> {
                layoutManager.switchCaseMode()
                null // No output, just internal state change
            }
            
            is ActionConfig.Backspace -> {
                KeyboardOutput.Action(KeyAction.Backspace)
            }
            
            is ActionConfig.Space -> {
                KeyboardOutput.Action(KeyAction.Space)
            }
            
            is ActionConfig.Enter -> {
                KeyboardOutput.Action(KeyAction.Enter)
            }
            
            is ActionConfig.ShowNumbers -> {
                // Switch to numbers mode
                layoutManager.switchToSymbols()
                null
            }
            
            is ActionConfig.ShowLetters -> {
                // Switch to letters mode
                layoutManager.switchToLetters()
                null
            }
            
            is ActionConfig.Paste -> {
                KeyboardOutput.Action(KeyAction.Paste)
            }
            is ActionConfig.CopyAll -> {
                KeyboardOutput.Action(KeyAction.CopyAll)
            }
            is ActionConfig.DeleteAll -> {
                KeyboardOutput.Action(KeyAction.DeleteAll)
            }
            
            is ActionConfig.CustomFunction -> {
                processCustomFunction(actionConfig.functionId, actionConfig.parameters)
            }
        }
    }
    
    /**
     * Procesa funciones personalizadas
     */
    private fun processCustomFunction(functionId: String, parameters: Map<String, Any>): KeyboardOutput? {
        Log.d("ActionProcessor", "Processing custom function: $functionId with params: $parameters")
        
        return when (functionId) {
            "toggle_caps_lock" -> {
                // Ejemplo: alternar caps lock permanente
                layoutManager.toggleCapsLock()
                null
            }
            
            "insert_emoji" -> {
                // Ejemplo: insertar emoji específico
                val emoji = parameters["emoji"] as? String ?: "😊"
                KeyboardOutput.Character(emoji)
            }
            
            "quick_text" -> {
                // Ejemplo: texto rápido predefinido
                val text = parameters["text"] as? String ?: ""
                KeyboardOutput.Character(text)
            }
            
            "voice_input" -> {
                // Ejemplo: activar entrada de voz (placeholder)
                Log.i("ActionProcessor", "Voice input requested")
                null
            }
            
            else -> {
                Log.w("ActionProcessor", "Unknown custom function: $functionId")
                null
            }
        }
    }
}
