package com.onehandkeyboard.memes

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.core.content.FileProvider
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import java.io.File
import java.io.IOException

/**
 * Sends a meme to the app being typed in, the way image keyboards do (`commitContent`).
 *
 * The picked folder is only readable by this app, so the image is first copied to the
 * app's cache and shared through its own [FileProvider], with a read grant for the
 * receiving app only. Static images become a square sticker or JPEG when the field takes one
 * (see [MemeFormat]). Apps that do not accept images get it in the clipboard instead.
 */
class MemeSender(
    private val context: Context,
    /** Shareable URI of a cached copy (the app's FileProvider). */
    private val shareUri: (File) -> Uri = { FileProvider.getUriForFile(context, authority(context), it) },
) {
    enum class Outcome { SENT, COPIED, FAILED }

    /** A meme ready to share: the cached [file] and the MIME type it is sent as. */
    data class Prepared(
        val file: File,
        val mimeType: String,
    )

    private val cacheRoot get() = File(context.cacheDir, CACHE_DIR)

    /**
     * Copies the meme to the shareable cache and, for a field taking [fieldTypes], converts it
     * to the [MemeFormat] that field prefers. Blocking: call it off the main thread.
     */
    fun prepare(
        meme: Meme,
        fieldTypes: Array<String> = emptyArray(),
    ): Prepared? {
        cleanCache()
        val folder = File(cacheRoot, Integer.toHexString(meme.uri.hashCode()))
        val file = File(folder, safeFileName(meme.fileName))
        val copied = isCached(file) || copy(meme, file)
        return if (copied) convert(meme, Prepared(file, meme.mimeType), fieldTypes) else null
    }

    /** The [original] in the field's [MemeFormat], or the original if it stays as it is. */
    private fun convert(
        meme: Meme,
        original: Prepared,
        fieldTypes: Array<String>,
    ): Prepared {
        val format = MemeFormat.choose(meme.mimeType, fieldTypes)
        if (format == MemeFormat.ORIGINAL || MemeFormat.isAnimatedWebp(original.file)) return original
        val name = "${safeFileName(meme.title)}.${format.name.lowercase()}.${format.extension}"
        val converted = File(original.file.parentFile, name)
        val ready = isCached(converted) || MemeConverter.convert(original.file, format, converted)
        return if (ready) Prepared(converted, checkNotNull(format.mimeType)) else original
    }

    private fun isCached(file: File) =
        file.exists() && file.length() > 0 && file.setLastModified(System.currentTimeMillis())

    private fun copy(
        meme: Meme,
        file: File,
    ): Boolean =
        try {
            file.parentFile?.mkdirs()
            val input = context.contentResolver.openInputStream(Uri.parse(meme.uri))
            input?.use { source -> file.outputStream().use { source.copyTo(it) } } != null
        } catch (_: IOException) {
            file.delete()
            false
        } catch (_: SecurityException) {
            false
        }

    /** Inserts the [prepared] meme in the field, or copies it to the clipboard. Main thread. */
    fun commit(
        meme: Meme,
        prepared: Prepared,
        ic: InputConnection?,
        info: EditorInfo?,
    ): Outcome {
        val uri =
            try {
                shareUri(prepared.file)
            } catch (_: IllegalArgumentException) {
                null
            }
        val description = ClipDescription(meme.title, arrayOf(prepared.mimeType))
        return when {
            uri == null -> Outcome.FAILED
            commitContent(uri, description, ic, info) -> Outcome.SENT
            copyToClipboard(uri, description) -> Outcome.COPIED
            else -> Outcome.FAILED
        }
    }

    private fun commitContent(
        uri: Uri,
        description: ClipDescription,
        ic: InputConnection?,
        info: EditorInfo?,
    ): Boolean {
        val accepted = info != null && accepts(EditorInfoCompat.getContentMimeTypes(info), description.getMimeType(0))
        if (ic == null || info == null || !accepted) return false
        val content = InputContentInfoCompat(uri, description, null)
        val flags = InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
        return InputConnectionCompat.commitContent(ic, info, content, flags, null)
    }

    private fun copyToClipboard(
        uri: Uri,
        description: ClipDescription,
    ): Boolean {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData(description, ClipData.Item(uri)))
        return clipboard != null
    }

    /** Drops copies not used for a day (the receiving app has long finished reading them). */
    private fun cleanCache() {
        val limit = System.currentTimeMillis() - CACHE_TTL_MS
        cacheRoot.listFiles()?.forEach { dir ->
            if (dir.listFiles()?.all { it.lastModified() < limit } != false) dir.deleteRecursively()
        }
    }

    companion object {
        private const val CACHE_DIR = "memes"
        private const val CACHE_TTL_MS = 24L * 60 * 60 * 1000

        fun authority(context: Context) = "${context.packageName}.memes"

        /** True if the field accepts [mimeType] (a field taking any image accepts a GIF). */
        fun accepts(
            fieldTypes: Array<String>,
            mimeType: String,
        ): Boolean = fieldTypes.any { ClipDescription.compareMimeTypes(mimeType, it) }

        internal fun safeFileName(name: String): String =
            name.replace(Regex("""[\\/:*?"<>|\u0000-\u001f]"""), "_").trim().ifEmpty { "meme" }
    }
}
