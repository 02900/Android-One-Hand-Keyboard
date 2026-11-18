package com.onehandkeyboard.spell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordDictionaryTest {
    private val dictionary =
        WordDictionary.parse(
            sequenceOf(
                "está\t200", "esta\t100", "estado\t50", "casa\t80", "cosa\t70",
                "caso\t60", "año\t90", "ano\t5", "don't\t30", "bad line",
            ),
        )

    @Test
    fun `folding removes case, accents and apostrophes but keeps ñ`() {
        assertEquals("cancion", WordDictionary.fold("Canción"))
        assertEquals("dont", WordDictionary.fold("don't"))
        assertEquals("año", WordDictionary.fold("AÑO"))
        assertEquals("pinguino", WordDictionary.fold("pingüino"))
    }

    @Test
    fun `exact lookup respects accents`() {
        assertTrue(dictionary.isWord("esta"))
        assertTrue(dictionary.isWord("Está"))
        assertFalse(dictionary.isWord("estä"))
        assertEquals(listOf("está", "esta"), dictionary.variants("ESTA").map { it.word })
        assertEquals(listOf("año"), dictionary.variants("año").map { it.word })
        assertEquals(listOf("ano"), dictionary.variants("ano").map { it.word })
    }

    @Test
    fun `fuzzy search finds words within the cost and skips accent-only matches`() {
        val found = dictionary.search("cssa", 1f, ErrorModel.UNIFORM).map { it.word }.toSet()
        assertEquals(setOf("casa", "cosa"), found)
        val transposed = dictionary.search("csaa", 1f, ErrorModel.UNIFORM).map { it.word }
        assertEquals(listOf("casa"), transposed)
        assertTrue(dictionary.search("esta", 0f, ErrorModel.UNIFORM).isEmpty())
    }

    @Test
    fun `same cell substitutions cost half`() {
        val model = ErrorModel.fromCells(mapOf(0 to listOf('s', 'z'), 1 to listOf('a')))
        val costs = dictionary.search("cesa", 1f, model).associate { it.word to it.cost }
        assertEquals(1f, costs["casa"])
        val cheap = dictionary.search("caza", 1f, model).associate { it.word to it.cost }
        assertEquals(ErrorModel.SAME_CELL_COST, cheap["casa"])
    }

    @Test
    fun `completions are the most frequent longer words with the prefix`() {
        assertEquals(listOf("estado"), dictionary.completions("est", 3).map { it.word }.filter { it.length > 4 })
        assertEquals(listOf("casa", "cosa", "caso").sorted(), dictionary.completions("c", 3).map { it.word }.sorted())
    }
}
