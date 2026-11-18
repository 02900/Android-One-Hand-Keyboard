package com.onehandkeyboard.memes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MemeCatalogTest {
    private fun file(
        name: String,
        folder: String? = null,
        type: String? = null,
    ) = ScannedFile("content://memes/${folder.orEmpty()}/$name", name, type, folder)

    @Test
    fun `sub-folders become categories sorted by name without the order prefix`() {
        val categories =
            MemeCatalog.build(
                listOf(
                    file("b.gif", "02-Saludos"),
                    file("a.gif", "01-Reacciones"),
                    file("c.webp", "10_Stickers"),
                ),
            )
        assertEquals(listOf("Reacciones", "Saludos", "Stickers"), categories.map { it.name })
    }

    @Test
    fun `loose files go to the general tab after the folders`() {
        val categories = MemeCatalog.build(listOf(file("suelto.png"), file("a.gif", "Risa")))
        assertEquals(listOf("Risa", MemeCatalog.LOOSE_CATEGORY), categories.map { it.name })
        assertEquals(listOf("suelto.png"), categories.last().memes.map { it.fileName })
    }

    @Test
    fun `memes inside a tab are sorted by file name ignoring case`() {
        val memes = MemeCatalog.build(listOf(file("c.gif", "R"), file("A.gif", "R"), file("b.gif", "R"))).single().memes
        assertEquals(listOf("A.gif", "b.gif", "c.gif"), memes.map { it.fileName })
    }

    @Test
    fun `only images apps accept are kept and empty or hidden folders are skipped`() {
        val categories =
            MemeCatalog.build(
                listOf(
                    file("video.mp4", "Videos"),
                    file("notas.txt", "Videos"),
                    file(".gitkeep", "Vacía"),
                    file("a.gif", ".oculta"),
                    file("ok.JPG", "Fotos"),
                ),
            )
        assertEquals(listOf("Fotos"), categories.map { it.name })
        assertEquals("image/jpeg", categories.single().memes.single().mimeType)
    }

    @Test
    fun `mime type comes from the provider when it is supported, else from the extension`() {
        assertEquals("image/webp", MemeCatalog.mimeTypeOf("sticker", "image/webp"))
        assertEquals("image/gif", MemeCatalog.mimeTypeOf("risa.gif", "application/octet-stream"))
        assertNull(MemeCatalog.mimeTypeOf("clip.mp4", "video/mp4"))
        assertNull(MemeCatalog.mimeTypeOf(".nomedia", null))
    }

    @Test
    fun `display name keeps names that are only a number`() {
        assertEquals("2024", MemeCatalog.displayName("2024"))
        assertEquals("Memes 2024", MemeCatalog.displayName("03 - Memes 2024"))
    }

    @Test
    fun `recents come first and skip memes no longer in the folder`() {
        val categories = MemeCatalog.build(listOf(file("a.gif", "R"), file("b.gif", "R")))
        val uriB = categories.single().memes[1].uri
        val tabs = MemeCatalog.withRecents(categories, listOf("content://borrado", uriB))
        assertEquals(listOf(MemeCatalog.RECENT_CATEGORY, "R"), tabs.map { it.name })
        assertEquals(listOf("b.gif"), tabs.first().memes.map { it.fileName })
        assertEquals(categories, MemeCatalog.withRecents(categories, emptyList()))
    }

    @Test
    fun `sending a meme moves it to the front of the recents, capped`() {
        val many = (1..MemeCatalog.MAX_RECENTS).map { "u$it" }
        val pushed = MemeCatalog.pushRecent(many, "u5")
        assertEquals("u5", pushed.first())
        assertEquals(MemeCatalog.MAX_RECENTS, pushed.size)
        assertEquals(1, pushed.count { it == "u5" })
        assertEquals(MemeCatalog.MAX_RECENTS, MemeCatalog.pushRecent(many, "nuevo").size)
    }

    @Test
    fun `previews are only scaled down`() {
        assertEquals(1f, MemeThumbnails.scaleFor(100, 50, 200))
        assertEquals(0.5f, MemeThumbnails.scaleFor(400, 200, 200))
        assertEquals(1f, MemeThumbnails.scaleFor(0, 0, 200))
    }
}
