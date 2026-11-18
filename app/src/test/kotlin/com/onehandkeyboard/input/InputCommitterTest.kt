package com.onehandkeyboard.input

import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class InputCommitterTest {
    private val committer = InputCommitter()
    private val ic = mockk<InputConnection>(relaxed = true)

    private fun editorInfo(
        imeOptions: Int,
        inputType: Int = InputType.TYPE_CLASS_TEXT,
    ) = EditorInfo().apply {
        this.imeOptions = imeOptions
        this.inputType = inputType
    }

    @Test
    fun `enter in a single line search field runs the search action`() {
        committer.enter(ic, editorInfo(EditorInfo.IME_ACTION_SEARCH))

        verify { ic.performEditorAction(EditorInfo.IME_ACTION_SEARCH) }
        verify(exactly = 0) { ic.commitText("\n", any()) }
    }

    @Test
    fun `enter in a multi line field inserts a newline`() {
        val info =
            editorInfo(
                EditorInfo.IME_ACTION_SEND,
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
            )

        committer.enter(ic, info)

        verify { ic.commitText("\n", 1) }
        verify(exactly = 0) { ic.performEditorAction(any()) }
    }

    @Test
    fun `enter honours IME_FLAG_NO_ENTER_ACTION`() {
        committer.enter(ic, editorInfo(EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_ENTER_ACTION))

        verify { ic.commitText("\n", 1) }
    }

    @Test
    fun `enter without editor action sends an ENTER key event`() {
        val events = mutableListOf<KeyEvent>()
        every { ic.sendKeyEvent(capture(events)) } returns true

        committer.enter(ic, editorInfo(EditorInfo.IME_ACTION_UNSPECIFIED))

        assertEquals(listOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP), events.map { it.action })
        assertTrue(events.all { it.keyCode == KeyEvent.KEYCODE_ENTER })
    }

    @Test
    fun `submit runs the field action even in multi line fields`() {
        val info =
            editorInfo(
                EditorInfo.IME_ACTION_SEND or EditorInfo.IME_FLAG_NO_ENTER_ACTION,
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE,
            )

        committer.submit(ic, info)

        verify { ic.performEditorAction(EditorInfo.IME_ACTION_SEND) }
    }

    @Test
    fun `backspace without selection sends DEL instead of deleting one UTF-16 unit`() {
        every { ic.getSelectedText(0) } returns null
        val event = slot<KeyEvent>()
        every { ic.sendKeyEvent(capture(event)) } returns true

        committer.backspace(ic)

        assertEquals(KeyEvent.KEYCODE_DEL, event.captured.keyCode)
        verify(exactly = 0) { ic.deleteSurroundingText(any(), any()) }
    }

    @Test
    fun `backspace with selection removes the selection`() {
        every { ic.getSelectedText(0) } returns "hola"

        committer.backspace(ic)

        verify { ic.commitText("", 1) }
        verify(exactly = 0) { ic.sendKeyEvent(any()) }
    }

    @Test
    fun `quick double space after a word becomes period and space`() {
        every { ic.getTextBeforeCursor(2, 0) } returns "a "

        committer.space(ic, now = 1_000)
        committer.space(ic, now = 1_300)

        verify(exactly = 1) { ic.commitText(" ", 1) }
        verify { ic.deleteSurroundingText(1, 0) }
        verify { ic.commitText(". ", 1) }
    }

    @Test
    fun `slow double space stays two spaces`() {
        every { ic.getTextBeforeCursor(2, 0) } returns "a "

        committer.space(ic, now = 1_000)
        committer.space(ic, now = 1_000 + InputCommitter.DOUBLE_SPACE_MS + 1)

        verify(exactly = 2) { ic.commitText(" ", 1) }
        verify(exactly = 0) { ic.commitText(". ", 1) }
    }

    @Test
    fun `double space is not converted after punctuation or when disabled`() {
        every { ic.getTextBeforeCursor(2, 0) } returns ". "
        committer.space(ic, now = 1_000)
        committer.space(ic, now = 1_100)

        every { ic.getTextBeforeCursor(2, 0) } returns "a "
        committer.space(ic, now = 5_000, allowPeriodShortcut = false)
        committer.space(ic, now = 5_100, allowPeriodShortcut = false)

        verify(exactly = 0) { ic.commitText(". ", 1) }
    }

    @Test
    fun `word deletion length covers word plus trailing spaces or a punctuation run`() {
        assertEquals(6, InputCommitter.wordDeletionLength("hola mundo "))
        assertEquals(5, InputCommitter.wordDeletionLength("hola mundo"))
        assertEquals(3, InputCommitter.wordDeletionLength("hola..."))
        assertEquals(2, InputCommitter.wordDeletionLength("ok 😀")) // emoji = 2 UTF-16 units, kept whole
        assertEquals(0, InputCommitter.wordDeletionLength(""))
    }

    @Test
    fun `delete word removes the previous word and can be undone`() {
        every { ic.getSelectedText(0) } returns null
        every { ic.getTextBeforeCursor(any(), 0) } returns "hola mundo "

        committer.deleteWord(ic)
        verify { ic.deleteSurroundingText(6, 0) }

        assertTrue(committer.undoDelete(ic))
        verify { ic.commitText("mundo ", 1) }
    }

    @Test
    fun `cursor moves with dpad key events`() {
        val events = mutableListOf<KeyEvent>()
        every { ic.sendKeyEvent(capture(events)) } returns true

        committer.moveCursor(ic, -2)

        assertEquals(4, events.size) // 2 steps × (down + up)
        assertTrue(events.all { it.keyCode == KeyEvent.KEYCODE_DPAD_LEFT })
    }

    @Test
    fun `password and email fields are detected`() {
        val password = editorInfo(0, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        val email = editorInfo(0, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        val prose = editorInfo(0, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)

        assertTrue(InputCommitter.isPassword(password))
        assertFalse(InputCommitter.isPassword(email))
        assertFalse(InputCommitter.allowsPeriodShortcut(password))
        assertFalse(InputCommitter.allowsPeriodShortcut(email))
        assertTrue(InputCommitter.allowsPeriodShortcut(prose))
    }

    @Test
    fun `delete all can be undone`() {
        every { ic.getExtractedText(any(), 0) } returns
            android.view.inputmethod.ExtractedText().apply { text = "mensaje largo" }

        committer.deleteAll(ic)
        verify { ic.setSelection(0, "mensaje largo".length) }
        verify { ic.commitText("", 1) }

        assertTrue(committer.undoDelete(ic))
        verify { ic.commitText("mensaje largo", 1) }
        assertFalse("Undo only restores once", committer.undoDelete(ic))
    }
}
