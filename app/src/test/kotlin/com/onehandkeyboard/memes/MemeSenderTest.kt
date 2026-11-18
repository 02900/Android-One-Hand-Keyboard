package com.onehandkeyboard.memes

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MemeSenderTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val sender = MemeSender(context) { Uri.parse("content://memes.share/${it.name}") }

    /** A meme in the picked folder, as the documents provider would serve it. */
    private fun memeFile(name: String = "risa.gif"): Meme {
        val uri = Uri.parse("content://memes.test/document/$name")
        shadowOf(context.contentResolver).registerInputStreamSupplier(uri) {
            ByteArrayInputStream(byteArrayOf(1, 2, 3))
        }
        return Meme(uri.toString(), name, "image/gif")
    }

    private fun field(vararg types: String) =
        EditorInfo().also { EditorInfoCompat.setContentMimeTypes(it, arrayOf(*types)) }

    @Test
    fun `field accepts the meme when its types match`() {
        assertTrue(MemeSender.accepts(arrayOf("image/*"), "image/gif"))
        assertTrue(MemeSender.accepts(arrayOf("image/png", "image/gif"), "image/gif"))
        assertFalse(MemeSender.accepts(arrayOf("image/png"), "image/gif"))
        assertFalse(MemeSender.accepts(emptyArray(), "image/gif"))
    }

    @Test
    fun `prepare copies the meme to the shareable cache`() {
        val prepared = sender.prepare(memeFile())
        assertNotNull(prepared)
        assertEquals("risa.gif", prepared!!.file.name)
        assertEquals("image/gif", prepared.mimeType)
        assertEquals(listOf<Byte>(1, 2, 3), prepared.file.readBytes().toList())
    }

    @Test
    fun `prepare fails softly when the meme is gone`() {
        val uri = Uri.parse("content://memes.test/document/nada.gif")
        shadowOf(context.contentResolver).registerInputStreamSupplier(uri) { throw FileNotFoundException() }
        assertNull(sender.prepare(Meme(uri.toString(), "nada.gif", "image/gif")))
    }

    @Test
    fun `a field that accepts gifs gets the meme as content`() {
        val meme = memeFile()
        val ic = mockk<InputConnection>(relaxed = true)
        every { ic.commitContent(any(), any(), any()) } returns true
        val outcome = sender.commit(meme, sender.prepare(meme)!!, ic, field("image/gif"))
        assertEquals(MemeSender.Outcome.SENT, outcome)
        verify { ic.commitContent(any(), any(), any()) }
    }

    @Test
    fun `a text-only field gets the meme in the clipboard`() {
        val meme = memeFile()
        val ic = mockk<InputConnection>(relaxed = true)
        val outcome = sender.commit(meme, sender.prepare(meme)!!, ic, field())
        assertEquals(MemeSender.Outcome.COPIED, outcome)
        verify(exactly = 0) { ic.commitContent(any(), any(), any()) }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertEquals("image/gif", clipboard.primaryClip!!.description.getMimeType(0))
    }

    @Test
    fun `file names are made safe for the cache`() {
        assertEquals("a_b_c.gif", MemeSender.safeFileName("a/b:c.gif"))
        assertEquals("meme", MemeSender.safeFileName("  "))
    }
}
