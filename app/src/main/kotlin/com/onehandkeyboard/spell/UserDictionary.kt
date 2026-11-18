package com.onehandkeyboard.spell

import java.io.File

/**
 * Words the user taught the keyboard, so they are never "corrected" again.
 *
 * A word is learned when the user undoes its correction ([learn]) or after typing it, unknown
 * and uncorrected, [LEARN_AT] times ([recordUnknown]). Stored only in the app's private files,
 * as "word<TAB>count" lines; [persist] decides where the write runs (a background executor in
 * the app, inline in tests). Thread-safe: suggestions are computed off the main thread.
 */
class UserDictionary(
    private val file: File?,
    private val persist: (Runnable) -> Unit = { it.run() },
) {
    /** Insertion-ordered so the oldest pending (not yet learned) words are evicted first. */
    private val counts = LinkedHashMap<String, Int>()

    init {
        file?.takeIf { it.exists() }?.forEachLine { line ->
            val tab = line.indexOf('\t')
            val count = if (tab > 0) line.substring(tab + 1).toIntOrNull() else null
            if (tab > 0 && count != null) counts[line.substring(0, tab)] = count
        }
    }

    @Synchronized
    fun isLearned(word: String): Boolean = (counts[normalize(word)] ?: 0) >= LEARN_AT

    @Synchronized
    fun learnedWords(): List<String> = counts.filterValues { it >= LEARN_AT }.keys.toList()

    @Synchronized
    fun learn(word: String) {
        val key = normalize(word)
        if (key.isEmpty() || (counts[key] ?: 0) >= LEARN_AT) return
        counts[key] = LEARN_AT
        changed()
    }

    /** Counts one more use of an unknown word; returns true when this use makes it learned. */
    @Synchronized
    fun recordUnknown(word: String): Boolean {
        val key = normalize(word)
        val count = (counts[key] ?: 0) + 1
        val counted = key.isNotEmpty() && count <= LEARN_AT
        if (counted) {
            counts[key] = count
            changed()
        }
        return counted && count == LEARN_AT
    }

    @Synchronized
    fun clear() {
        counts.clear()
        changed()
    }

    private fun changed() {
        trim()
        val snapshot = counts.entries.joinToString("") { "${it.key}\t${it.value}\n" }
        val target = file ?: return
        persist(Runnable { target.writeText(snapshot) })
    }

    private fun trim() {
        if (counts.size <= MAX_WORDS) return
        val pending = counts.entries.filter { it.value < LEARN_AT }.map { it.key }
        val excess = counts.size - MAX_WORDS
        pending.take(excess).forEach(counts::remove)
        // Still too many learned words: forget the oldest ones.
        while (counts.size > MAX_WORDS) counts.remove(counts.keys.first())
    }

    private fun normalize(word: String) = word.trim().lowercase()

    companion object {
        const val LEARN_AT = 2
        const val MAX_WORDS = 5_000
    }
}
