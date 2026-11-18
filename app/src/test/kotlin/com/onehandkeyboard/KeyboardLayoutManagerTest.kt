package com.onehandkeyboard

import android.text.InputType
import com.onehandkeyboard.config.KeyboardConfigurationFactory
import com.onehandkeyboard.core.domain.model.CaseMode
import com.onehandkeyboard.enums.LayoutMode
import com.onehandkeyboard.input.DeadKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class KeyboardLayoutManagerTest {
    private var now = 1_000L
    private val manager = KeyboardLayoutManager(KeyboardConfigurationFactory()).apply { clock = { now } }

    private fun tapShift(afterMs: Long = 1_000L) {
        now += afterMs
        manager.switchCaseMode()
    }

    @Test
    fun `single shift tap capitalizes only the next letter`() {
        tapShift()
        assertEquals(CaseMode.CAPITALIZE, manager.currentCaseMode)
        assertEquals("A", manager.transformChar("a"))

        assertTrue("label refresh needed", manager.onCharacterWritten())
        assertEquals(CaseMode.LOWERCASE, manager.currentCaseMode)
        assertEquals("b", manager.transformChar("b"))
    }

    @Test
    fun `double shift tap locks caps until tapped again`() {
        tapShift()
        tapShift(afterMs = 150)
        assertEquals(CaseMode.UPPERCASE, manager.currentCaseMode)

        assertFalse(manager.onCharacterWritten())
        assertEquals("C", manager.transformChar("c"))

        tapShift()
        assertEquals(CaseMode.LOWERCASE, manager.currentCaseMode)
    }

    @Test
    fun `slow second tap turns one-shot off instead of locking`() {
        tapShift()
        tapShift(afterMs = 600)
        assertEquals(CaseMode.LOWERCASE, manager.currentCaseMode)
    }

    @Test
    fun `auto shift capitalizes at sentence start and is consumed by a letter`() {
        assertTrue(manager.updateAutoShift(shouldCapitalize = true))
        assertTrue(manager.isAutoShifted)
        assertEquals("H", manager.transformChar("h"))

        manager.onCharacterWritten()
        assertFalse(manager.updateAutoShift(shouldCapitalize = false))
        assertEquals(CaseMode.LOWERCASE, manager.currentCaseMode)
    }

    @Test
    fun `auto shift is withdrawn when the cursor leaves a sentence start`() {
        manager.updateAutoShift(shouldCapitalize = true)
        assertTrue(manager.updateAutoShift(shouldCapitalize = false))
        assertEquals(CaseMode.LOWERCASE, manager.currentCaseMode)
    }

    @Test
    fun `auto shift never overrides caps lock`() {
        tapShift()
        tapShift(afterMs = 100)
        assertFalse(manager.updateAutoShift(shouldCapitalize = false))
        assertEquals(CaseMode.UPPERCASE, manager.currentCaseMode)
    }

    @Test
    fun `numeric phone and date fields open on the symbols layer`() {
        assertEquals(LayoutMode.SYMBOLS, KeyboardLayoutManager.initialModeFor(InputType.TYPE_CLASS_NUMBER))
        assertEquals(LayoutMode.SYMBOLS, KeyboardLayoutManager.initialModeFor(InputType.TYPE_CLASS_PHONE))
        assertEquals(LayoutMode.SYMBOLS, KeyboardLayoutManager.initialModeFor(InputType.TYPE_CLASS_DATETIME))
        val email = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        assertEquals(LayoutMode.LETTERS, KeyboardLayoutManager.initialModeFor(email))

        manager.resetForNewInput(InputType.TYPE_CLASS_PHONE)
        assertEquals(LayoutMode.SYMBOLS, manager.currentMode)
    }

    @Test
    fun `dead key accents the next vowel once`() {
        manager.setDeadKey(DeadKeys.ACUTE)
        assertEquals("é", manager.transformChar("e"))

        assertTrue("labels must drop the accent preview", manager.onCharacterWritten())
        assertEquals("e", manager.transformChar("e"))
    }

    @Test
    fun `pressing the same dead key twice cancels it`() {
        manager.setDeadKey(DeadKeys.ACUTE)
        manager.setDeadKey(DeadKeys.ACUTE)
        assertEquals(null, manager.pendingDeadKey)
    }

    @Test
    fun `dead key respects one-shot capital`() {
        tapShift()
        manager.setDeadKey(DeadKeys.ACUTE)
        assertEquals("Á", manager.transformChar("a"))
    }

    @Test
    fun `new input resets layer and case`() {
        manager.setMode(LayoutMode.SYMBOLS)
        tapShift()
        manager.resetForNewInput()
        assertEquals(LayoutMode.LETTERS, manager.currentMode)
        assertEquals(CaseMode.LOWERCASE, manager.currentCaseMode)
    }
}
