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
 * receiving app only. Apps that do not accept images get it in the clipboard instead.
 */
class MemeSender(
    private val context: Context,
    /** Shareable URI of a cached copy (the app's FileProvider). */
    private val shareUri: (File) -> Uri = { FileProvider.getUriForFile(context, authority(context), it) },
) {
    enum class Outcome { SENT, COPIED, FAILED }

    private val cacheRoot get() = File(context.cacheDir, CACHE_DIR)

    /** Copies the meme to the shareable cache. Blocking: call it off the main thread. */
    fun prepare(meme: Meme): File? {
        cleanCache()
        val file = File(File(cacheRoot, Integer.toHexString(meme.uri.hashCode())), safeFileName(meme.fileName))
        val cached = file.exists() && file.length() > 0 && file.setLastModified(System.currentTimeMillis())
        val ready = cached || copy(meme, file)
        return file.takeIf { ready }
    }

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

    /** Inserts the prepared [file] in the field, or copies it to the clipboard. Main thread. */
    fun commit(
        meme: Meme,
        file: File,
        ic: InputConnection?,
        info: EditorInfo?,
    ): Outcome {
        val uri =
            try {
                shareUri(file)
            } catch (_: IllegalArgumentException) {
                null
            }
        val description = ClipDescription(meme.title, arrayOf(meme.mimeType))
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
