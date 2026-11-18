package com.onehandkeyboard.ui

import com.onehandkeyboard.KeyboardLayoutManager
import com.onehandkeyboard.actions.ActionConfig
import com.onehandkeyboard.config.CellConfiguration
import com.onehandkeyboard.core.domain.model.CaseMode
import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.gestures.GestureAction
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
class GestureHintProvider(private val layoutManager: KeyboardLayoutManager) {
    /**
     * Obtiene hints visuales de los gestos configurados para una celda
     */
    fun getConfigurationHints(cellIndex: Int): String {
        val cellConfig =
            layoutManager.keyboardConfiguration.cells.find { it.cellIndex == cellIndex }
                ?: return ""

        val hints = mutableListOf<String>()

        // Procesar cada acción de gesto configurada
        cellConfig.actions.forEach { (gestureAction, actionConfig) ->
            val directionSymbol = getDirectionSymbol(gestureAction.direction)
            val actionSymbol =
                when (actionConfig) {
                    is com.onehandkeyboard.actions.ActionConfig.Character -> applyCaseTransformation(actionConfig.char.toString())
                    is com.onehandkeyboard.actions.ActionConfig.Text -> applyCaseTransformation(actionConfig.text)
                    is com.onehandkeyboard.actions.ActionConfig.Action -> getActionSymbol(actionConfig.keyAction)
                    is com.onehandkeyboard.actions.ActionConfig.SwitchLayout -> getSwitchLayoutLabel()
                    is com.onehandkeyboard.actions.ActionConfig.SwitchCase -> getCaseModeLabel()
                    is com.onehandkeyboard.actions.ActionConfig.Backspace -> "⌫"
                    is com.onehandkeyboard.actions.ActionConfig.Space -> "␣"
                    is com.onehandkeyboard.actions.ActionConfig.Enter -> "↵"
                    is com.onehandkeyboard.actions.ActionConfig.Submit -> "📤"
                    is com.onehandkeyboard.actions.ActionConfig.ShowNumbers -> "123"
                    is com.onehandkeyboard.actions.ActionConfig.ShowLetters -> "ABC"
                    is com.onehandkeyboard.actions.ActionConfig.Paste -> "📋"
                    is com.onehandkeyboard.actions.ActionConfig.CopyAll -> "📄"
                    is com.onehandkeyboard.actions.ActionConfig.DeleteAll -> "🗑️"
                    is com.onehandkeyboard.actions.ActionConfig.IncreaseCellHeight -> "⬆️"
                    is com.onehandkeyboard.actions.ActionConfig.DecreaseCellHeight -> "⬇️"
                    is com.onehandkeyboard.actions.ActionConfig.ResetCellHeight -> "🔄"
                    is com.onehandkeyboard.actions.ActionConfig.CustomFunction -> actionConfig.functionId
                }
            val gestureSymbol =
                when (gestureAction.gestureType) {
                    GestureType.SWIPE -> "→"
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
            is KeyAction.UndoDelete -> "↶"
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
    fun getDirectionalLabel(
        cellConfig: CellConfiguration,
        direction: Direction,
    ): String {
        val actionConfig =
            cellConfig.actions.entries.find { it.key.direction == direction }?.value
                ?: return ""

        return when (actionConfig) {
            is com.onehandkeyboard.actions.ActionConfig.Character -> applyCaseTransformation(actionConfig.char.toString())
            is com.onehandkeyboard.actions.ActionConfig.Text -> applyCaseTransformation(actionConfig.text)
            is com.onehandkeyboard.actions.ActionConfig.Action -> getActionSymbol(actionConfig.keyAction)
            is com.onehandkeyboard.actions.ActionConfig.SwitchLayout -> getSwitchLayoutLabel()
            is com.onehandkeyboard.actions.ActionConfig.SwitchCase -> getCaseModeLabel()
            is com.onehandkeyboard.actions.ActionConfig.Backspace -> "⌫"
            is com.onehandkeyboard.actions.ActionConfig.Space -> "␣"
            is com.onehandkeyboard.actions.ActionConfig.Enter -> "↵"
            is com.onehandkeyboard.actions.ActionConfig.Submit -> "📤"
            is com.onehandkeyboard.actions.ActionConfig.ShowNumbers -> "123"
            is com.onehandkeyboard.actions.ActionConfig.ShowLetters -> "ABC"
            is com.onehandkeyboard.actions.ActionConfig.Paste -> "📋"
            is com.onehandkeyboard.actions.ActionConfig.CopyAll -> "📄"
            is com.onehandkeyboard.actions.ActionConfig.DeleteAll -> "🗑️"
            is com.onehandkeyboard.actions.ActionConfig.IncreaseCellHeight -> "⬆️"
            is com.onehandkeyboard.actions.ActionConfig.DecreaseCellHeight -> "⬇️"
            is com.onehandkeyboard.actions.ActionConfig.ResetCellHeight -> "🔄"
            is com.onehandkeyboard.actions.ActionConfig.CustomFunction -> actionConfig.functionId
        }
    }

    /**
     * Obtiene el label para el centro (solo TAP)
     */
    fun getCenterLabel(cellConfig: CellConfiguration): String {
        val tapActionConfig =
            cellConfig.actions.entries.find {
                it.key.direction == Direction.CENTER && it.key.gestureType == GestureType.TAP
            }?.value

        return when (tapActionConfig) {
            is com.onehandkeyboard.actions.ActionConfig.Character -> applyCaseTransformation(tapActionConfig.char.toString())
            is com.onehandkeyboard.actions.ActionConfig.Text -> applyCaseTransformation(tapActionConfig.text)
            is com.onehandkeyboard.actions.ActionConfig.Action -> getActionSymbol(tapActionConfig.keyAction)
            is com.onehandkeyboard.actions.ActionConfig.SwitchLayout -> getSwitchLayoutLabel()
            is com.onehandkeyboard.actions.ActionConfig.SwitchCase -> getCaseModeLabel()
            is com.onehandkeyboard.actions.ActionConfig.Backspace -> "⌫"
            is com.onehandkeyboard.actions.ActionConfig.Space -> "␣"
            is com.onehandkeyboard.actions.ActionConfig.Enter -> "↵"
            is com.onehandkeyboard.actions.ActionConfig.Submit -> "📤"
            is com.onehandkeyboard.actions.ActionConfig.ShowNumbers -> "123"
            is com.onehandkeyboard.actions.ActionConfig.ShowLetters -> "ABC"
            is com.onehandkeyboard.actions.ActionConfig.Paste -> "📋"
            is com.onehandkeyboard.actions.ActionConfig.CopyAll -> "📄"
            is com.onehandkeyboard.actions.ActionConfig.DeleteAll -> "🗑️"
            is com.onehandkeyboard.actions.ActionConfig.IncreaseCellHeight -> "⬆️"
            is com.onehandkeyboard.actions.ActionConfig.DecreaseCellHeight -> "⬇️"
            is com.onehandkeyboard.actions.ActionConfig.ResetCellHeight -> "🔄"
            is com.onehandkeyboard.actions.ActionConfig.CustomFunction -> tapActionConfig.functionId
            null -> ""
        }
    }

    /**
     * Apply case transformation based on current case mode
     */
    private fun applyCaseTransformation(text: String): String {
        return when (layoutManager.currentCaseMode) {
            CaseMode.UPPERCASE -> text.uppercase()
            CaseMode.LOWERCASE -> text.lowercase()
            CaseMode.CAPITALIZE -> text.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Get the appropriate case mode label for the switch case button
     */
    private fun getCaseModeLabel(): String {
        return when (layoutManager.currentCaseMode) {
            CaseMode.LOWERCASE -> "⇧" // shift off
            CaseMode.CAPITALIZE -> "⬆" // one-shot capital
            CaseMode.UPPERCASE -> "⇪" // caps lock
        }
    }

    /**
     * Get the appropriate layout switch label based on current layout
     */
    private fun getSwitchLayoutLabel(): String {
        return when (layoutManager.currentMode) {
            com.onehandkeyboard.enums.LayoutMode.LETTERS -> "123"  // Show numbers when in letters
            com.onehandkeyboard.enums.LayoutMode.SYMBOLS -> "ABC"  // Show letters when in symbols
        }
    }
}
