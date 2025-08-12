package com.onehandkeyboard.ui

import com.onehandkeyboard.KeyboardConfigManager
import com.onehandkeyboard.config.CellConfiguration
import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.GestureType
import com.onehandkeyboard.core.domain.model.KeyAction

/**
 * Proveedor de hints visuales y símbolos para gestos del teclado.
 * 
 * Responsabilidades:
 * - Generar hints visuales de gestos configurados
 * - Proporcionar símbolos para direcciones y acciones
 * - Gestionar la representación visual de configuraciones
 */
class GestureHintProvider(private val configManager: KeyboardConfigManager) {
    
    /**
     * Obtiene hints visuales de los gestos configurados para una celda
     */
    fun getConfigurationHints(cellIndex: Int): String {
        val cellConfig = configManager.keyboardConfiguration.cells.find { it.cellIndex == cellIndex }
            ?: return ""
        
        val hints = mutableListOf<String>()
        
        // Procesar cada acción de gesto configurada
        cellConfig.actions.forEach { (gestureAction, actionConfig) ->
            val directionSymbol = getDirectionSymbol(gestureAction.direction)
            val actionSymbol = when (actionConfig) {
                is com.onehandkeyboard.actions.ActionConfig.Character -> actionConfig.char.toString()
                is com.onehandkeyboard.actions.ActionConfig.Text -> actionConfig.text
                is com.onehandkeyboard.actions.ActionConfig.Action -> getActionSymbol(actionConfig.action)
            }
            val gestureSymbol = when (gestureAction.gestureType) {
                GestureType.SWIPE -> "→"
                GestureType.LONG_PRESS -> "⏱"
                GestureType.TAP -> "●"
                GestureType.FLICK -> "⚡"
                GestureType.DRAG -> "↔"
            }
            hints.add("$directionSymbol$gestureSymbol$actionSymbol")
        }
        
        return hints.joinToString(" ")
    }
    
    /**
     * Obtiene el símbolo visual para una dirección
     */
    fun getDirectionSymbol(direction: Direction): String {
        return when (direction) {
            Direction.N -> "↑"
            Direction.NE -> "↗"
            Direction.E -> "→"
            Direction.SE -> "↘"
            Direction.S -> "↓"
            Direction.SW -> "↙"
            Direction.W -> "←"
            Direction.NW -> "↖"
            Direction.CENTER -> "●"
        }
    }
    
    /**
     * Obtiene el símbolo visual para una acción
     */
    fun getActionSymbol(action: KeyAction): String {
        return when (action) {
            is KeyAction.Backspace -> "⌫"
            is KeyAction.Space -> "␣"
            is KeyAction.Enter -> "↵"
            is KeyAction.ToggleCase -> "Aa"
            is KeyAction.SwitchToNumbers -> "123"
            is KeyAction.SwitchToLetters -> "ABC"
            is KeyAction.InsertText -> action.text.take(2)
            else -> "?"
        }
    }
    
    /**
     * Obtiene hints visuales de los gestos disponibles para una celda (método legacy)
     */
    fun getLegacyGestureHints(cellIndex: Int): String {
        val hints = mutableListOf<String>()
        
        // Ejemplo de hints básicos - esto se puede expandir
        when (cellIndex) {
            0 -> hints.addAll(listOf("↑á", "←à", "→â"))
            1 -> hints.addAll(listOf("↑é", "←è", "→ê"))
            2 -> hints.addAll(listOf("↑í", "←ì", "→î"))
            3 -> hints.addAll(listOf("↑ó", "←ò", "→ô"))
            4 -> hints.addAll(listOf("↑ú", "←ù", "→û"))
            5 -> hints.addAll(listOf("↑ñ", "←ç", "→ü"))
            6 -> hints.addAll(listOf("↑¿", "←¡", "→!"))
            7 -> hints.addAll(listOf("↑.", "←,", "→;"))
        }
        
        return hints.joinToString(" ")
    }
    
    /**
     * Obtiene el label para una dirección específica
     */
    fun getDirectionalLabel(cellConfig: CellConfiguration, direction: Direction): String {
        val actionConfig = cellConfig.actions.entries.find { it.key.direction == direction }?.value
            ?: return ""
        
        return when (actionConfig) {
            is com.onehandkeyboard.actions.ActionConfig.Character -> actionConfig.char.toString()
            is com.onehandkeyboard.actions.ActionConfig.Text -> actionConfig.text
            is com.onehandkeyboard.actions.ActionConfig.Action -> getActionSymbol(actionConfig.action)
        }
    }
    
    /**
     * Obtiene el label para el centro (combinando TAP y LONG_PRESS)
     */
    fun getCenterLabel(cellConfig: CellConfiguration): String {
        val tapActionConfig = cellConfig.actions.entries.find { 
            it.key.direction == Direction.CENTER && it.key.gestureType == GestureType.TAP 
        }?.value
        
        val longPressActionConfig = cellConfig.actions.entries.find { 
            it.key.direction == Direction.CENTER && it.key.gestureType == GestureType.LONG_PRESS 
        }?.value
        
        val tapLabel = tapActionConfig?.let { actionConfig ->
            when (actionConfig) {
                is com.onehandkeyboard.actions.ActionConfig.Character -> actionConfig.char.toString()
                is com.onehandkeyboard.actions.ActionConfig.Text -> actionConfig.text
                is com.onehandkeyboard.actions.ActionConfig.Action -> getActionSymbol(actionConfig.action)
            }
        } ?: ""
        
        val longPressLabel = longPressActionConfig?.let { actionConfig ->
            when (actionConfig) {
                is com.onehandkeyboard.actions.ActionConfig.Character -> actionConfig.char.toString()
                is com.onehandkeyboard.actions.ActionConfig.Text -> actionConfig.text
                is com.onehandkeyboard.actions.ActionConfig.Action -> getActionSymbol(actionConfig.action)
            }
        } ?: ""
        
        return when {
            tapLabel.isNotEmpty() && longPressLabel.isNotEmpty() -> "$tapLabel ($longPressLabel)"
            tapLabel.isNotEmpty() -> tapLabel
            longPressLabel.isNotEmpty() -> "($longPressLabel)"
            else -> ""
        }
    }
}
