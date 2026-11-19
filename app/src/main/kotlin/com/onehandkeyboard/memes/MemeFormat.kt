package com.onehandkeyboard.memes

import android.content.ClipDescription
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import java.io.File
import java.io.IOException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The form a static meme takes for the field it goes to (GIFs and animated WebP are sent as
 * they are):
 *
 * - [WHATSAPP_STICKER]: the field takes `image/webp.wasticker` (WhatsApp) → 512×512 WebP sticker.
 * - [WEBP_STICKER]: the field names `image/webp` (Telegram) → the same 512×512 WebP.
 * - [SQUARE_JPEG]: any other field that takes JPEG (Instagram…) → square JPEG.
 * - [ORIGINAL]: the file as it is (GIFs, or a field that takes none of the above).
 *
 * Every converted meme is made 1:1 with black bars on the short side.
 */
enum class MemeFormat(
    val mimeType: String?,
    val extension: String,
    /** Side of the square in px, or 0 to keep the image's own long side (capped). */
    val side: Int,
    val maxBytes: Long,
) {
    WHATSAPP_STICKER("image/webp.wasticker", "webp", STICKER_SIDE, WHATSAPP_MAX_BYTES),
    WEBP_STICKER("image/webp", "webp", STICKER_SIDE, WHATSAPP_MAX_BYTES),
    SQUARE_JPEG("image/jpeg", "jpg", 0, Long.MAX_VALUE),
    ORIGINAL(null, "", 0, Long.MAX_VALUE),
    ;

    companion object {
        /** Picks the format for a meme of [memeMimeType] sent to a field taking [fieldTypes]. */
        fun choose(
            memeMimeType: String,
            fieldTypes: Array<String>,
        ): MemeFormat =
            when {
                memeMimeType !in STATIC_TYPES -> ORIGINAL
                WHATSAPP_STICKER.mimeType in fieldTypes -> WHATSAPP_STICKER
                fieldTypes.any { it.equals("image/webp", ignoreCase = true) } -> WEBP_STICKER
                fieldTypes.any { ClipDescription.compareMimeTypes("image/jpeg", it) } -> SQUARE_JPEG
                else -> ORIGINAL
            }

        /**
         * Where an image of [width]×[height] goes inside a [side]×[side] square: scaled to fit,
         * centered, so the bars fall on the short side.
         */
        fun fit(
            width: Int,
            height: Int,
            side: Int,
        ): Rect {
            val scale = side.toFloat() / max(width, height)
            val w = (width * scale).roundToInt().coerceIn(1, side)
            val h = (height * scale).roundToInt().coerceIn(1, side)
            val left = (side - w) / 2
            val top = (side - h) / 2
            return Rect(left, top, left + w, top + h)
        }

        /** True for an animated WebP (VP8X header with the animation flag): sent as it is. */
        fun isAnimatedWebp(file: File): Boolean {
            val header = ByteArray(WEBP_HEADER_BYTES)
            val read =
                try {
                    file.inputStream().use { it.read(header) }
                } catch (_: IOException) {
                    0
                }
            return read == WEBP_HEADER_BYTES &&
                String(header, 0, FOURCC, Charsets.US_ASCII) == "RIFF" &&
                String(header, VP8X_OFFSET, FOURCC, Charsets.US_ASCII) == "VP8X" &&
                header[VP8X_FLAGS_OFFSET].toInt() and ANIMATION_FLAG != 0
        }

        /** MIME type from the file's first bytes (GIF, PNG, JPEG or WebP), or null. */
        fun sniffMimeType(file: File): String? {
            val header = ByteArray(WEBP_HEADER_BYTES)
            val read =
                try {
                    file.inputStream().use { it.read(header) }
                } catch (_: IOException) {
                    0
                }
            val start = header.copyOf(read.coerceAtLeast(0))
            return SIGNATURES.entries.firstOrNull { (signature, _) -> signature.matches(start) }?.value
        }

        /** Leading bytes of each format; `null` bytes may be anything (the RIFF size of a WebP). */
        private class Signature(
            vararg val bytes: Int?,
        ) {
            fun matches(start: ByteArray) =
                start.size >= bytes.size && bytes.withIndex().all { (i, b) -> b == null || start[i] == b.toByte() }
        }

        @Suppress("MagicNumber") // the formats' published magic numbers
        private val SIGNATURES =
            mapOf(
                Signature(0x47, 0x49, 0x46) to "image/gif",
                Signature(0x89, 0x50, 0x4E, 0x47) to "image/png",
                Signature(0xFF, 0xD8, 0xFF) to "image/jpeg",
                Signature(0x52, 0x49, 0x46, 0x46, null, null, null, null, 0x57, 0x45, 0x42, 0x50) to "image/webp",
            )

        private val STATIC_TYPES = setOf("image/png", "image/jpeg", "image/webp")
        private const val WEBP_HEADER_BYTES = 21
        private const val FOURCC = 4
        private const val VP8X_OFFSET = 12
        private const val VP8X_FLAGS_OFFSET = 20
        private const val ANIMATION_FLAG = 0x02
    }
}

private const val STICKER_SIDE = 512
private const val WHATSAPP_MAX_BYTES = 100L * 1024

/** Draws memes into the square [MemeFormat]s. Blocking: call it off the main thread. */
internal object MemeConverter {
    private const val MAX_SQUARE_SIDE = 1080
    private const val START_QUALITY = 90
    private const val MIN_QUALITY = 30
    private const val QUALITY_STEP = 10

    /** Writes [source] as [format] to [dest]; false if the image cannot be read. */
    fun convert(
        source: File,
        format: MemeFormat,
        dest: File,
    ): Boolean {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.path, bounds)
        val longSide = max(bounds.outWidth, bounds.outHeight)
        val side = if (format.side > 0) format.side else min(longSide, MAX_SQUARE_SIDE)
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(longSide, side) }
        val image = if (longSide > 0) BitmapFactory.decodeFile(source.path, options) else null
        return image != null && draw(image, side, format, dest)
    }

    private fun draw(
        image: Bitmap,
        side: Int,
        format: MemeFormat,
        dest: File,
    ): Boolean {
        val square = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        Canvas(square).apply {
            drawColor(Color.BLACK)
            drawBitmap(image, null, MemeFormat.fit(image.width, image.height, side), Paint(Paint.FILTER_BITMAP_FLAG))
        }
        image.recycle()
        val written = write(square, format, dest)
        square.recycle()
        return written
    }

    /** Lowers the quality until the file fits [MemeFormat.maxBytes] (WhatsApp's sticker limit). */
    private fun write(
        bitmap: Bitmap,
        format: MemeFormat,
        dest: File,
    ): Boolean {
        dest.parentFile?.mkdirs()
        var quality = START_QUALITY
        while (true) {
            try {
                dest.outputStream().use { bitmap.compress(compressFormat(format), quality, it) }
            } catch (_: IOException) {
                dest.delete()
                return false
            }
            if (dest.length() <= format.maxBytes || quality <= MIN_QUALITY) return dest.length() > 0
            quality -= QUALITY_STEP
        }
    }

    private fun compressFormat(format: MemeFormat): Bitmap.CompressFormat =
        when {
            format == MemeFormat.SQUARE_JPEG -> Bitmap.CompressFormat.JPEG
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Bitmap.CompressFormat.WEBP_LOSSY
            else ->
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
        }

    /** Largest power of two that still decodes the image at least [side] px long. */
    private fun sampleSize(
        longSide: Int,
        side: Int,
    ): Int {
        var sample = 1
        while (longSide / (sample * 2) >= side) sample *= 2
        return sample
    }
}
