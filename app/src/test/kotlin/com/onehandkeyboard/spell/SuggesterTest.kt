package com.onehandkeyboard.spell

import com.onehandkeyboard.config.KeyboardConfigurationFactory
import com.onehandkeyboard.enums.Language
import com.onehandkeyboard.enums.LayoutMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Uses the real bundled dictionaries and keyboard layouts. */
class SuggesterTest {
    private fun suggester(language: Language): Suggester {
        val asset = if (language == Language.SPANISH) "es" else "en"
        val dictionary = File("src/main/assets/dict/$asset.tsv").useLines { WordDictionary.parse(it) }
        val config = KeyboardConfigurationFactory().getConfiguration(language, LayoutMode.LETTERS)
        return Suggester(dictionary, UserDictionary(null), ErrorModel.fromLayout(config))
    }

    private val spanish by lazy { suggester(Language.SPANISH) }
    private val english by lazy { suggester(Language.ENGLISH) }

    @Test
    fun `missing accents are fixed`() {
        assertEquals("canción", spanish.suggest("cancion").autoCorrect)
        assertEquals("también", spanish.suggest("tambien").autoCorrect)
        assertEquals("tenés", spanish.suggest("tenes").autoCorrect)
    }

    @Test
    fun `corrections keep the typed capitalization`() {
        assertEquals("Canción", spanish.suggest("Cancion").autoCorrect)
        assertEquals("CANCIÓN", spanish.suggest("CANCION").autoCorrect)
    }

    @Test
    fun `valid words are never replaced but their accented twin is offered`() {
        val result = spanish.suggest("esta")
        assertNull(result.autoCorrect)
        assertTrue(result.slots.any { it?.text == "está" })
        assertEquals(Suggestion("esta", Suggestion.Kind.TYPED), result.slots[1])
        assertNull(spanish.suggest("como").autoCorrect)
        assertNull(spanish.suggest("si").autoCorrect)
    }

    @Test
    fun `voseo and common words are known`() {
        listOf("tenés", "querés", "vos", "sos", "hola", "acá").forEach { word ->
            assertTrue("$word should be known", spanish.isKnown(word))
        }
    }

    @Test
    fun `typos are fixed and a wrong swipe in the same cell is cheap`() {
        assertEquals("hola", spanish.suggest("hloa").autoCorrect)
        assertEquals("gracias", spanish.suggest("graicas").autoCorrect)
        // p and b share a cell: "pueno" is one wrong-direction swipe away from "bueno".
        assertEquals("bueno", spanish.suggest("pueno").autoCorrect)
        assertEquals("the", english.suggest("teh").autoCorrect)
    }

    @Test
    fun `a missed space bar that typed the letter above it is split into two words`() {
        // The cells above the space bar are t, e, s (Spanish) and z, c (English).
        assertEquals("en méxico", spanish.suggest("enemexico").autoCorrect)
        assertEquals("les caen", spanish.suggest("lesscaen").autoCorrect)
        assertEquals("hola como", spanish.suggest("holaecomo").autoCorrect)
        assertEquals("En méxico", spanish.suggest("Enemexico").autoCorrect)
        assertEquals("the best", english.suggest("thecbest").autoCorrect)
    }

    @Test
    fun `a missed space never beats a likely one-word typo`() {
        // Each of these has a split with an s/t/c in it ("est do", "con go", "than ks").
        assertEquals("estado", spanish.suggest("estsdo").autoCorrect)
        assertEquals("contigo", spanish.suggest("contgo").autoCorrect)
        assertEquals("thanks", english.suggest("thanzks").autoCorrect)
        assertNull("short words are not split", spanish.suggest("ate").autoCorrect)
    }

    @Test
    fun `english contractions get their apostrophe`() {
        assertEquals("don't", english.suggest("dont").autoCorrect)
        assertEquals("I'm", english.suggest("Im").autoCorrect)
        assertNull("its is a word", english.suggest("its").autoCorrect)
    }

    @Test
    fun `unknown words without a close candidate and very short words are left alone`() {
        assertNull(spanish.suggest("zxqwj").autoCorrect)
        assertNull(spanish.suggest("tb").autoCorrect)
        assertNull(spanish.suggest("q").autoCorrect)
    }

    @Test
    fun `learned words are known and suggested as completions`() {
        val user = UserDictionary(null)
        val dictionary = WordDictionary.of(listOf("hola" to 100L))
        val suggester = Suggester(dictionary, user, ErrorModel.UNIFORM)
        assertFalse(suggester.isKnown("Gonzalito"))
        user.learn("Gonzalito")
        assertTrue(suggester.isKnown("gonzalito"))
        assertTrue(suggester.suggest("gonz").slots.any { it?.text == "gonzalito" })
    }

    @Test
    fun `a search over the whole dictionary is fast`() {
        spanish.suggest("warmup")
        val start = System.nanoTime()
        repeat(REPEAT) { spanish.suggest("electrodomestico") }
        val perCallMs = (System.nanoTime() - start) / 1_000_000.0 / REPEAT
        assertTrue("suggest took $perCallMs ms", perCallMs < MAX_MS)
    }

    private companion object {
        const val REPEAT = 20
        const val MAX_MS = 50.0
    }
}
