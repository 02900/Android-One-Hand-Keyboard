package com.onehandkeyboard.memes

/**
 * One image of the library. [uri] is the document URI inside the folder the user picked, or
 * the image's https address for a meme found with the search (downloaded when sent).
 */
data class Meme(
    val uri: String,
    val fileName: String,
    val mimeType: String,
    /** What the panel shows: the image itself, or a small preview of a remote one. */
    val previewUri: String = uri,
) {
    /** File name without extension, used as accessibility text. */
    val title: String get() = fileName.substringBeforeLast('.').ifEmpty { fileName }

    /** True for a meme from the search that still has to be downloaded. */
    val isRemote: Boolean get() = uri.startsWith("https://")
}

/** A tab of the panel: one sub-folder of the library (or the loose files, or the recents). */
data class MemeCategory(
    val name: String,
    val memes: List<Meme>,
)

/** A file found while scanning the library folder. [folder] is null for files at its root. */
data class ScannedFile(
    val uri: String,
    val fileName: String,
    val mimeType: String?,
    val folder: String? = null,
)

/**
 * Turns the files of the user's meme folder into the tabs of the panel.
 *
 * - Each sub-folder is a category; files directly in the folder go to [LOOSE_CATEGORY].
 * - Folders and files are sorted by name, so a numeric prefix sets the order; the prefix
 *   (`01-`, `02_`, `3 `) is not shown.
 * - Only images Android apps accept as stickers are kept: GIF, WebP, PNG and JPEG. Hidden
 *   files (`.gitkeep`, `.nomedia`) and empty folders are skipped.
 */
object MemeCatalog {
    const val LOOSE_CATEGORY = "General"
    const val RECENT_CATEGORY = "🕘 Recientes"
    const val MAX_RECENTS = 24

    private val TYPES_BY_EXTENSION =
        mapOf(
            "gif" to "image/gif",
            "webp" to "image/webp",
            "png" to "image/png",
            "jpg" to "image/jpeg",
            "jpeg" to "image/jpeg",
        )

    private val ORDER_PREFIX = Regex("""^\d+\s*[-_.\s]\s*""")

    /** Supported MIME type of a file, or null if the panel cannot show it. */
    fun mimeTypeOf(
        fileName: String,
        providerMimeType: String?,
    ): String? {
        if (fileName.startsWith('.')) return null
        val byExtension = TYPES_BY_EXTENSION[fileName.substringAfterLast('.', "").lowercase()]
        val byProvider = providerMimeType?.lowercase()?.takeIf { it in TYPES_BY_EXTENSION.values }
        return byProvider ?: byExtension
    }

    /** Folder name as shown in the tab: without the ordering prefix. */
    fun displayName(folder: String): String = folder.replaceFirst(ORDER_PREFIX, "").trim().ifEmpty { folder }

    fun build(files: List<ScannedFile>): List<MemeCategory> {
        val byFolder = LinkedHashMap<String?, MutableList<Meme>>()
        files.forEach { file ->
            if (file.folder?.startsWith('.') == true) return@forEach
            val type = mimeTypeOf(file.fileName, file.mimeType) ?: return@forEach
            byFolder.getOrPut(file.folder) { ArrayList() }.add(Meme(file.uri, file.fileName, type))
        }
        val loose = byFolder.remove(null)
        val folders =
            byFolder.entries
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.key.orEmpty() })
                .map { (folder, memes) -> MemeCategory(displayName(folder.orEmpty()), memes.sortedByName()) }
        return if (loose.isNullOrEmpty()) folders else folders + MemeCategory(LOOSE_CATEGORY, loose.sortedByName())
    }

    /**
     * Tabs with the recently sent memes first. Recents that are no longer in the library
     * (deleted or renamed) are dropped.
     */
    fun withRecents(
        categories: List<MemeCategory>,
        recentUris: List<String>,
    ): List<MemeCategory> {
        val byUri = categories.flatMap { it.memes }.associateBy { it.uri }
        val recents = recentUris.mapNotNull(byUri::get).take(MAX_RECENTS)
        return if (recents.isEmpty()) categories else listOf(MemeCategory(RECENT_CATEGORY, recents)) + categories
    }

    /** Recents list after sending [uri]: moved to the front, without duplicates, capped. */
    fun pushRecent(
        recentUris: List<String>,
        uri: String,
    ): List<String> = (listOf(uri) + recentUris.filter { it != uri }).take(MAX_RECENTS)

    private fun List<Meme>.sortedByName() = sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.fileName })
}
