package com.onehandkeyboard.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceTextTest {
    @Test
    fun `dictation gets a space only when it would stick to the previous word`() {
        assertEquals("", VoiceText.prefix(null))
        assertEquals("", VoiceText.prefix(""))
        assertEquals("", VoiceText.prefix("hola "))
        assertEquals("", VoiceText.prefix("¿"))
        assertEquals(" ", VoiceText.prefix("hola"))
        assertEquals(" ", VoiceText.prefix("hola,"))
    }

    @Test
    fun `a sentence starts at the beginning or after a sentence end`() {
        assertTrue(VoiceText.startsSentence(null))
        assertTrue(VoiceText.startsSentence(""))
        assertTrue(VoiceText.startsSentence("Listo. "))
        assertTrue(VoiceText.startsSentence("¿Vienes? "))
        assertFalse(VoiceText.startsSentence("hola "))
        assertFalse(VoiceText.startsSentence("hola, "))
    }

    @Test
    fun `capitals are added at a sentence start and never removed`() {
        assertEquals("Nos vemos mañana", VoiceText.format("nos vemos mañana", capitalize = true))
        assertEquals("nos vemos mañana", VoiceText.format("nos vemos mañana", capitalize = false))
        assertEquals("María viene", VoiceText.format("María viene", capitalize = false))
    }
}
