package com.onehandkeyboard

import com.onehandkeyboard.config.KeyboardConfigurationFactory
import com.onehandkeyboard.core.domain.model.CaseMode
import com.onehandkeyboard.enums.LayoutMode
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
        assertEquals("A", manager.applyCase("a"))

        assertTrue("label refresh needed", manager.onCharacterWritten())
        assertEquals(CaseMode.LOWERCASE, manager.currentCaseMode)
        assertEquals("b", manager.applyCase("b"))
    }

    @Test
    fun `double shift tap locks caps until tapped again`() {
        tapShift()
        tapShift(afterMs = 150)
        assertEquals(CaseMode.UPPERCASE, manager.currentCaseMode)

        assertFalse(manager.onCharacterWritten())
        assertEquals("C", manager.applyCase("c"))

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
        assertEquals("H", manager.applyCase("h"))

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
    fun `new input resets layer and case`() {
        manager.switchToSymbols()
        tapShift()
        manager.resetForNewInput()
        assertEquals(LayoutMode.LETTERS, manager.currentMode)
        assertEquals(CaseMode.LOWERCASE, manager.currentCaseMode)
    }
}
