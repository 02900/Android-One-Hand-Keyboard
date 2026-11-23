package com.onehandkeyboard.memes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MemeFormatTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    // What each app declared on a real phone (EditorInfo content MIME types).
    private val whatsapp =
        arrayOf("image/gif", "video/x.looping_mp4", "image/jpeg", "image/jpg", "image/png", "image/webp.wasticker")
    private val telegram = arrayOf("image/gif", "image/*", "image/jpg", "image/png", "image/webp")
    private val instagram = arrayOf("image/png", "image/jpeg", "image/gif")

    @Test
    fun `each app gets the format it supports`() {
        assertEquals(MemeFormat.WHATSAPP_STICKER, MemeFormat.choose("image/png", whatsapp))
        assertEquals(MemeFormat.WEBP_STICKER, MemeFormat.choose("image/jpeg", telegram))
        assertEquals(MemeFormat.SQUARE_JPEG, MemeFormat.choose("image/webp", instagram))
        assertEquals(MemeFormat.SQUARE_JPEG, MemeFormat.choose("image/png", arrayOf("image/*")))
        assertEquals(MemeFormat.ORIGINAL, MemeFormat.choose("image/png", emptyArray()))
    }

    @Test
    fun `gifs are sent as they are`() {
        assertEquals(MemeFormat.ORIGINAL, MemeFormat.choose("image/gif", whatsapp))
        assertEquals(MemeFormat.ORIGINAL, MemeFormat.choose("image/gif", telegram))
    }

    @Test
    fun `the image is centered with bars on the short side`() {
        assertEquals(Rect(0, 128, 512, 384), MemeFormat.fit(200, 100, 512))
        assertEquals(Rect(128, 0, 384, 512), MemeFormat.fit(100, 200, 512))
        assertEquals(Rect(0, 0, 512, 512), MemeFormat.fit(300, 300, 512))
    }

    @Test
    fun `animated webp is recognised from its header`() {
        val animated = webpHeader(flags = 0x02)
        val still = webpHeader(flags = 0x00)
        assertTrue(MemeFormat.isAnimatedWebp(animated))
        assertFalse(MemeFormat.isAnimatedWebp(still))
    }

    @Test
    fun `a wide png becomes a 512 px whatsapp sticker with black bars`() {
        assumeNativeGraphics()
        val meme = pngMeme(width = 200, height = 100)
        val prepared = MemeSender(context) { Uri.parse("content://share/${it.name}") }.prepare(meme, whatsapp)

        assertNotNull(prepared)
        assertEquals("image/webp.wasticker", prepared!!.mimeType)
        assertTrue(prepared.file.length() <= 100 * 1024)
        val sticker = BitmapFactory.decodeFile(prepared.file.path)
        assertEquals(512, sticker.width)
        assertEquals(512, sticker.height)
        assertEquals(Color.BLACK, opaque(sticker.getPixel(256, 20)))
        assertEquals(Color.RED, opaque(sticker.getPixel(256, 256)))
    }

    @Test
    fun `a png with transparency keeps it in the sticker, with transparent bars`() {
        assumeNativeGraphics()
        val meme = pngMeme(width = 200, height = 100, transparentLeft = true)
        val prepared = MemeSender(context) { Uri.parse("content://share/${it.name}") }.prepare(meme, telegram)

        assertEquals("image/webp", prepared!!.mimeType)
        val sticker = BitmapFactory.decodeFile(prepared.file.path)
        assertEquals("bar", 0, Color.alpha(sticker.getPixel(256, 20)))
        assertEquals("transparent half", 0, Color.alpha(sticker.getPixel(64, 256)))
        assertEquals(Color.RED, opaque(sticker.getPixel(448, 256)))
    }

    @Test
    fun `a png with transparency still gets a black background as a jpeg`() {
        assumeNativeGraphics()
        val meme = pngMeme(width = 100, height = 100, transparentLeft = true)
        val prepared = MemeSender(context) { Uri.parse("content://share/${it.name}") }.prepare(meme, instagram)

        assertEquals("image/jpeg", prepared!!.mimeType)
        val square = BitmapFactory.decodeFile(prepared.file.path)
        assertEquals(Color.BLACK, opaque(square.getPixel(10, 50)))
    }

    @Test
    fun `instagram gets a square jpeg`() {
        assumeNativeGraphics()
        val meme = pngMeme(width = 100, height = 300)
        val prepared = MemeSender(context) { Uri.parse("content://share/${it.name}") }.prepare(meme, instagram)

        assertEquals("image/jpeg", prepared!!.mimeType)
        val square = BitmapFactory.decodeFile(prepared.file.path)
        assertEquals(300, square.width)
        assertEquals(300, square.height)
        assertEquals(Color.BLACK, opaque(square.getPixel(10, 150)))
    }

    /** Real image encoding needs Robolectric's native graphics, which 4.11 lacks on Windows (CI runs Linux). */
    private fun assumeNativeGraphics() = assumeFalse(System.getProperty("os.name").orEmpty().startsWith("Windows"))

    /** Ignores JPEG/WebP rounding: compares channels with a tolerance. */
    private fun opaque(pixel: Int): Int {
        fun snap(channel: Int) = if (channel > 127) 255 else 0
        return Color.rgb(snap(Color.red(pixel)), snap(Color.green(pixel)), snap(Color.blue(pixel)))
    }

    /** A red PNG; with [transparentLeft], its left half is fully transparent. */
    private fun pngMeme(
        width: Int,
        height: Int,
        transparentLeft: Boolean = false,
    ): Meme {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        if (transparentLeft) {
            for (x in 0 until width / 2) for (y in 0 until height) bitmap.setPixel(x, y, Color.TRANSPARENT)
        }
        val bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        val name = "rojo-${width}x$height${if (transparentLeft) "-transparente" else ""}.png"
        val uri = Uri.parse("content://memes.test/document/$name")
        shadowOf(context.contentResolver).registerInputStreamSupplier(uri) { ByteArrayInputStream(bytes) }
        return Meme(uri.toString(), name, "image/png")
    }

    private fun webpHeader(flags: Int): File {
        val header = "RIFF\u0000\u0000\u0000\u0000WEBPVP8X\u000A\u0000\u0000\u0000".toByteArray(Charsets.ISO_8859_1)
        return File.createTempFile("meme", ".webp").apply { writeBytes(header + byteArrayOf(flags.toByte(), 0, 0, 0)) }
    }
}
