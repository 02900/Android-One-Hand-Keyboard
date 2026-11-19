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
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Decodes the panel's previews off the main thread, scaled down to the cell size.
 *
 * GIFs and animated WebPs play on Android 9+ ([ImageDecoder]); older versions show the
 * first frame. A small cache keeps the previews of the tab being browsed; [clear] drops
 * everything when the panel closes.
 */
class MemeThumbnails(
    private val context: Context,
    private val main: Executor,
) {
    private val workers: ExecutorService = Executors.newFixedThreadPool(THREADS)
    private val cache = LruCache<String, Drawable>(CACHE_ENTRIES)

    /** Bumped by [clear] so that decodes started before it are dropped. */
    @Volatile private var generation = 0

    fun load(
        meme: Meme,
        sizePx: Int,
        target: ImageView,
    ) {
        target.tag = meme.uri
        val cached = cache.get(meme.uri)
        target.setImageDrawable(cached)
        if (cached != null) {
            (cached as? Animatable)?.start()
            return
        }
        val started = generation
        workers.execute {
            val drawable = decode(Uri.parse(meme.previewUri), sizePx) ?: return@execute
            main.execute {
                if (started != generation) return@execute
                cache.put(meme.uri, drawable)
                if (target.tag == meme.uri) {
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
        uri: Uri,
        sizePx: Int,
    ): Drawable? =
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                decodeAnimated(uri, sizePx)
            } else {
                decodeFirstFrame(uri, sizePx)
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
        uri: Uri,
        sizePx: Int,
    ): Drawable {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
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
        uri: Uri,
        sizePx: Int,
    ): Drawable? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= sizePx) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
        return BitmapDrawable(context.resources, bitmap)
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
        private const val THREADS = 2
        private const val CACHE_ENTRIES = 36

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
