package com.onehandkeyboard.config

import com.onehandkeyboard.actions.ActionConfig
import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.GestureType
import com.onehandkeyboard.core.domain.model.KeyAction
import com.onehandkeyboard.enums.Language
import com.onehandkeyboard.enums.LayoutMode
import com.onehandkeyboard.input.DeadKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardConfigurationFactoryTest {
    private val factory = KeyboardConfigurationFactory()

    private fun allConfigurations() =
        Language.values().flatMap { language ->
            LayoutMode.values().map { mode -> factory.getConfiguration(language, mode) }
        }

    private fun KeyboardConfiguration.characters(): Set<Char> =
        cells.flatMap { it.actions.values }
            .filterIsInstance<ActionConfig.Character>()
            .map { it.char }
            .toSet()

    @Test
    fun `every layout builds without duplicated gestures`() {
        // keyboardGrid throws on duplicates, so building them is the assertion.
        assertEquals(4, allConfigurations().size)
    }

    @Test
    fun `symbols layout exposes previously missing characters`() {
        val symbols = factory.getConfiguration(Language.SPANISH, LayoutMode.SYMBOLS).characters()
        listOf('*', '<', '/', '\\', '~', '`').forEach { char ->
            assertTrue("Symbols layout should contain '$char'", char in symbols)
        }
    }

    @Test
    fun `spanish letters contain the whole alphabet`() {
        val letters = factory.getConfiguration(Language.SPANISH, LayoutMode.LETTERS).characters()
        ('a'..'z').plus('ñ').forEach { char ->
            assertTrue("Spanish layout should contain '$char'", char in letters)
        }
    }

    @Test
    fun `delete all is no longer on the case key and can be undone from backspace`() {
        allConfigurations().forEach { config ->
            val caseCell = config.cells.first { it.cellIndex == 7 }
            assertFalse(caseCell.actions.values.contains(ActionConfig.DeleteAll))

            val backspace = config.cells.first { it.cellIndex == 11 }
            assertEquals(ActionConfig.DeleteAll, backspace.getActionFor(GestureType.SWIPE, Direction.N))
            assertEquals(
                ActionConfig.Action(KeyAction.UndoDelete),
                backspace.getActionFor(GestureType.SWIPE, Direction.E),
            )
        }
    }

    @Test
    fun `english letters contain the whole alphabet`() {
        val letters = factory.getConfiguration(Language.ENGLISH, LayoutMode.LETTERS).characters()
        ('a'..'z').forEach { char ->
            assertTrue("English layout should contain '$char'", char in letters)
        }
    }

    @Test
    fun `spanish letters offer accents, inverted marks and email characters`() {
        val config = factory.getConfiguration(Language.SPANISH, LayoutMode.LETTERS)
        val chars = config.characters()
        listOf('¿', '¡', '!', '?', '@', '/', '-').forEach { char ->
            assertTrue("Spanish layout should contain '$char'", char in chars)
        }

        val deadKeys =
            config.cells.flatMap { it.actions.values }
                .filterIsInstance<ActionConfig.Action>()
                .mapNotNull { (it.keyAction as? KeyAction.DeadKey)?.accent }
                .toSet()
        assertEquals(setOf(DeadKeys.ACUTE, DeadKeys.DIAERESIS), deadKeys)
        "aeiou".forEach { vowel -> assertTrue(vowel in chars) }
    }

    @Test
    fun `backspace swipes left to delete a word`() {
        allConfigurations().forEach { config ->
            val backspace = config.cells.first { it.cellIndex == 11 }
            assertEquals(
                ActionConfig.Action(KeyAction.DeleteWord),
                backspace.getActionFor(GestureType.SWIPE, Direction.W),
            )
        }
    }

    @Test
    fun `configurations are cached per language and mode`() {
        val first = factory.getConfiguration(Language.SPANISH, LayoutMode.LETTERS)
        assertTrue(first === factory.getConfiguration(Language.SPANISH, LayoutMode.LETTERS))
        assertFalse(first === factory.getConfiguration(Language.ENGLISH, LayoutMode.LETTERS))
    }

    @Test
    fun `layout key switches layer and only swipes down to the memes`() {
        allConfigurations().forEach { config ->
            val layoutKey = config.cells.first { it.cellIndex == 3 }
            assertEquals(setOf(Direction.S), layoutKey.getDirectionsFor(GestureType.SWIPE).toSet())
            assertEquals(
                ActionConfig.Action(KeyAction.ShowMemes),
                layoutKey.getActionFor(GestureType.SWIPE, Direction.S),
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `grid dsl rejects a direction declared twice`() {
        keyboardGrid {
            cell(8) {
                tap('7')
                swipe(Direction.W, '*')
                swipe(Direction.W, '<')
            }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `grid dsl rejects a cell declared twice`() {
        keyboardGrid {
            cell(0) { tap('a') }
            cell(0) { tap('b') }
        }
    }
}
