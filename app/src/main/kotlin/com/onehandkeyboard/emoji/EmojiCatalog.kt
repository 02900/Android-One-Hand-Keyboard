package com.onehandkeyboard.emoji

/** A section of the emoji panel: its tab [icon], its title and its emojis. */
data class EmojiGroup(
    val icon: String,
    val name: String,
    val emojis: List<String>,
)

/**
 * The emojis of the panel, from assets/emoji/emoji.tsv (tools/build_emoji.py): one line per
 * group, "<icon>\t<name>\t<emoji> <emoji>…".
 */
object EmojiCatalog {
    const val RECENT_ICON = "🕘"
    const val RECENT_NAME = "Recientes"
    const val MAX_RECENTS = 32
    private const val FIELDS = 3

    /**
     * The groups of [lines], keeping only the emojis [canShow] (the phone's font may not have the
     * newest ones). Groups left empty are dropped.
     */
    fun parse(
        lines: Sequence<String>,
        canShow: (String) -> Boolean = { true },
    ): List<EmojiGroup> =
        lines
            .mapNotNull { line ->
                val fields = line.trimEnd('\r').split('\t') // a Windows checkout may add \r
                if (fields.size < FIELDS) return@mapNotNull null
                val emojis = fields[2].split(' ').filter { it.isNotEmpty() && canShow(it) }
                EmojiGroup(fields[0], fields[1], emojis).takeIf { emojis.isNotEmpty() }
            }.toList()

    /** [groups] with the recently used emojis first, when there are any. */
    fun withRecents(
        groups: List<EmojiGroup>,
        recents: List<String>,
    ): List<EmojiGroup> =
        if (recents.isEmpty()) groups else listOf(EmojiGroup(RECENT_ICON, RECENT_NAME, recents)) + groups

    /** [recents] with [emoji] moved to the front, at most [MAX_RECENTS]. */
    fun pushRecent(
        recents: List<String>,
        emoji: String,
    ): List<String> = (listOf(emoji) + recents.filter { it != emoji }).take(MAX_RECENTS)
}
