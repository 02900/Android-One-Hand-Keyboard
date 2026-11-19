package com.onehandkeyboard.memes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrendingParserTest {
    /** A gallery item as knowyourmeme.com/photos/trending serves it (trimmed). */
    private fun item(
        title: String,
        id: String,
        imgClass: String = "",
        entry: String = "/memes/some-meme",
        cdn: String = "https://i.kym-cdn.com/photos/images",
    ) = """
        <a class="item" onclick='let tmpMode = false; tmpMode == "hand-select" ? toggleSelect(this, event) : showLightBox(this, event)'
           alt="alt text" data-entry-link="$entry" data-is-photo="true" data-offset="0" data-title="$title"
           href="https://knowyourmeme.com:443/photos/$id-slug">
        <div><h3>$title</h3>
        <div class="not-vertical-only">
        <img loading="lazy" class="$imgClass" alt="alt text" data-image="$cdn/original/$id"
             src="$cdn/masonry/$id" style="aspect-ratio: 0.8;">
        </div>
        <div class="vertical-only"><img loading="lazy" src="$cdn/original/$id"></div>
        <span href="/memes/some-meme">Some meme</span>
        </div>
        </a>
        """

    @Test
    fun `reads title, full image and preview of each photo`() {
        val html =
            "<html><a class=\"result\" href=\"/memes/x\"><img src=\"https://i.kym-cdn.com/entries/icons/x.jpg\"></a>" +
                item("Maid Lucina", "003/323/021/577") + item("Tom &amp; Jerry", "003/301/542/bdd.jpg") + "</html>"

        val photos = TrendingParser.parse(html)

        assertEquals(2, photos.size)
        assertEquals(
            TrendingPhoto(
                "Maid Lucina",
                "https://i.kym-cdn.com/photos/images/original/003/323/021/577",
                "https://i.kym-cdn.com/photos/images/masonry/003/323/021/577",
            ),
            photos[0],
        )
        assertEquals("Tom & Jerry", photos[1].title)
        assertEquals("https://i.kym-cdn.com/photos/images/original/003/301/542/bdd.jpg", photos[1].imageUrl)
    }

    @Test
    fun `skips sensitive photos and images outside the site's CDN`() {
        val html =
            item("Blurred", "1", imgClass = "nsfw-img") +
                item("Sensitive entry", "2", entry = "/sensitive/memes/x") +
                item("Elsewhere", "3", cdn = "https://evil.example/photos/images") +
                item("Ok", "4")

        assertEquals(listOf("Ok"), TrendingParser.parse(html).map { it.title })
    }

    @Test
    fun `repeated photos appear once and a page without photos gives none`() {
        assertEquals(1, TrendingParser.parse(item("A", "1") + item("A again", "1")).size)
        assertTrue(TrendingParser.parse("<html><body>Cloudflare check</body></html>").isEmpty())
    }

    @Test
    fun `trending tab goes first, after the recents`() {
        val folder = MemeCategory("Risa", listOf(Meme("content://a", "a.gif", "image/gif")))
        val trending =
            MemeCategory(
                MemeCatalog.TRENDING_CATEGORY,
                listOf(Meme("https://i.kym-cdn.com/photos/images/original/1", "t.jpg", "image/jpeg", "file:/p")),
            )

        val tabs = MemeCatalog.withRecents(MemeCatalog.withTrending(listOf(folder), trending), listOf("content://a"))

        assertEquals(
            listOf(MemeCatalog.RECENT_CATEGORY, MemeCatalog.TRENDING_CATEGORY, "Risa"),
            tabs.map { it.name },
        )
        assertEquals(listOf(folder), MemeCatalog.withTrending(listOf(folder), null))
        assertTrue(trending.memes[0].isRemote)
        assertEquals("file:/p", trending.memes[0].previewUri)
        assertEquals("content://a", folder.memes[0].previewUri)
    }
}
