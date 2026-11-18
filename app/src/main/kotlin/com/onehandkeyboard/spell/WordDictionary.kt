package com.onehandkeyboard.spell

import java.text.Normalizer

/**
 * Read-only word list with frequencies, searchable by typo distance.
 *
 * Words are indexed by their folded form (lowercase, no accents, no apostrophes), so all
 * spellings that differ only in accents ("esta", "está") share one key. Keys are kept in a
 * sorted array instead of a node trie: the fuzzy search walks them in order and reuses the
 * edit-distance rows of the shared prefix, which gives trie speed with a few MB of memory.
 */
class WordDictionary private constructor(
    private val keys: Array<String>,
    private val forms: Array<Array<String>>,
    private val frequencies: Array<LongArray>,
) {
    data class Entry(
        val word: String,
        val frequency: Long,
        /** Edit cost from the typed word (0 for exact or accent-only matches). */
        val cost: Float = 0f,
    )

    val size: Int get() = keys.size

    private val maxKeyLength = keys.maxOfOrNull { it.length } ?: 0

    /** True if [word] is in the list with exactly these letters and accents (case-insensitive). */
    fun isWord(word: String): Boolean {
        val index = indexOf(fold(word))
        return index >= 0 && forms[index].any { it.equals(word, ignoreCase = true) }
    }

    /** Every spelling that folds to the same key as [word] ("esta" → está, esta), most frequent first. */
    fun variants(word: String): List<Entry> {
        val index = indexOf(fold(word))
        if (index < 0) return emptyList()
        return forms[index].indices.map { Entry(forms[index][it], frequencies[index][it]) }
    }

    /** Most frequent words that start with [prefix] (folded) and are longer than it. */
    fun completions(
        prefix: String,
        limit: Int,
    ): List<Entry> {
        val key = fold(prefix)
        if (key.isEmpty()) return emptyList()
        val start = lowerBound(key)
        val end = firstWithoutPrefix(key, start)
        val best = ArrayList<Entry>()
        for (i in start until end) {
            if (keys[i].length == key.length) continue
            best += Entry(forms[i][0], frequencies[i][0], cost = 0f)
        }
        return best.sortedByDescending { it.frequency }.take(limit)
    }

    /**
     * Words within [maxCost] of [typed] (weighted Damerau-Levenshtein on folded forms, so a
     * missing accent costs nothing). Accent-only matches are not returned: use [variants].
     */
    fun search(
        typed: String,
        maxCost: Float,
        model: ErrorModel,
    ): List<Entry> {
        val target = fold(typed)
        val n = target.length
        if (n == 0 || keys.isEmpty()) return emptyList()
        val rows = Array(maxKeyLength + 1) { FloatArray(n + 1) }
        for (j in 0..n) rows[0][j] = j.toFloat()

        val results = ArrayList<Entry>()
        var previous = ""
        var validDepth = 0
        var i = 0
        while (i < keys.size) {
            val key = keys[i]
            val prunedAt = walk(rows, key, minOf(commonPrefix(previous, key), validDepth) + 1, target, maxCost, model)
            previous = key
            if (prunedAt > 0) {
                // No word under this prefix can get back below maxCost.
                validDepth = prunedAt - 1
                i = firstWithoutPrefix(key.substring(0, prunedAt), i)
            } else {
                validDepth = key.length
                val cost = rows[key.length][n]
                if (cost <= maxCost && key != target) addForms(i, cost, results)
                i++
            }
        }
        return results
    }

    /**
     * Fills the rows of [key] from [fromDepth]; returns the depth where every cost went over
     * [maxCost] (so the whole prefix can be skipped), or -1 if the key was completed.
     */
    @Suppress("LongParameterList")
    private fun walk(
        rows: Array<FloatArray>,
        key: String,
        fromDepth: Int,
        target: String,
        maxCost: Float,
        model: ErrorModel,
    ): Int {
        for (depth in fromDepth..key.length) {
            if (fillRow(rows, depth, key, target, model) > maxCost) return depth
        }
        return -1
    }

    /** Computes row [depth] of the edit-distance table for [key]; returns the row minimum. */
    private fun fillRow(
        rows: Array<FloatArray>,
        depth: Int,
        key: String,
        target: String,
        model: ErrorModel,
    ): Float {
        val c = key[depth - 1]
        val row = rows[depth]
        val up = rows[depth - 1]
        row[0] = depth.toFloat()
        var rowMin = row[0]
        for (j in 1..target.length) {
            val typedChar = target[j - 1]
            var value =
                minOf(
                    up[j - 1] + model.substitution(typedChar, c),
                    up[j] + 1f,
                    row[j - 1] + 1f,
                )
            val transposed = depth > 1 && j > 1 && typedChar == key[depth - 2] && target[j - 2] == c
            if (transposed) value = minOf(value, rows[depth - 2][j - 2] + 1f)
            row[j] = value
            if (value < rowMin) rowMin = value
        }
        return rowMin
    }

    private fun addForms(
        index: Int,
        cost: Float,
        out: MutableList<Entry>,
    ) {
        forms[index].indices.forEach { out += Entry(forms[index][it], frequencies[index][it], cost) }
    }

    private fun commonPrefix(
        a: String,
        b: String,
    ): Int {
        val limit = minOf(a.length, b.length)
        var i = 0
        while (i < limit && a[i] == b[i]) i++
        return i
    }

    private fun indexOf(key: String): Int {
        val index = keys.binarySearch(key)
        return if (index >= 0) index else -1
    }

    private fun lowerBound(key: String): Int {
        val index = keys.binarySearch(key)
        return if (index >= 0) index else -index - 1
    }

    /** First index at or after [from] whose key does not start with [prefix] (keys are sorted). */
    private fun firstWithoutPrefix(
        prefix: String,
        from: Int,
    ): Int {
        var low = from
        var high = keys.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (keys[mid].startsWith(prefix)) low = mid + 1 else high = mid
        }
        return low
    }

    companion object {
        private val COMBINING_MARKS = Regex("\\p{Mn}+")

        /** Lowercase, accents and apostrophes removed; ñ stays a letter of its own. */
        fun fold(word: String): String {
            val lower = word.lowercase().replace("'", "").replace('ñ', '\u0000')
            val stripped = COMBINING_MARKS.replace(Normalizer.normalize(lower, Normalizer.Form.NFD), "")
            return stripped.replace('\u0000', 'ñ')
        }

        /** Parses "word<TAB>frequency" lines (blank and malformed lines are skipped). */
        fun parse(lines: Sequence<String>): WordDictionary =
            of(
                lines.mapNotNull { line ->
                    val tab = line.indexOf('\t')
                    if (tab <= 0) return@mapNotNull null
                    val frequency = line.substring(tab + 1).trim().toLongOrNull() ?: return@mapNotNull null
                    line.substring(0, tab) to frequency
                }.asIterable(),
            )

        fun of(words: Iterable<Pair<String, Long>>): WordDictionary {
            val grouped = sortedMapOf<String, MutableList<Pair<String, Long>>>()
            words.forEach { (word, frequency) ->
                val key = fold(word)
                if (key.isNotEmpty()) grouped.getOrPut(key) { mutableListOf() } += word to frequency
            }
            val keys = grouped.keys.toTypedArray()
            val sortedGroups = grouped.values.map { group -> group.sortedByDescending { it.second } }
            return WordDictionary(
                keys = keys,
                forms = sortedGroups.map { group -> group.map { it.first }.toTypedArray() }.toTypedArray(),
                frequencies = sortedGroups.map { group -> group.map { it.second }.toLongArray() }.toTypedArray(),
            )
        }
    }
}
