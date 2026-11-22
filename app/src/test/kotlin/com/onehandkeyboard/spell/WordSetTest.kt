package com.onehandkeyboard.spell

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import kotlin.random.Random

/** Reads the bundled set, built in Python by tools/build_word_set.py (same hashes). */
class WordSetTest {
    private val words by lazy { File("src/main/assets/dict/es_words.bin").inputStream().use(WordSet::read) }

    @Test
    fun `every form of a word is in it, with or without accents`() {
        for (word in listOf("grafito", "eliminábamos", "Eliminabamos", "tendríamos", "pidió", "año", "pingüino")) {
            assertTrue(word, word in words)
        }
    }

    @Test
    fun `few non-words get through`() {
        val random = Random(3)
        val junk = List(10_000) { String(CharArray(8) { 'a' + random.nextInt(26) }) }
        val through = junk.count { it in words }
        assertTrue("$through of 10000", through < 300) // built for ~1 %
        assertFalse("" in words)
    }

    @Test(expected = IOException::class)
    fun `another file is refused`() {
        WordSet.read(ByteArrayInputStream("hola\t1\n".toByteArray()))
    }
}
