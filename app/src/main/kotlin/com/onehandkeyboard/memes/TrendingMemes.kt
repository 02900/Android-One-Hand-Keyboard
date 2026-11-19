package com.onehandkeyboard.memes

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/** One photo of Know Your Meme's trending gallery. */
data class TrendingPhoto(
    val title: String,
    /** Full-size image, downloaded only when the meme is sent. */
    val imageUrl: String,
    /** Small preview (~15 KB) shown in the panel. */
    val previewUrl: String,
)

/**
 * Reads the photos of https://knowyourmeme.com/photos/trending from its HTML (the site has no
 * public API). Photos marked as sensitive, NSFW or spoilers are skipped, and only images on
 * Know Your Meme's own CDN are accepted.
 */
object TrendingParser {
    private val ITEM = Regex("""<a\b([^>]*\bdata-is-photo="true"[^>]*)>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)
    private val IMG = Regex("""<img\b[^>]*>""")
    private val ATTRIBUTE = Regex("""([\w-]+)="([^"]*)"""")
    private val HIDDEN = Regex("""nsfw|sensitive|spoiler""", RegexOption.IGNORE_CASE)
    private const val CDN = "https://i.kym-cdn.com/photos/images/"

    fun parse(html: String): List<TrendingPhoto> =
        ITEM
            .findAll(html)
            .mapNotNull { item -> photo(attributes(item.groupValues[1]), item.groupValues[2]) }
            .distinctBy { it.imageUrl }
            .toList()

    private fun photo(
        link: Map<String, String>,
        body: String,
    ): TrendingPhoto? {
        val tag = IMG.find(body)?.value ?: return null
        val image = attributes(tag)
        val hidden =
            HIDDEN.containsMatchIn(image["class"].orEmpty()) ||
                HIDDEN.containsMatchIn(link["class"].orEmpty()) ||
                HIDDEN.containsMatchIn(link["data-entry-link"].orEmpty()) ||
                HIDDEN.containsMatchIn(link["href"].orEmpty())
        val full = image["data-image"]?.takeIf { it.startsWith(CDN) }
        val preview = image["src"]?.takeIf { it.startsWith(CDN) } ?: full
        if (hidden || full == null || preview == null) return null
        val title = unescape(link["data-title"] ?: image["alt"].orEmpty()).trim().ifEmpty { "meme" }
        return TrendingPhoto(title, full, preview)
    }

    private fun attributes(tag: String): Map<String, String> =
        ATTRIBUTE.findAll(tag).associate { it.groupValues[1] to it.groupValues[2] }

    internal fun unescape(text: String): String =
        text
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&#x27;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
}

/**
 * The "🔥 Tendencias" tab: Know Your Meme's trending photos (only in builds with Internet).
 *
 * - The gallery is read at most every [REFRESH_MS]; the previews are kept in the app's cache.
 * - The full image is downloaded only when a meme is sent ([download]).
 * - Nothing is sent to the site besides the requests themselves (no account, no cookies).
 *
 * Blocking: call everything off the main thread.
 */
class TrendingMemes(
    cacheDir: File,
    private val userAgent: String,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val folder = File(cacheDir, CACHE_DIR)

    @Volatile private var cached: MemeCategory? = null

    @Volatile private var loadedAt = 0L

    /** The trending tab, or null when the site can't be reached (offline, changed page…). */
    @Synchronized
    fun load(): MemeCategory? = cached?.takeIf { now() - loadedAt < REFRESH_MS } ?: refresh() ?: cached

    private fun refresh(): MemeCategory? {
        val photos =
            try {
                TrendingParser.parse(get(PAGE_URL, MAX_PAGE_BYTES).decodeToString()).take(MAX_PHOTOS)
            } catch (_: IOException) {
                emptyList()
            }
        folder.mkdirs()
        val memes = photos.mapNotNull(::meme)
        if (memes.isEmpty()) return null
        clean(keep = memes.map { it.previewUri }.toSet())
        loadedAt = now()
        return MemeCategory(MemeCatalog.TRENDING_CATEGORY, memes).also { cached = it }
    }

    /** Downloads the full image of a trending meme to [dest]. */
    fun download(
        url: String,
        dest: File,
    ): Boolean {
        if (!url.startsWith(CDN)) return false
        return try {
            dest.parentFile?.mkdirs()
            dest.writeBytes(get(url, MAX_IMAGE_BYTES))
            true
        } catch (_: IOException) {
            dest.delete()
            false
        }
    }

    private fun meme(photo: TrendingPhoto): Meme? {
        val preview = File(folder, Integer.toHexString(photo.previewUrl.hashCode()))
        val ready = preview.length() > 0 || download(photo.previewUrl, preview)
        if (!ready) return null
        preview.setLastModified(now())
        val extension = photo.imageUrl.substringAfterLast('/').substringAfterLast('.', "").lowercase()
        val type = MemeCatalog.mimeTypeOf("x.$extension", null) ?: "image/jpeg"
        return Meme(photo.imageUrl, "${photo.title}.${extension.ifEmpty { "jpg" }}", type, preview.toURI().toString())
    }

    /** Drops previews that are no longer trending. */
    private fun clean(keep: Set<String>) {
        folder.listFiles()?.forEach { if (it.toURI().toString() !in keep) it.delete() }
    }

    private fun get(
        url: String,
        maxBytes: Int,
    ): ByteArray {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", userAgent)
        try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code")
            return connection.inputStream.use { readLimited(it, maxBytes) }
        } finally {
            connection.disconnect()
        }
    }

    private fun readLimited(
        input: InputStream,
        maxBytes: Int,
    ): ByteArray {
        val bytes = input.readNBytesCompat(maxBytes + 1)
        if (bytes.size > maxBytes) throw IOException("Response too big")
        return bytes
    }

    private fun InputStream.readNBytesCompat(limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_BYTES)
        while (out.size() < limit) {
            val read = read(buffer, 0, minOf(buffer.size, limit - out.size()))
            if (read < 0) break
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    companion object {
        const val PAGE_URL = "https://knowyourmeme.com/photos/trending"
        private const val CDN = "https://i.kym-cdn.com/"
        private const val CACHE_DIR = "trending"
        private const val REFRESH_MS = 15L * 60 * 1000
        private const val MAX_PHOTOS = 40
        private const val MAX_PAGE_BYTES = 3 * 1024 * 1024
        private const val MAX_IMAGE_BYTES = 15 * 1024 * 1024
        private const val TIMEOUT_MS = 15_000
        private const val BUFFER_BYTES = 16 * 1024
    }
}
