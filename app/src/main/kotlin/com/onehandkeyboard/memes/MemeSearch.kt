package com.onehandkeyboard.memes

import com.onehandkeyboard.emoji.EmojiKitchen
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** One page of Know Your Meme's image search. */
data class SearchPage(
    val memes: List<Meme>,
    /** Where the next page starts, or null when there are no more results. */
    val nextOffset: Int?,
)

/**
 * Reads a page of Know Your Meme's image search, in the JSON the site itself asks for while
 * scrolling its results (the site has no public API).
 *
 * Images marked as NSFW, sensitive or spoilers are skipped, and only images on Know Your Meme's
 * own CDN are accepted.
 */
object MemeSearchParser {
    private const val CDN = "https://i.kym-cdn.com/photos/images/"
    private val HIDDEN_FLAGS = listOf("nsfw", "sensitive", "spoiler", "nsfw_or_sensitive")

    /** @throws JSONException when the site answers something else (a captcha page, an error…). */
    fun parse(
        json: String,
        offset: Int,
    ): SearchPage {
        val groups = JSONObject(json).optJSONArray("groups")
        val items = mutableListOf<JSONObject>()
        for (g in 0 until (groups?.length() ?: 0)) {
            val list = groups?.optJSONObject(g)?.optJSONArray("items") ?: continue
            for (i in 0 until list.length()) list.optJSONObject(i)?.let(items::add)
        }
        if (items.isEmpty()) return SearchPage(emptyList(), nextOffset = null)
        val next = maxOf(offset + items.size, items.maxOf { it.optInt("offset", -1) } + 1)
        val memes = items.mapNotNull(::meme).distinctBy { it.uri }
        return SearchPage(memes, next)
    }

    private fun meme(item: JSONObject): Meme? {
        if (HIDDEN_FLAGS.any { item.optBoolean(it, false) }) return null
        val image = item.optString("image").takeIf { it.startsWith(CDN) } ?: return null
        val preview = item.optString("image_masonry").takeIf { it.startsWith(CDN) } ?: image
        val title =
            item
                .optString("title")
                .ifBlank { item.optString("entry_title") }
                .trim()
                .ifEmpty { "meme" }
                .replace('/', ' ')
        val extension = image.substringAfterLast('/').substringAfterLast('.', "").lowercase()
        val type = MemeCatalog.mimeTypeOf("x.$extension", null) ?: "image/jpeg"
        return Meme(image, "$title.${extension.ifEmpty { "jpg" }}", type, previewUri = preview)
    }
}

/**
 * Searches Know Your Meme's images (only in builds with Internet).
 *
 * - [search] reads one page of results; the panel asks for the next one when scrolled near the end.
 * - Previews are fetched by the panel as they scroll into view ([fetch]); the full image is
 *   downloaded only when a meme is sent ([download]).
 * - Nothing is sent to the site besides the query itself (no account, no cookies).
 * - The emoji panel also gets its Emoji Kitchen mixes through [fetch] and [download].
 *
 * Blocking: call everything off the main thread.
 */
class MemeSearch(
    private val userAgent: String,
) {
    /** A page of results for [query] from [offset], or null when the site can't be reached or read. */
    fun search(
        query: String,
        offset: Int = 0,
    ): SearchPage? =
        try {
            val json = get(searchUrl(query, offset), MAX_PAGE_BYTES, json = true).decodeToString()
            MemeSearchParser.parse(json, offset)
        } catch (_: IOException) {
            null
        } catch (_: JSONException) {
            null
        }

    /** The bytes of a preview, or null. */
    fun fetch(url: String): ByteArray? {
        if (!isImage(url)) return null
        return try {
            get(url, MAX_PREVIEW_BYTES)
        } catch (_: IOException) {
            null
        }
    }

    /** Downloads the full image of a meme to [dest]. */
    fun download(
        url: String,
        dest: File,
    ): Boolean {
        if (!isImage(url)) return false
        return try {
            dest.parentFile?.mkdirs()
            dest.writeBytes(get(url, MAX_IMAGE_BYTES))
            true
        } catch (_: IOException) {
            dest.delete()
            false
        }
    }

    /** Images come only from Know Your Meme's CDN and Emoji Kitchen's mixes (the emoji panel). */
    private fun isImage(url: String) = url.startsWith(CDN) || url.startsWith(EmojiKitchen.IMAGE_HOST)

    private fun get(
        url: String,
        maxBytes: Int,
        json: Boolean = false,
    ): ByteArray {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", userAgent)
        if (json) {
            // What the site's own infinite scroll sends to get the results as JSON.
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
        }
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
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_BYTES)
        while (out.size() <= maxBytes) {
            val read = input.read(buffer, 0, minOf(buffer.size, maxBytes + 1 - out.size()))
            if (read < 0) break
            out.write(buffer, 0, read)
        }
        if (out.size() > maxBytes) throw IOException("Response too big")
        return out.toByteArray()
    }

    companion object {
        private const val CDN = "https://i.kym-cdn.com/"
        private const val MAX_PAGE_BYTES = 3 * 1024 * 1024
        private const val MAX_PREVIEW_BYTES = 2 * 1024 * 1024
        private const val MAX_IMAGE_BYTES = 15 * 1024 * 1024
        private const val TIMEOUT_MS = 15_000
        private const val BUFFER_BYTES = 16 * 1024

        fun searchUrl(
            query: String,
            offset: Int,
        ): String {
            val q = URLEncoder.encode(query.trim(), "UTF-8").replace("+", "%20")
            return "https://knowyourmeme.com/search?context=images&sort=&q=$q&offset=$offset"
        }
    }
}
