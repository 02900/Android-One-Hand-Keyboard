package com.onehandkeyboard.input

/**
 * Removes tracking from the links in pasted text.
 *
 * Links to social networks (Instagram, X, TikTok…) lose their whole query string, except the
 * few parameters that say *what* is linked (YouTube's `v`, Facebook's `story_fbid`…). Links to
 * any other site only lose well-known trackers (`utm_*`, `fbclid`, `gclid`…), since their
 * parameters may matter. The rest of the text, and the `#fragment`, are left as they were.
 */
object LinkCleaner {
    fun clean(text: CharSequence): String = URL.replace(text) { cleanUrl(it.value) }

    /** [url] without its tracking parameters; punctuation right after it is kept. */
    fun cleanUrl(url: String): String {
        val end = url.trimEnd { it in TRAILING }
        val link = url.substring(0, end.length)
        val trailing = url.substring(end.length)
        val fragmentAt = link.indexOf('#').takeIf { it >= 0 } ?: link.length
        val queryAt = link.indexOf('?')
        if (queryAt < 0 || queryAt > fragmentAt) return url

        val base = link.substring(0, queryAt)
        val keep = keeperFor(host(base))
        val kept =
            link
                .substring(queryAt + 1, fragmentAt)
                .split('&')
                .filter { it.isNotEmpty() && keep(it.substringBefore('=').lowercase()) }
        val query = if (kept.isEmpty()) "" else kept.joinToString("&", prefix = "?")
        return base + query + link.substring(fragmentAt) + trailing
    }

    private fun host(base: String): String =
        base
            .substringAfter("://")
            .substringBefore('/')
            .substringAfterLast('@')
            .substringBefore(':')
            .lowercase()

    /** Which parameters stay for links to [host]. */
    private fun keeperFor(host: String): (String) -> Boolean {
        val social = SOCIAL.entries.firstOrNull { (domain, _) -> host == domain || host.endsWith(".$domain") }
        return if (social != null) {
            { it in social.value }
        } else {
            { !isTracker(it) }
        }
    }

    private fun isTracker(name: String) = name.startsWith("utm_") || name in TRACKERS

    private val URL = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)

    /** Punctuation that usually closes a sentence rather than the link. */
    private const val TRAILING = ".,;:!?)]}"

    /** Social networks: every parameter goes except these, which name the post or video. */
    private val SOCIAL =
        mapOf(
            "instagram.com" to emptySet(),
            "threads.net" to emptySet(),
            "threads.com" to emptySet(),
            "x.com" to setOf("q"),
            "twitter.com" to setOf("q"),
            "tiktok.com" to setOf("q"),
            "facebook.com" to setOf("story_fbid", "id", "fbid", "v", "set"),
            "fb.watch" to emptySet(),
            "youtube.com" to setOf("v", "t", "list", "index", "start", "search_query"),
            "youtu.be" to setOf("t", "list"),
            "reddit.com" to setOf("q"),
            "redd.it" to emptySet(),
            "linkedin.com" to emptySet(),
            "pinterest.com" to emptySet(),
            "pin.it" to emptySet(),
            "snapchat.com" to emptySet(),
            "spotify.com" to emptySet(),
            "spotify.link" to emptySet(),
            "bsky.app" to emptySet(),
            "twitch.tv" to emptySet(),
        )

    /** Trackers removed from links to any site. */
    private val TRACKERS =
        setOf(
            "fbclid", "gclid", "gclsrc", "dclid", "gbraid", "wbraid", "msclkid", "yclid",
            "twclid", "ttclid", "igsh", "igshid", "mc_cid", "mc_eid", "_hsenc", "_hsmi", "mkt_tok",
        )
}
