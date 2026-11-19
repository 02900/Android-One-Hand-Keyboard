package com.onehandkeyboard.memes

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.widget.ImageView
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Decodes the panel's previews off the main thread, scaled down to the cell size.
 *
 * GIFs and animated WebPs play on Android 9+ ([ImageDecoder]); older versions show the
 * first frame. A small cache keeps the previews of the tab being browsed; [clear] drops
 * everything when the panel closes. Previews on the web (search results) are fetched with
 * [fetch] as they scroll into view.
 */
class MemeThumbnails(
    private val context: Context,
    private val main: Executor,
    /** Downloads an https preview; null in builds without Internet. */
    private val fetch: ((String) -> ByteArray?)? = null,
) {
    private val workers: ExecutorService = Executors.newFixedThreadPool(THREADS)

    /** Previews by [cacheKey], limited by their size in memory (the big preview is large). */
    private val cache =
        object : LruCache<String, Drawable>(CACHE_BYTES) {
            override fun sizeOf(
                key: String,
                value: Drawable,
            ) = (value.intrinsicWidth.coerceAtLeast(1) * value.intrinsicHeight.coerceAtLeast(1) * BYTES_PER_PIXEL)
        }

    /** Bumped by [clear] so that decodes started before it are dropped. */
    @Volatile private var generation = 0

    fun load(
        meme: Meme,
        sizePx: Int,
        target: ImageView,
    ) {
        val key = cacheKey(meme, sizePx)
        target.tag = key
        val cached = cache.get(key)
        target.setImageDrawable(cached)
        if (cached != null) {
            (cached as? Animatable)?.start()
            return
        }
        val started = generation
        workers.execute {
            val drawable = decode(meme.previewUri, sizePx) ?: return@execute
            main.execute {
                if (started != generation) return@execute
                cache.put(key, drawable)
                if (target.tag == key) {
                    target.setImageDrawable(drawable)
                    (drawable as? Animatable)?.start()
                }
            }
        }
    }

    fun clear() {
        generation++
        cache.evictAll()
    }

    fun shutdown() {
        clear()
        workers.shutdownNow()
    }

    private fun decode(
        uri: String,
        sizePx: Int,
    ): Drawable? =
        try {
            val bytes = if (uri.startsWith("https://")) fetch?.invoke(uri) ?: return null else null
            val source = bytes?.let(ImageSource::Bytes) ?: ImageSource.Content(Uri.parse(uri))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                decodeAnimated(source, sizePx)
            } else {
                decodeFirstFrame(source, sizePx)
            }
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: OutOfMemoryError) {
            null
        }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.P)
    private fun decodeAnimated(
        image: ImageSource,
        sizePx: Int,
    ): Drawable {
        val source =
            when (image) {
                is ImageSource.Bytes -> ImageDecoder.createSource(ByteBuffer.wrap(image.bytes))
                is ImageSource.Content -> ImageDecoder.createSource(context.contentResolver, image.uri)
            }
        val drawable =
            ImageDecoder.decodeDrawable(source) { decoder, info, _ ->
                val scale = scaleFor(info.size.width, info.size.height, sizePx)
                if (scale < 1f) {
                    decoder.setTargetSize(
                        (info.size.width * scale).toInt().coerceAtLeast(1),
                        (info.size.height * scale).toInt().coerceAtLeast(1),
                    )
                }
            }
        return if (drawable is AnimatedImageDrawable) AnimatedPreview(drawable) else drawable
    }

    private fun decodeFirstFrame(
        image: ImageSource,
        sizePx: Int,
    ): Drawable? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        decodeStream(image, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= sizePx) sample *= 2
        val bitmap = decodeStream(image, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        return BitmapDrawable(context.resources, bitmap)
    }

    private fun decodeStream(
        image: ImageSource,
        options: BitmapFactory.Options,
    ) = when (image) {
        is ImageSource.Bytes -> BitmapFactory.decodeByteArray(image.bytes, 0, image.bytes.size, options)
        is ImageSource.Content ->
            context.contentResolver.openInputStream(image.uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    /** Where a preview's bytes come from: the phone (folder, cache) or the web (already downloaded). */
    private sealed interface ImageSource {
        class Content(
            val uri: Uri,
        ) : ImageSource

        class Bytes(
            val bytes: ByteArray,
        ) : ImageSource
    }

    /** Marks animated previews so they are (re)started when shown. */
    private interface Animatable {
        fun start()
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.P)
    private class AnimatedPreview(
        private val drawable: AnimatedImageDrawable,
    ) : android.graphics.drawable.DrawableWrapper(drawable),
        Animatable {
        override fun start() {
            if (!drawable.isRunning) drawable.start()
        }
    }

    companion object {
        private const val THREADS = 3
        private const val CACHE_BYTES = 32 * 1024 * 1024
        private const val BYTES_PER_PIXEL = 4

        /** A meme decoded at another size (the big preview) is another entry. */
        fun cacheKey(
            meme: Meme,
            sizePx: Int,
        ) = "${meme.uri}#$sizePx"

        /** Scale that fits a [width]×[height] image in a [sizePx] square (never above 1). */
        fun scaleFor(
            width: Int,
            height: Int,
            sizePx: Int,
        ): Float {
            val longest = maxOf(width, height)
            return if (longest <= 0 || longest <= sizePx) 1f else sizePx.toFloat() / longest
        }
    }
}
