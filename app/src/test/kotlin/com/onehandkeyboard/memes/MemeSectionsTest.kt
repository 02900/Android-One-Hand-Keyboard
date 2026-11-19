package com.onehandkeyboard.memes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemeSectionsTest {
    private fun meme(name: String) = Meme("content://memes/$name", "$name.gif", "image/gif")

    private val sections =
        MemeSections(
            listOf(
                MemeCategory("🕘 Recientes", listOf(meme("b"))),
                MemeCategory("Vacía", emptyList()),
                MemeCategory("Risa", listOf(meme("a"), meme("b"))),
            ),
        )

    @Test
    fun `each category is a title followed by its memes, empty ones left out`() {
        assertEquals(listOf("🕘 Recientes", "Risa"), sections.sectionNames)
        assertEquals(
            listOf(
                MemeRow.Header("🕘 Recientes", 0),
                MemeRow.Item(meme("b"), 0),
                MemeRow.Header("Risa", 1),
                MemeRow.Item(meme("a"), 1),
                MemeRow.Item(meme("b"), 1),
            ),
            sections.rows,
        )
    }

    @Test
    fun `positions map to their category and back`() {
        assertEquals(0, sections.sectionAt(1))
        assertEquals(1, sections.sectionAt(2))
        assertEquals(1, sections.sectionAt(99))
        assertEquals(0, sections.sectionAt(-3))
        assertEquals(2, sections.headerPosition(1))
    }

    @Test
    fun `keys tell the same meme apart in different categories`() {
        assertEquals("#Risa", sections.keyAt(2))
        assertEquals(1, sections.positionOfKey(sections.keyAt(1)))
        assertEquals(4, sections.positionOfKey(sections.keyAt(4)))
        assertEquals(-1, sections.positionOfKey("nada"))
        assertEquals(-1, sections.positionOfKey(null))
    }

    @Test
    fun `fast scroll fraction maps to the first, last and middle rows`() {
        assertEquals(0, MemeSections.positionForFraction(0f, 5))
        assertEquals(4, MemeSections.positionForFraction(1f, 5))
        assertEquals(2, MemeSections.positionForFraction(0.5f, 5))
        assertEquals(4, MemeSections.positionForFraction(3f, 5))
        assertEquals(0, MemeSections.positionForFraction(0.5f, 0))
        assertTrue(MemeSections(emptyList()).isEmpty())
    }
}
