package com.onehandkeyboard.emoji

import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream

/** An Emoji Kitchen mix of [first] and [second]: the image at [url] (Google's servers). */
data class EmojiMix(
    val first: String,
    val second: String,
    val url: String,
) {
    /** Accessibility text and file name of the sticker. */
    val title: String get() = "$first + $second"
}

/**
 * Which pairs of emojis have an Emoji Kitchen mix (Gboard's emoji fusions) and where their image
 * is, read from assets/emoji/kitchen.bin (tools/build_emoji.py). Only the list is bundled; the
 * images are downloaded from Google when shown.
 *
 * Kept compact (~300 KB): per emoji, the sorted list of its partners as the left one of the mix.
 */
class EmojiKitchen private constructor(
    /** Code points as written in the image addresses ("1f62e-200d-1f4a8"). */
    private val codes: Array<String>,
    private val dates: IntArray,
    /** Mixes with emoji i on the left: entries[starts[i] until starts[i + 1]], (right << 6 | date). */
    private val starts: IntArray,
    private val entries: CharArray,
) {
    /** Emoji (without variation selectors) → its index in [codes]. */
    private val index: Map<String, Int> = codes.withIndex().associate { (i, code) -> key(fromCode(code)) to i }

    /** Whether [emoji] mixes with anything. */
    fun supports(emoji: String): Boolean = key(emoji) in index

    /** The mix of [first] and [second] (in any order), or null if Emoji Kitchen has none. */
    fun mix(
        first: String,
        second: String,
    ): EmojiMix? {
        val a = index[key(first)] ?: return null
        val b = index[key(second)] ?: return null
        val date = dateOf(a, b)
        val url =
            if (date != null) {
                url(codes[a], codes[b], date)
            } else {
                dateOf(b, a)?.let { url(codes[b], codes[a], it) }
            }
        return url?.let { EmojiMix(first, second, it) }
    }

    /** Date of the mix with [left] on the left and [right] on the right, or null. */
    private fun dateOf(
        left: Int,
        right: Int,
    ): Int? {
        var low = starts[left]
        var high = starts[left + 1] - 1
        while (low <= high) {
            val middle = (low + high) ushr 1
            val partner = entries[middle].code ushr RIGHT_SHIFT
            when {
                partner < right -> low = middle + 1
                partner > right -> high = middle - 1
                else -> return dates[entries[middle].code and DATE_MASK]
            }
        }
        return null
    }

    companion object {
        private const val MAGIC = 0x454B4954 // "EKIT"
        private const val RIGHT_SHIFT = 6
        private const val DATE_MASK = (1 shl RIGHT_SHIFT) - 1
        private const val INITIAL_ENTRIES = 1 shl 16
        private const val HEX = 16
        private const val VARIATION_SELECTOR = 0xFE0F

        /** Where Emoji Kitchen's images are: the only images of this host the keyboard downloads. */
        const val IMAGE_HOST = "https://www.gstatic.com/android/keyboard/emojikitchen/"

        fun read(input: InputStream): EmojiKitchen {
            val data = DataInputStream(input.buffered())
            if (data.readInt() != MAGIC) throw IOException("Not an Emoji Kitchen table")
            val codes =
                Array(data.readUnsignedShort()) {
                    val bytes = ByteArray(data.readUnsignedByte())
                    data.readFully(bytes)
                    String(bytes, Charsets.UTF_8)
                }
            val dates = IntArray(data.readUnsignedByte()) { data.readInt() }
            val starts = IntArray(codes.size + 1)
            var entries = CharArray(INITIAL_ENTRIES)
            var size = 0
            for (i in codes.indices) {
                starts[i] = size
                repeat(data.readUnsignedShort()) {
                    val entry = data.readUnsignedShort()
                    if (entry ushr RIGHT_SHIFT >= codes.size || entry and DATE_MASK >= dates.size) {
                        throw IOException("Bad Emoji Kitchen table")
                    }
                    if (size == entries.size) entries = entries.copyOf(size * 2)
                    entries[size++] = entry.toChar()
                }
            }
            starts[codes.size] = size
            return EmojiKitchen(codes, dates, starts, entries.copyOf(size))
        }

        /** "1f62e-200d-1f4a8" → the emoji. */
        private fun fromCode(code: String): String =
            code.split('-').joinToString("") { String(Character.toChars(it.toInt(HEX))) }

        /** The emoji without variation selectors: "❤️" and "❤" are the same emoji here. */
        internal fun key(emoji: String): String =
            buildString {
                emoji.codePoints().forEach { if (it != VARIATION_SELECTOR) appendCodePoint(it) }
            }

        private fun url(
            left: String,
            right: String,
            date: Int,
        ): String {
            val l = left.replace("-", "-u")
            val r = right.replace("-", "-u")
            return "$IMAGE_HOST$date/u$l/u${l}_u$r.png"
        }
    }
}
