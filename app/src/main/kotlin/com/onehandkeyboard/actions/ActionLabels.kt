package com.onehandkeyboard.actions

import com.onehandkeyboard.core.domain.model.CaseMode
import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.KeyAction
import com.onehandkeyboard.core.domain.model.KeyboardOutput
import com.onehandkeyboard.enums.LayoutMode
import com.onehandkeyboard.input.DeadKeys

/** Visible label of an action (single source for every key label). */
fun ActionConfig.label(ctx: LabelContext): String =
    when (this) {
        is ActionConfig.Character -> ctx.letter(char.toString())
        is ActionConfig.Text -> ctx.letter(text)
        is ActionConfig.Action -> keyAction.label(ctx)
        is ActionConfig.CustomFunction -> functionId
        ActionConfig.SwitchLayout -> if (ctx.layoutMode == LayoutMode.LETTERS) "123" else "ABC"
        ActionConfig.SwitchCase -> caseLabel(ctx.caseMode)
        ActionConfig.Backspace -> "⌫"
        ActionConfig.Space -> "␣"
        ActionConfig.Enter -> ctx.enterLabel
        ActionConfig.Submit -> "📤"
        ActionConfig.ShowNumbers -> "123"
        ActionConfig.ShowLetters -> "ABC"
        ActionConfig.Paste -> "📋"
        ActionConfig.CopyAll -> "📄"
        ActionConfig.DeleteAll -> "🗑️"
        ActionConfig.IncreaseCellHeight -> "⬆️"
        ActionConfig.DecreaseCellHeight -> "⬇️"
        ActionConfig.ResetCellHeight -> "🔄"
    }

fun KeyAction.label(ctx: LabelContext): String =
    when (this) {
        KeyAction.Backspace -> "⌫"
        KeyAction.Space -> "␣"
        KeyAction.Enter -> ctx.enterLabel
        KeyAction.Submit -> "📤"
        KeyAction.ToggleCase -> caseLabel(ctx.caseMode)
        KeyAction.SwitchToNumbers, KeyAction.SwitchToSymbols -> "123"
        KeyAction.SwitchToLetters -> "ABC"
        KeyAction.CursorLeft -> "◂"
        KeyAction.CursorRight -> "▸"
        KeyAction.SelectAll -> "⬚"
        KeyAction.Cut -> "✂"
        KeyAction.Copy, KeyAction.CopyAll -> "📄"
        KeyAction.Paste -> "📋"
        KeyAction.DeleteAll -> "🗑️"
        KeyAction.UndoDelete -> "↶"
        KeyAction.DeleteWord -> "⇤"
        KeyAction.SwitchLanguage -> ctx.languageLabel
        KeyAction.ShowMemes -> "🖼️"
        is KeyAction.DeadKey -> accent.toString()
        KeyAction.IncreaseCellHeight -> "⬆️"
        KeyAction.DecreaseCellHeight -> "⬇️"
        KeyAction.ResetCellHeight -> "🔄"
        is KeyAction.Custom -> "?"
        is KeyAction.InsertText -> text.take(2)
    }

/** Spoken description for accessibility services (TalkBack). */
fun ActionConfig.spokenName(ctx: LabelContext): String =
    when (this) {
        is ActionConfig.Character, is ActionConfig.Text -> label(ctx)
        is ActionConfig.Action -> keyAction.spokenName(ctx)
        is ActionConfig.CustomFunction -> functionId
        ActionConfig.SwitchLayout -> if (ctx.layoutMode == LayoutMode.LETTERS) "números" else "letras"
        ActionConfig.SwitchCase -> caseSpoken(ctx.caseMode)
        ActionConfig.Backspace -> "borrar"
        ActionConfig.Space -> "espacio"
        ActionConfig.Enter -> "intro"
        ActionConfig.Submit -> "enviar"
        ActionConfig.ShowNumbers -> "números"
        ActionConfig.ShowLetters -> "letras"
        ActionConfig.Paste -> "pegar"
        ActionConfig.CopyAll -> "copiar todo"
        ActionConfig.DeleteAll -> "borrar todo"
        ActionConfig.IncreaseCellHeight -> "teclado más alto"
        ActionConfig.DecreaseCellHeight -> "teclado más bajo"
        ActionConfig.ResetCellHeight -> "altura por defecto"
    }

