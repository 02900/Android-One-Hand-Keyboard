package com.onehandkeyboard.emoji

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class EmojiCatalogTest {
    private val bundled by lazy { File("src/main/assets/emoji/emoji.tsv").useLines { EmojiCatalog.parse(it) } }

    @Test
    fun `the bundled list has every group, faces first, without skin tones`() {
        assertEquals(
            listOf("😀", "👋", "🐻", "🍔", "🚗", "⚽", "💡", "🔣", "🏳️"),
            bundled.map { it.icon },
        )
        assertEquals("Caras y emociones", bundled.first().name)
        assertEquals("😀", bundled.first().emojis.first())
        val all = bundled.flatMap { it.emojis }
        assertTrue("👍" in all && "❤️" in all && "🇦🇷" in all)
        assertFalse("skin tone variant", "👍🏽" in all)
        assertEquals(all.size, all.toSet().size)
    }

    @Test
    fun `emojis the phone can't draw are hidden and empty groups dropped`() {
        val lines = sequenceOf("😀\tCaras\t😀 🫠 😂\r", "🔣\tSímbolos\t🫟", "bad line")
        val groups = EmojiCatalog.parse(lines) { it != "🫠" && it != "🫟" }
        assertEquals(listOf(EmojiGroup("😀", "Caras", listOf("😀", "😂"))), groups)
    }

    @Test
    fun `recents come first, latest first, without repeats`() {
        val groups = listOf(EmojiGroup("😀", "Caras", listOf("😀")))
        assertEquals(groups, EmojiCatalog.withRecents(groups, emptyList()))
        assertEquals("Recientes", EmojiCatalog.withRecents(groups, listOf("🔥")).first().name)
        assertEquals(listOf("😂", "🔥"), EmojiCatalog.pushRecent(listOf("🔥", "😂"), "😂"))
        val full = (1..EmojiCatalog.MAX_RECENTS).map { "e$it" }
        assertEquals(EmojiCatalog.MAX_RECENTS, EmojiCatalog.pushRecent(full, "new").size)
    }
}
