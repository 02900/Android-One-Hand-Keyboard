package com.onehandkeyboard.actions

import com.onehandkeyboard.core.domain.model.KeyAction

/**
 * Configuración de acción que puede ser carácter, acción del teclado o función personalizada
 */
sealed class ActionConfig {
    data class Character(val char: Char) : ActionConfig()

    data class Action(val keyAction: KeyAction) : ActionConfig()

    data class Text(val text: String) : ActionConfig() // Para texto múltiple
    
    data class CustomFunction(val functionId: String, val parameters: Map<String, Any> = emptyMap()) : ActionConfig()
    
    // Acciones específicas del grid 4x4
    object SwitchLayout : ActionConfig()
    object SwitchCase : ActionConfig()
    object Backspace : ActionConfig()
    object Space : ActionConfig()
    object Enter : ActionConfig()
    object ShowNumbers : ActionConfig()
    object ShowLetters : ActionConfig()
}
