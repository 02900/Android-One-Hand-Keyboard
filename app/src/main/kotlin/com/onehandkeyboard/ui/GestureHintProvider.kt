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
        for (gestureAction in cellConfig.gestureActions) {
            val directionSymbol = getDirectionSymbol(gestureAction.direction)
            val actionSymbol = when (val action = gestureAction.action) {
                is com.onehandkeyboard.actions.ActionConfig.Character -> action.char.toString()
                is com.onehandkeyboard.actions.ActionConfig.Text -> action.text
                is com.onehandkeyboard.actions.ActionConfig.Action -> getActionSymbol(action.keyAction)
            }
            val gestureSymbol = when (gestureAction.gestureType) {
                GestureType.SWIPE -> "→"
                GestureType.LONG_PRESS -> "⏱"
                else -> ""
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
        val gestureAction = cellConfig.gestureActions.find { it.direction == direction }
            ?: return ""
        
        return when (val action = gestureAction.action) {
            is com.onehandkeyboard.actions.ActionConfig.Character -> action.char.toString()
            is com.onehandkeyboard.actions.ActionConfig.Text -> action.text
            is com.onehandkeyboard.actions.ActionConfig.Action -> getActionSymbol(action.keyAction)
        }
    }
    
    /**
     * Obtiene el label para el centro (combinando TAP y LONG_PRESS)
     */
    fun getCenterLabel(cellConfig: CellConfiguration): String {
        val tapAction = cellConfig.gestureActions.find { 
            it.direction == Direction.CENTER && it.gestureType == GestureType.TAP 
        }
        val longPressAction = cellConfig.gestureActions.find { 
            it.direction == Direction.CENTER && it.gestureType == GestureType.LONG_PRESS 
        }
        
        val tapLabel = tapAction?.let { gestureAction ->
            when (val action = gestureAction.action) {
                is com.onehandkeyboard.actions.ActionConfig.Character -> action.char.toString()
                is com.onehandkeyboard.actions.ActionConfig.Text -> action.text
                is com.onehandkeyboard.actions.ActionConfig.Action -> getActionSymbol(action.keyAction)
            }
        } ?: ""
        
        val longPressLabel = longPressAction?.let { gestureAction ->
            when (val action = gestureAction.action) {
                is com.onehandkeyboard.actions.ActionConfig.Character -> action.char.toString()
                is com.onehandkeyboard.actions.ActionConfig.Text -> action.text
                is com.onehandkeyboard.actions.ActionConfig.Action -> getActionSymbol(action.keyAction)
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
