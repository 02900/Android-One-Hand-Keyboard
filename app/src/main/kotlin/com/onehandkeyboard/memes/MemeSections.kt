package com.onehandkeyboard.memes

/** A row of the meme panel's list: a category title or one meme of it. */
sealed interface MemeRow {
    /** Index of the category the row belongs to. */
    val section: Int

    data class Header(
        val name: String,
        override val section: Int,
    ) : MemeRow

    data class Item(
        val meme: Meme,
        override val section: Int,
    ) : MemeRow
}

/**
 * All the categories of the panel in one vertical list: each title followed by its memes.
 * Empty categories are left out.
 */
class MemeSections(
    categories: List<MemeCategory>,
) {
    val sectionNames: List<String>
    val rows: List<MemeRow>

    init {
        val shown = categories.filter { it.memes.isNotEmpty() }
        sectionNames = shown.map { it.name }
        rows =
            shown.flatMapIndexed { section, category ->
                listOf(MemeRow.Header(category.name, section)) + category.memes.map { MemeRow.Item(it, section) }
            }
    }

    val size: Int get() = rows.size

    fun isEmpty() = rows.isEmpty()

    /** Category of the row at [position] (clamped to the list). */
    fun sectionAt(position: Int): Int = rows.getOrNull(position.coerceIn(0, (size - 1).coerceAtLeast(0)))?.section ?: 0

    /** Position of the title of [section]. */
    fun headerPosition(section: Int): Int = rows.indexOfFirst { it is MemeRow.Header && it.section == section }

    /**
     * Stable identity of the row at [position], used to keep the scroll where it was when the
     * list changes (e.g. more search results arrive).
     */
    fun keyAt(position: Int): String? =
        when (val row = rows.getOrNull(position)) {
            is MemeRow.Header -> "#${row.name}"
            is MemeRow.Item -> "${sectionNames[row.section]}/${row.meme.uri}"
            null -> null
        }

    fun positionOfKey(key: String?): Int = if (key == null) -1 else rows.indices.firstOrNull { keyAt(it) == key } ?: -1

    companion object {
        /** Row to jump to when the fast-scroll thumb is at [fraction] (0 = top, 1 = bottom). */
        fun positionForFraction(
            fraction: Float,
            count: Int,
        ): Int = if (count <= 0) 0 else (fraction.coerceIn(0f, 1f) * (count - 1)).toInt()
    }
}
