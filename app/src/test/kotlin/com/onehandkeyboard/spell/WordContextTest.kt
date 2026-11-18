package com.onehandkeyboard.spell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordContextTest {
    @Test
    fun `the word before the cursor is found`() {
        assertEquals(WordContext("cancion", true), WordContext.of("hola cancion", ""))
        assertEquals(WordContext("don't", true), WordContext.of("I don't", null))
        assertEquals("", WordContext.of("hola ", "")?.word)
    }

    @Test
    fun `words glued to digits or symbols are not autocorrected`() {
        assertFalse(WordContext.of("user@gmial", "")!!.mayAutoCorrect)
        assertFalse(WordContext.of("#hastag", "")!!.mayAutoCorrect)
        assertFalse(WordContext.of("3er", "")!!.mayAutoCorrect)
        assertTrue(WordContext.of("¿cancion", "")!!.mayAutoCorrect)
    }

    @Test
    fun `capitalized words are names unless they start a sentence`() {
        assertFalse(WordContext.of("vi a Gonzalo", "")!!.mayAutoCorrect)
        assertTrue(WordContext.of("Cancion", "")!!.mayAutoCorrect)
        assertTrue(WordContext.of("hola. Cancion", "")!!.mayAutoCorrect)
        assertTrue(WordContext.of("hola ¿Cancion", "")!!.mayAutoCorrect)
        assertTrue("all caps is emphasis, not a name", WordContext.of("vi la CANCION", "")!!.mayAutoCorrect)
    }

    @Test
    fun `the cursor inside a word means no correction`() {
        assertNull(WordContext.of("canc", "ion"))
    }
}
