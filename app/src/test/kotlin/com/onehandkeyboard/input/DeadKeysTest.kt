package com.onehandkeyboard.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeadKeysTest {
    @Test
    fun `acute accent composes every spanish vowel`() {
        assertEquals(
            listOf("á", "é", "í", "ó", "ú"),
            listOf("a", "e", "i", "o", "u").map { DeadKeys.compose(DeadKeys.ACUTE, it) },
        )
    }

    @Test
    fun `composition keeps the case of the letter`() {
        assertEquals("É", DeadKeys.compose(DeadKeys.ACUTE, "E"))
        assertEquals("Ü", DeadKeys.compose(DeadKeys.DIAERESIS, "U"))
    }

    @Test
    fun `diaeresis composes u for spanish words like pingüino`() {
        assertEquals("ü", DeadKeys.compose(DeadKeys.DIAERESIS, "u"))
    }

    @Test
    fun `non composable input keeps the accent before it`() {
        assertEquals("´x", DeadKeys.compose(DeadKeys.ACUTE, "x"))
        assertFalse(DeadKeys.canCompose(DeadKeys.ACUTE, 'x'))
        assertTrue(DeadKeys.canCompose(DeadKeys.ACUTE, 'O'))
    }
}
