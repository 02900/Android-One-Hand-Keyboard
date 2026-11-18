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
