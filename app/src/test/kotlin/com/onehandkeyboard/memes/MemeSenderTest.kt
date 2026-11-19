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
    fun `a trending meme is downloaded and named after its real type`() {
        val downloads = mutableListOf<String>()
        val online =
            MemeSender(context, download = { url, file ->
                downloads += url
                file.parentFile?.mkdirs()
                file.writeBytes("GIF89a....".toByteArray())
                true
            }) { Uri.parse("content://memes.share/${it.name}") }
        val meme =
            Meme("https://i.kym-cdn.com/photos/images/original/9/9/9", "Gato bailando.jpg", "image/jpeg", "file:/p")

        val prepared = online.prepare(meme)!!
        val again = online.prepare(meme)!!

        assertEquals("Gato bailando.gif", prepared.file.name)
        assertEquals("image/gif", prepared.mimeType)
        assertEquals(prepared.file, again.file)
        assertEquals(listOf(meme.uri), downloads) // the second time it comes from the cache
    }

    @Test
    fun `a trending meme that is not an image, or can't be downloaded, is not sent`() {
        val meme = Meme("https://i.kym-cdn.com/photos/images/original/1/2/3", "x.jpg", "image/jpeg")
        val html =
            MemeSender(context, download = { _, file ->
                file.parentFile?.mkdirs()
                file.writeText("<html>")
                true
            }) { Uri.parse("content://memes.share/${it.name}") }
        assertNull(html.prepare(meme))
        assertNull(MemeSender(context, download = { _, _ -> false }).prepare(meme.copy(uri = meme.uri + "4")))
        assertNull(sender.prepare(meme.copy(uri = meme.uri + "5"))) // build without Internet
    }

    @Test
    fun `file types are recognised from their first bytes`() {
        val file = java.io.File.createTempFile("sniff", ".bin")

        fun type(vararg bytes: Int): String? {
            file.writeBytes(ByteArray(bytes.size) { bytes[it].toByte() })
            return MemeFormat.sniffMimeType(file)
        }
        assertEquals("image/gif", type(0x47, 0x49, 0x46, 0x38))
        assertEquals("image/png", type(0x89, 0x50, 0x4E, 0x47, 0x0D))
        assertEquals("image/jpeg", type(0xFF, 0xD8, 0xFF, 0xE0))
        assertEquals("image/webp", type(0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50, 0x56))
        assertNull(type(0x3C, 0x68, 0x74, 0x6D, 0x6C))
        file.delete()
    }

    @Test
    fun `file names are made safe for the cache`() {
        assertEquals("a_b_c.gif", MemeSender.safeFileName("a/b:c.gif"))
        assertEquals("meme", MemeSender.safeFileName("  "))
    }
}
