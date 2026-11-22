package com.onehandkeyboard.memes

import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** org.json comes from Android: Robolectric provides it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MemeSearchParserTest {
    /** A result as knowyourmeme.com/search?context=images serves it as JSON (trimmed). */
    private fun item(
        title: String,
        id: String,
        offset: Int,
        flags: String = "",
        cdn: String = "https://i.kym-cdn.com/photos/images",
    ) = """
        {"title": "$title", "entry_title": "Enel Face", "offset": $offset, "is_photo": true,
         "image": "$cdn/original/$id", "image_masonry": "$cdn/masonry/$id",
         "nsfw": false, "spoiler": false, "sensitive": false $flags}
        """

    private fun page(vararg groups: List<String>) =
        """{"total_results": 38, "groups": [${groups.joinToString { """{"items": [${it.joinToString()}]}""" }}]}"""

    @Test
    fun `reads each image with its preview, and where the next page starts`() {
        val json =
            page(
                listOf(item("Enel Face Exploitable", "002/846/619/c35.png", 16), item("Tom &amp; Jerry", "1.gif", 17)),
                listOf(item("", "2.jpeg", 18)),
            )

        val result = MemeSearchParser.parse(json, offset = 16)

        assertEquals(
            Meme(
                "https://i.kym-cdn.com/photos/images/original/002/846/619/c35.png",
                "Enel Face Exploitable.png",
                "image/png",
                previewUri = "https://i.kym-cdn.com/photos/images/masonry/002/846/619/c35.png",
            ),
            result.memes[0],
        )
        assertEquals("image/gif", result.memes[1].mimeType)
        assertEquals("Enel Face.jpeg", result.memes[2].fileName) // no title: the entry's
        assertTrue(result.memes.all { it.isRemote })
        assertEquals(19, result.nextOffset)
    }

    @Test
    fun `skips hidden images and images outside the site's CDN, but still moves on`() {
        val json =
            page(
                listOf(
                    item("Blurred", "1.png", 0, flags = """, "nsfw": true"""),
                    item("Sensitive", "2.png", 1, flags = """, "nsfw_or_sensitive": true"""),
                    item("Spoiler", "3.png", 2, flags = """, "spoiler": true"""),
                    item("Elsewhere", "4.png", 3, cdn = "https://evil.example/photos/images"),
                    item("Ok", "5.png", 4),
                    item("Ok again", "5.png", 5),
                ),
            )

        val result = MemeSearchParser.parse(json, offset = 0)

        assertEquals(listOf("Ok"), result.memes.map { it.title })
        assertEquals(6, result.nextOffset)
    }

    @Test
    fun `no results means no more pages`() {
        val result = MemeSearchParser.parse("""{"total_results": 0, "groups": []}""", offset = 32)

        assertTrue(result.memes.isEmpty())
        assertNull(result.nextOffset)
    }

    @Test(expected = JSONException::class)
    fun `a page that is not JSON (a captcha, an error) fails`() {
        MemeSearchParser.parse("<html>Cloudflare check</html>", offset = 0)
    }

    @Test
    fun `the query goes in the address encoded`() {
        assertEquals(
            "https://knowyourmeme.com/search?context=images&sort=&q=enel%20face%20%26%20%C3%B1&offset=16",
            MemeSearch.searchUrl(" enel face & ñ ", 16),
        )
    }
}
