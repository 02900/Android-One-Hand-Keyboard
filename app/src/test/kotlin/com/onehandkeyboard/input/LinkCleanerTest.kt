package com.onehandkeyboard.input

import org.junit.Assert.assertEquals
import org.junit.Test

class LinkCleanerTest {
    private fun assertCleans(
        expected: String,
        pasted: String,
    ) = assertEquals(expected, LinkCleaner.clean(pasted))

    @Test
    fun `social links lose their whole query`() {
        assertCleans(
            "https://www.instagram.com/reel/DAbc123/",
            "https://www.instagram.com/reel/DAbc123/?igsh=MWQ1ZGUxMzBkMA==",
        )
        assertCleans(
            "https://x.com/user/status/1790000000000000000",
            "https://x.com/user/status/1790000000000000000?s=46&t=AbC-dEf",
        )
        assertCleans("https://twitter.com/user/status/1", "https://twitter.com/user/status/1?ref_src=twsrc%5Etfw")
        assertCleans(
            "https://www.tiktok.com/@user/video/7300000000000000000",
            "https://www.tiktok.com/@user/video/7300000000000000000?is_from_webapp=1&sender_device=pc&_r=1&_t=8abc",
        )
        assertCleans("https://vm.tiktok.com/ZMabc123/", "https://vm.tiktok.com/ZMabc123/?_t=8abc")
        assertCleans(
            "https://open.spotify.com/track/4uLU6hMCjMI75M1A2tKUQC",
            "https://open.spotify.com/track/4uLU6hMCjMI75M1A2tKUQC?si=abc123",
        )
        assertCleans("https://www.threads.net/@user/post/C1", "https://www.threads.net/@user/post/C1?xmt=AQG")
    }

    @Test
    fun `parameters that name the post or video stay`() {
        assertCleans(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=42",
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ&si=xyz&t=42&pp=ygU",
        )
        assertCleans("https://youtu.be/dQw4w9WgXcQ?t=42", "https://youtu.be/dQw4w9WgXcQ?si=Ab_Cd&t=42")
        assertCleans(
            "https://m.facebook.com/story.php?story_fbid=123&id=456",
            "https://m.facebook.com/story.php?story_fbid=123&id=456&mibextid=Nif5oz",
        )
        assertCleans("https://x.com/search?q=enel%20face", "https://x.com/search?q=enel%20face&src=typed_query")
    }

    @Test
    fun `other sites only lose known trackers`() {
        assertCleans(
            "https://example.com/news?id=7&page=2",
            "https://example.com/news?utm_source=whatsapp&id=7&fbclid=IwAR0&page=2&utm_medium=social",
        )
        assertCleans("https://shop.example/item?color=red", "https://shop.example/item?color=red")
        assertCleans("https://example.com/a", "https://example.com/a?gclid=abc")
    }

    @Test
    fun `the text around, the fragment and closing punctuation stay`() {
        assertCleans(
            "Mira esto: https://x.com/user/status/1. Y esto (https://www.instagram.com/p/C2/)!",
            "Mira esto: https://x.com/user/status/1?s=20. Y esto (https://www.instagram.com/p/C2/?igsh=abc)!",
        )
        assertCleans("https://example.com/doc?id=1#part-2", "https://example.com/doc?id=1&utm_campaign=x#part-2")
        assertCleans("https://x.com/user#top", "https://x.com/user?s=09#top")
    }

    @Test
    fun `text without links and look-alike domains are left alone`() {
        assertCleans("hola ¿qué tal? a=1&b=2", "hola ¿qué tal? a=1&b=2")
        assertCleans("https://notx.com/page?s=20", "https://notx.com/page?s=20")
        assertCleans("https://example.com/#/route?utm_source=x", "https://example.com/#/route?utm_source=x")
    }
}
