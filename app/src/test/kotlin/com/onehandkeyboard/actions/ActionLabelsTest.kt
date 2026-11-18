package com.onehandkeyboard.actions

import com.onehandkeyboard.core.domain.model.CaseMode
import com.onehandkeyboard.core.domain.model.KeyAction
import com.onehandkeyboard.core.domain.model.KeyboardOutput
import com.onehandkeyboard.enums.LayoutMode
import com.onehandkeyboard.input.DeadKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ActionLabelsTest {
    private val allSimpleActions =
        listOf(
            ActionConfig.SwitchLayout, ActionConfig.SwitchCase, ActionConfig.Backspace, ActionConfig.Space,
            ActionConfig.Enter, ActionConfig.Submit, ActionConfig.ShowNumbers, ActionConfig.ShowLetters,
            ActionConfig.Paste, ActionConfig.CopyAll, ActionConfig.DeleteAll,
        )

    @Test
    fun `letters follow the case mode`() {
        val a = ActionConfig.Character('a')
        assertEquals("a", a.label(LabelContext(caseMode = CaseMode.LOWERCASE)))
        assertEquals("A", a.label(LabelContext(caseMode = CaseMode.CAPITALIZE)))
        assertEquals("A", a.label(LabelContext(caseMode = CaseMode.UPPERCASE)))
    }

    @Test
    fun `a pending dead key previews accented vowels only`() {
        val ctx = LabelContext(pendingDeadKey = DeadKeys.ACUTE)
        assertEquals("é", ActionConfig.Character('e').label(ctx))
        assertEquals("k", ActionConfig.Character('k').label(ctx))
        assertEquals(",", ActionConfig.Character(',').label(ctx))
    }

    @Test
    fun `state dependent keys show the current state`() {
        assertEquals("123", ActionConfig.SwitchLayout.label(LabelContext(layoutMode = LayoutMode.LETTERS)))
        assertEquals("ABC", ActionConfig.SwitchLayout.label(LabelContext(layoutMode = LayoutMode.SYMBOLS)))
        assertEquals("⇪", ActionConfig.SwitchCase.label(LabelContext(caseMode = CaseMode.UPPERCASE)))
        assertEquals("🔍", ActionConfig.Enter.label(LabelContext(enterLabel = "🔍")))
        assertEquals("EN", ActionConfig.Action(KeyAction.SwitchLanguage).label(LabelContext(languageLabel = "EN")))
        assertEquals("´", ActionConfig.Action(KeyAction.DeadKey(DeadKeys.ACUTE)).label(LabelContext()))
    }

    @Test
    fun `every built-in action has a label and an output`() {
        allSimpleActions.forEach { action ->
            assert(action.label(LabelContext()).isNotEmpty()) { "$action has no label" }
            assertNotNull("$action has no output", action.toKeyboardOutput())
        }
        assertNull(ActionConfig.CustomFunction("x").toKeyboardOutput())
    }

    @Test
    fun `typed text goes through the case and dead key transform`() {
        val output = ActionConfig.Character('e').toKeyboardOutput { it.uppercase() }
        assertEquals(KeyboardOutput.Character("E"), output)
        assertEquals(KeyboardOutput.Action(KeyAction.SwitchToNumbers), ActionConfig.SwitchLayout.toKeyboardOutput())
        assertEquals(KeyboardOutput.Action(KeyAction.SwitchToLetters), ActionConfig.ShowLetters.toKeyboardOutput())
    }

    @Test
    fun `spoken names describe control keys in words`() {
        assertEquals("borrar", ActionConfig.Backspace.spokenName(LabelContext()))
        assertEquals("tilde", ActionConfig.Action(KeyAction.DeadKey(DeadKeys.ACUTE)).spokenName(LabelContext()))
        assertEquals("borrar palabra", ActionConfig.Action(KeyAction.DeleteWord).spokenName(LabelContext()))
        assertEquals("ñ", ActionConfig.Character('ñ').spokenName(LabelContext()))
    }
}