private fun KeyAction.spokenName(ctx: LabelContext): String =
    when (this) {
        KeyAction.UndoDelete -> "deshacer borrado"
        KeyAction.DeleteWord -> "borrar palabra"
        KeyAction.SwitchLanguage -> "cambiar idioma"
        KeyAction.ShowMemes -> "memes"
        KeyAction.CursorLeft -> "cursor a la izquierda"
        KeyAction.CursorRight -> "cursor a la derecha"
        is KeyAction.DeadKey -> if (accent == DeadKeys.ACUTE) "tilde" else "diéresis"
        else -> label(ctx)
    }

fun Direction.spokenName(): String =
    when (this) {
        Direction.N -> "arriba"
        Direction.NE -> "arriba a la derecha"
        Direction.E -> "derecha"
        Direction.SE -> "abajo a la derecha"
        Direction.S -> "abajo"
        Direction.SW -> "abajo a la izquierda"
        Direction.W -> "izquierda"
        Direction.NW -> "arriba a la izquierda"
        Direction.CENTER -> "toque"
    }

/**
 * Maps a configured action to what the keyboard emits. [transform] applies case and
 * dead keys to typed text.
 */
fun ActionConfig.toKeyboardOutput(transform: (String) -> String = { it }): KeyboardOutput? =
    when (this) {
        is ActionConfig.Character -> KeyboardOutput.Character(transform(char.toString()))
        is ActionConfig.Text -> KeyboardOutput.Character(transform(text))
        is ActionConfig.Action -> KeyboardOutput.Action(keyAction)
        is ActionConfig.CustomFunction -> null
        ActionConfig.SwitchLayout, ActionConfig.ShowNumbers -> KeyboardOutput.Action(KeyAction.SwitchToNumbers)
        ActionConfig.ShowLetters -> KeyboardOutput.Action(KeyAction.SwitchToLetters)
        ActionConfig.SwitchCase -> KeyboardOutput.Action(KeyAction.ToggleCase)
        ActionConfig.Backspace -> KeyboardOutput.Action(KeyAction.Backspace)
        ActionConfig.Space -> KeyboardOutput.Action(KeyAction.Space)
        ActionConfig.Enter -> KeyboardOutput.Action(KeyAction.Enter)
        ActionConfig.Submit -> KeyboardOutput.Action(KeyAction.Submit)
        ActionConfig.Paste -> KeyboardOutput.Action(KeyAction.Paste)
        ActionConfig.CopyAll -> KeyboardOutput.Action(KeyAction.CopyAll)
        ActionConfig.DeleteAll -> KeyboardOutput.Action(KeyAction.DeleteAll)
        ActionConfig.IncreaseCellHeight -> KeyboardOutput.Action(KeyAction.IncreaseCellHeight)
        ActionConfig.DecreaseCellHeight -> KeyboardOutput.Action(KeyAction.DecreaseCellHeight)
        ActionConfig.ResetCellHeight -> KeyboardOutput.Action(KeyAction.ResetCellHeight)
    }

private fun caseLabel(mode: CaseMode) =
    when (mode) {
        CaseMode.LOWERCASE -> "⇧" // shift off
        CaseMode.CAPITALIZE -> "⬆" // one-shot capital
        CaseMode.UPPERCASE -> "⇪" // caps lock
    }

private fun caseSpoken(mode: CaseMode) =
    when (mode) {
        CaseMode.LOWERCASE -> "mayúscula"
        CaseMode.CAPITALIZE -> "mayúscula activada"
        CaseMode.UPPERCASE -> "bloqueo de mayúsculas"
    }
