package com.onehandkeyboard.emoji

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException

/** Uses the real bundled table (assets/emoji/kitchen.bin). */
class EmojiKitchenTest {
    private val kitchen by lazy { File("src/main/assets/emoji/kitchen.bin").inputStream().use(EmojiKitchen::read) }

    @Test
    fun `a mix has the address of its image in either order`() {
        val url = "https://www.gstatic.com/android/keyboard/emojikitchen/20211115/u1f436/u1f436_u1f600.png"
        assertEquals(EmojiMix("😀", "🐶", url), kitchen.mix("😀", "🐶"))
        assertEquals(EmojiMix("🐶", "😀", url), kitchen.mix("🐶", "😀"))
        assertEquals(
            "https://www.gstatic.com/android/keyboard/emojikitchen/20201001/u1f602/u1f602_u1f602.png",
            kitchen.mix("😂", "😂")?.url,
        )
    }

    @Test
    fun `emojis with a variation selector match with or without it`() {
        val url = "https://www.gstatic.com/android/keyboard/emojikitchen/20230821/u2764-ufe0f/u2764-ufe0f_u1f525.png"
        assertEquals(url, kitchen.mix("❤️", "🔥")?.url)
        assertEquals(url, kitchen.mix("❤", "🔥")?.url)
        assertTrue(kitchen.supports("❤"))
    }

    @Test
    fun `emojis Emoji Kitchen doesn't have don't mix`() {
        assertFalse(kitchen.supports("🇦🇷"))
        assertNull(kitchen.mix("🇦🇷", "😀"))
        assertNull(kitchen.mix("😀", "a"))
    }

    @Test(expected = IOException::class)
    fun `another file is rejected`() {
        EmojiKitchen.read(ByteArrayInputStream("WSET1234".toByteArray()))
    }

    @Test(expected = IOException::class)
    fun `a mix with an emoji out of the table is rejected`() {
        // One emoji ("1f600"), one date, and a mix with emoji #5.
        val bytes =
            byteArrayOf(0x45, 0x4B, 0x49, 0x54, 0, 1, 5) + "1f600".toByteArray() +
                byteArrayOf(1, 1, 0x34, 0x5D, 0x21, 0, 1, 1, 0x40)
        EmojiKitchen.read(ByteArrayInputStream(bytes))
    }
}
