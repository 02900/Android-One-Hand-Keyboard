package com.onehandkeyboard.core.domain.model

/**
 * Sealed class para representar la salida del teclado
 * 
 * Representa los diferentes tipos de salida que puede generar el teclado:
 * - Character: Un carácter individual para insertar
 * - Action: Una acción específica del teclado (backspace, enter, etc.)
 */
sealed class KeyboardOutput {
    /**
     * Representa un carácter que debe ser insertado en el texto
     */
    data class Character(val char: String) : KeyboardOutput()
    
    /**
     * Representa una acción específica del teclado que debe ser ejecutada
     */
    data class Action(val action: KeyAction) : KeyboardOutput()
}
