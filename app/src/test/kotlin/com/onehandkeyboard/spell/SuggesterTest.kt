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
        val allWords =
            File(
                "src/main/assets/dict/${asset}_words.bin",
            ).takeIf { it.exists() }?.inputStream()?.use(WordSet::read)
        return Suggester(dictionary, UserDictionary(null), ErrorModel.fromLayout(config), allWords)
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
    fun `a space that was not typed at all is restored`() {
        assertEquals("no puede", spanish.suggest("nopuede").autoCorrect)
        assertEquals("méxico como", spanish.suggest("mexicocomo").autoCorrect)
        assertEquals("bien bueno", spanish.suggest("bienbueno").autoCorrect)
        assertEquals("don't know", english.suggest("dontknow").autoCorrect)
    }

    @Test
    fun `regular forms of listed verbs are known, not split or fixed`() {
        for (word in listOf("eliminaba", "eliminábamos", "eliminarán", "eliminándolo", "contestaste", "volvía")) {
            assertTrue(word, spanish.isKnown(word))
            assertNull(word, spanish.suggest(word).autoCorrect)
        }
        assertEquals("eliminaba", spanish.suggest("eliminsba").autoCorrect) // and typos in them are fixed
        assertFalse("not a verb form", spanish.isKnown("estó"))
    }

    @Test
    fun `rare real words are not forced into a common one, typos of common words still are`() {
        // Not in the 90k dictionary, but real: they used to become chicago, posadero, "levi amos".
        for (word in listOf("chilango", "botadero", "levitamos", "electronegativa")) {
            assertNull(word, spanish.suggest(word).autoCorrect)
        }
        assertEquals("bueno", spanish.suggest("pueno").autoCorrect)
        assertEquals("canción", spanish.suggest("cancion").autoCorrect) // accents still added
        assertEquals("estado", spanish.suggest("estsdo").autoCorrect)
    }

    @Test
    fun `rare 2-letter tokens are not split off the end of a word`() {
        // The subtitle list has junk like "ba" (as in "elimina ba"); real 2-letter words still split.
        val dictionary =
            WordDictionary.of(listOf("elimina" to 1_345L, "ba" to 1_949L, "no" to 12_000_000L, "puede" to 500_000L))
        val suggester = Suggester(dictionary, UserDictionary(null), ErrorModel.UNIFORM)
        val suggestions = suggester.suggest("eliminaba")
        assertFalse(suggestions.toString(), suggestions.autoCorrect == "elimina ba")
        assertFalse(suggestions.toString(), suggestions.slots.any { it?.text == "elimina ba" })
        assertEquals("no puede", suggester.suggest("nopuede").autoCorrect)
    }

    @Test
    fun `a close call between a split and a one-word fix is only offered`() {
        val suggestions = spanish.suggest("aydios")
        assertNull("not forced to adiós", suggestions.autoCorrect)
        val offered = suggestions.slots.mapNotNull { it?.text }
        assertTrue(offered.toString(), "ay dios" in offered)
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
