package com.onehandkeyboard.spell

import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.Executor

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AutoCorrectorTest {
    private val text = StringBuilder()
    private val ic =
        mockk<InputConnection>(relaxed = true).also { ic ->
            every { ic.getTextBeforeCursor(any(), any()) } answers { text.takeLast(firstArg<Int>()).toString() }
            every { ic.getTextAfterCursor(any(), any()) } returns ""
            every { ic.getSelectedText(any()) } returns null
            every { ic.deleteSurroundingText(any(), any()) } answers {
                text.setLength(text.length - firstArg<Int>())
                true
            }
            every { ic.commitText(any(), any()) } answers {
                text.append(firstArg<CharSequence>())
                true
            }
        }

    private val user = UserDictionary(null)
    private val dictionary =
        WordDictionary.of(
            listOf(
                "canción" to 40_000L,
                "también" to 400_000L,
                "esta" to 1_000_000L,
                "está" to 2_000_000L,
                "hola" to 300_000L,
            ),
        )
    private val direct = Executor { it.run() }
    private val published = mutableListOf<Suggestions>()
    private val corrector =
        AutoCorrector({ Suggester(dictionary, user, ErrorModel.UNIFORM) }, { user }, direct, direct).apply {
            onSuggestions = { published += it }
            startInput(FieldPolicy(suggestions = true, autoCorrect = true, learn = true))
        }

    /** Types [word] and then a space, the way the action processor does. */
    private fun typeWordAndSpace(word: String) {
        text.append(word)
        corrector.beforeSeparator(ic, " ")
        text.append(" ")
    }

    @Test
    fun `an unknown word is corrected when the word is closed`() {
        typeWordAndSpace("cancion")
        assertEquals("canción ", text.toString())
    }

    @Test
    fun `backspace right after a correction restores the original once and learns it`() {
        typeWordAndSpace("cancion")

        assertTrue(corrector.onBackspace(ic))
        assertEquals("cancion ", text.toString())
        assertTrue(user.isLearned("cancion"))
        assertFalse("only the first backspace reverts", corrector.onBackspace(ic))

        typeWordAndSpace("cancion")
        assertEquals("not corrected again", "cancion cancion ", text.toString())
    }

    @Test
    fun `any other key in between keeps the correction`() {
        typeWordAndSpace("cancion")
        corrector.invalidate()
        assertFalse(corrector.onBackspace(ic))
        assertEquals("canción ", text.toString())
    }

    @Test
    fun `punctuation also closes a word`() {
        text.append("tambien")
        corrector.beforeSeparator(ic, ",")
        text.append(",")
        assertEquals("también,", text.toString())
    }

    @Test
    fun `valid words are never touched`() {
        typeWordAndSpace("esta")
        assertEquals("esta ", text.toString())
    }

    @Test
    fun `capitalized words mid-sentence look like names and are left alone`() {
        text.append("hola ")
        typeWordAndSpace("Cancion")
        assertEquals("hola Cancion ", text.toString())

        text.append(". ")
        typeWordAndSpace("Cancion")
        assertEquals("hola Cancion . Canción ", text.toString())
    }

    @Test
    fun `nothing happens in fields without suggestions`() {
        corrector.startInput(FieldPolicy.NONE)
        typeWordAndSpace("cancion")
        assertEquals("cancion ", text.toString())
        assertFalse(user.isLearned("cancion"))
    }

    @Test
    fun `incognito fields do not learn but still respect the undo`() {
        corrector.startInput(FieldPolicy(suggestions = true, autoCorrect = true, learn = false))
        typeWordAndSpace("cancion")
        assertTrue(corrector.onBackspace(ic))
        assertFalse(user.isLearned("cancion"))
        typeWordAndSpace("cancion")
        assertEquals("cancion cancion ", text.toString())
    }

    @Test
    fun `an unknown word typed twice without a correction is learned`() {
        typeWordAndSpace("zorglub")
        assertFalse(user.isLearned("zorglub"))
        typeWordAndSpace("zorglub")
        assertTrue(user.isLearned("zorglub"))
    }

    @Test
    fun `picking suggestions replaces, keeps or reverts the word`() {
        text.append("hol")
        corrector.pick(ic, Suggestion("hola", Suggestion.Kind.ALTERNATIVE))
        assertEquals("hola ", text.toString())

        text.append("gonzalito")
        corrector.pick(ic, Suggestion("gonzalito", Suggestion.Kind.TYPED))
        assertEquals("hola gonzalito ", text.toString())
        assertTrue(user.isLearned("gonzalito"))

        typeWordAndSpace("cancion")
        corrector.pick(ic, Suggestion("cancion", Suggestion.Kind.REVERT))
        assertEquals("hola gonzalito cancion ", text.toString())
    }

    @Test
    fun `the strip shows the word being typed and the undo after a correction`() {
        text.append("cancion")
        corrector.refresh(ic)
        assertEquals("canción", published.last().autoCorrect)

        corrector.beforeSeparator(ic, " ")
        text.append(" ")
        corrector.refresh(ic)
        assertEquals(Suggestions.revert("cancion"), published.last())
    }

    @Test
    fun `field policy follows the input type`() {
        fun policy(
            type: Int,
            options: Int = 0,
        ) = FieldPolicy.from(
            EditorInfo().apply {
                inputType = type
                imeOptions = options
            },
            suggestionsEnabled = true,
            autoCorrectEnabled = true,
        )
        val text = InputType.TYPE_CLASS_TEXT
        assertEquals(FieldPolicy(true, true, true), policy(text or InputType.TYPE_TEXT_FLAG_MULTI_LINE))
        assertEquals(FieldPolicy.NONE, policy(text or InputType.TYPE_TEXT_VARIATION_PASSWORD))
        assertEquals(FieldPolicy.NONE, policy(text or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS))
        assertEquals(FieldPolicy.NONE, policy(text or InputType.TYPE_TEXT_VARIATION_URI))
        assertEquals(FieldPolicy.NONE, policy(text or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS))
        assertEquals(FieldPolicy.NONE, policy(InputType.TYPE_CLASS_NUMBER))
        assertFalse(policy(text or InputType.TYPE_TEXT_VARIATION_PERSON_NAME).autoCorrect)
        assertFalse(policy(text, EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING).learn)
    }
}
