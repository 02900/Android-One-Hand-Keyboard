package com.onehandkeyboard.memes

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document

/**
 * Reads the meme folder the user picked in the settings (a Storage Access Framework tree).
 *
 * The keyboard has no storage permission: it can only read the folder the user granted, and
 * only while that grant lasts. Nothing is copied until a meme is sent.
 */
class MemeLibrary(
    private val context: Context,
) {
    sealed interface Result {
        /** No folder picked yet. */
        data object NotConfigured : Result

        /** The folder was moved, deleted or its permission revoked. */
        data object NoAccess : Result

        data class Loaded(
            val categories: List<MemeCategory>,
        ) : Result
    }

    /** Scans the folder. Blocking: call it off the main thread. */
    fun load(folderUri: String?): Result {
        folderUri ?: return Result.NotConfigured
        return try {
            val tree = Uri.parse(folderUri)
            val files = ArrayList<ScannedFile>()
            scan(context.contentResolver, tree, DocumentsContract.getTreeDocumentId(tree), null, 0, files)
            Result.Loaded(MemeCatalog.build(files))
        } catch (_: SecurityException) {
            Result.NoAccess
        } catch (_: IllegalArgumentException) {
            Result.NoAccess
        }
    }

    /** Name of the picked folder, for the settings screen. */
    fun folderName(folderUri: String): String? =
        try {
            val tree = Uri.parse(folderUri)
            val document = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            context.contentResolver.query(document, arrayOf(Document.COLUMN_DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        } catch (_: SecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }

    /**
     * Adds the images under [documentId] to [out]. Files inside nested folders belong to the
     * top-level folder ([category]) they are in.
     */
    @Suppress("LongParameterList")
    private fun scan(
        resolver: ContentResolver,
        tree: Uri,
        documentId: String,
        category: String?,
        depth: Int,
        out: MutableList<ScannedFile>,
    ) {
        val (folders, files) = children(resolver, tree, documentId).partition { it.type == Document.MIME_TYPE_DIR }
        files.take(MAX_FILES - out.size).forEach { child ->
            val uri = DocumentsContract.buildDocumentUriUsingTree(tree, child.id).toString()
            out.add(ScannedFile(uri, child.name, child.type, category))
        }
        if (depth >= MAX_DEPTH) return
        folders
            .filterNot { it.name.startsWith('.') }
            .forEach { scan(resolver, tree, it.id, category ?: it.name, depth + 1, out) }
    }

    private data class Child(
        val id: String,
        val name: String,
        val type: String?,
    )

    private fun children(
        resolver: ContentResolver,
        tree: Uri,
        documentId: String,
    ): List<Child> {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, documentId)
        val children = ArrayList<Child>()
        resolver.query(uri, COLUMNS, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                val name = cursor.getString(1)
                if (id != null && name != null) children.add(Child(id, name, cursor.getString(2)))
            }
        }
        return children
    }

    private companion object {
        val COLUMNS = arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE)
        const val MAX_DEPTH = 3
        const val MAX_FILES = 2000
    }
}
