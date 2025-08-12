package com.onehandkeyboard.actions

import com.onehandkeyboard.core.domain.model.KeyAction

/**
 * Configuración de acción que puede ser carácter o acción del teclado
 */
sealed class ActionConfig {
    data class Character(val char: Char) : ActionConfig()

    data class Action(val keyAction: KeyAction) : ActionConfig()

    data class Text(val text: String) : ActionConfig() // Para texto múltiple
}
