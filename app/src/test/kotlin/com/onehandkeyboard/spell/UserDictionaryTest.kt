package com.onehandkeyboard.spell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UserDictionaryTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `undone corrections are learned at once and survive a restart`() {
        val file = folder.newFile("words.tsv")
        UserDictionary(file).learn("Cancion")

        val reloaded = UserDictionary(file)
        assertTrue(reloaded.isLearned("cancion"))
        assertEquals(listOf("cancion"), reloaded.learnedWords())
    }

    @Test
    fun `unknown words are learned on their second use`() {
        val words = UserDictionary(null)
        assertFalse(words.recordUnknown("zorglub"))
        assertFalse(words.isLearned("zorglub"))
        assertTrue(words.recordUnknown("Zorglub"))
        assertTrue(words.isLearned("zorglub"))
        assertFalse("already learned", words.recordUnknown("zorglub"))
    }

    @Test
    fun `clear forgets everything on disk too`() {
        val file = folder.newFile("words.tsv")
        UserDictionary(file).apply {
            learn("uno")
            clear()
        }
        assertTrue(UserDictionary(file).learnedWords().isEmpty())
    }

    @Test
    fun `pending words are evicted before learned ones when full`() {
        val words = UserDictionary(null)
        words.learn("keep")
        repeat(UserDictionary.MAX_WORDS + 10) { words.recordUnknown("w$it") }
        assertTrue(words.isLearned("keep"))
    }
}
