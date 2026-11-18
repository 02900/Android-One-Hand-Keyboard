package com.onehandkeyboard.spell

import kotlin.math.ln

/** One entry of the suggestion strip. */
data class Suggestion(
    val text: String,
    val kind: Kind,
) {
    enum class Kind {
        /** The word exactly as typed: picking it keeps it (and teaches it to the keyboard). */
        TYPED,

        /** The correction that the next space or punctuation will apply. */
        AUTO,

        /** Another option (accent variant, completion or correction). */
        ALTERNATIVE,

        /** Shown right after a correction: picking it restores what the user typed. */
        REVERT,
    }
}

/** What the strip shows for the word being typed; [slots] are left, center, right (or empty). */
data class Suggestions(
    val typed: String,
    val autoCorrect: String?,
    val slots: List<Suggestion?>,
) {
    companion object {
        val EMPTY = Suggestions("", null, emptyList())

        fun revert(original: String) =
            Suggestions(
                "",
                null,
                listOf(null, Suggestion(original, Suggestion.Kind.REVERT), null),
            )
    }
}

/**
 * Ranks corrections and completions for a typed word and decides whether it is safe to
 * autocorrect it.
 *
 * Only unknown words are ever autocorrected (neither in the dictionary nor learned): a valid
 * word such as "esta" is never replaced by "está", it is only offered in the strip.
 */
class Suggester(
    private val dictionary: WordDictionary,
    private val userDictionary: UserDictionary?,
    private val model: ErrorModel,
) {
    fun isKnown(word: String): Boolean = dictionary.isWord(word) || userDictionary?.isLearned(word) == true

    fun suggest(
        typed: String,
        allowAutoCorrect: Boolean = true,
    ): Suggestions {
        if (typed.isEmpty()) return Suggestions.EMPTY
        val known = isKnown(typed)
        val corrections = rankedCorrections(typed)
        val autoCorrect = if (allowAutoCorrect && !known) pickAutoCorrect(typed, corrections) else null

        val lowerTyped = typed.lowercase()
        val alternatives =
            (corrections + completions(typed))
                .sortedByDescending { it.score }
                .map { matchCase(it.entry.word, typed) }
                .filter { it.lowercase() != lowerTyped && it != autoCorrect }
                .distinctBy { it.lowercase() }

        val slots =
            if (autoCorrect != null) {
                listOf(
                    Suggestion(typed, Suggestion.Kind.TYPED),
                    Suggestion(autoCorrect, Suggestion.Kind.AUTO),
                    alternatives.getOrNull(0)?.let { Suggestion(it, Suggestion.Kind.ALTERNATIVE) },
                )
            } else {
                listOf(
                    alternatives.getOrNull(0)?.let { Suggestion(it, Suggestion.Kind.ALTERNATIVE) },
                    Suggestion(typed, Suggestion.Kind.TYPED),
                    alternatives.getOrNull(1)?.let { Suggestion(it, Suggestion.Kind.ALTERNATIVE) },
                )
            }
        return Suggestions(typed, autoCorrect, slots)
    }

    private data class Scored(
        val entry: WordDictionary.Entry,
        val score: Double,
    )

    /** Accent variants (cost 0) and typo corrections, best first. */
    private fun rankedCorrections(typed: String): List<Scored> {
        val maxCost = if (typed.length <= SHORT_WORD) 1f else 2f
        val candidates = dictionary.variants(typed) + dictionary.search(typed, maxCost, model)
        return candidates
            .filter { !it.word.equals(typed, ignoreCase = true) }
            .map { Scored(it, score(it.frequency, it.cost)) }
            .sortedByDescending { it.score }
    }

    private fun completions(typed: String): List<Scored> {
        val fromDictionary =
            dictionary.completions(typed, COMPLETIONS).map {
                Scored(it, score(it.frequency, 0f) - COMPLETION_PENALTY)
            }
        val prefix = WordDictionary.fold(typed)
        val learned =
            userDictionary?.learnedWords().orEmpty()
                .filter { WordDictionary.fold(it).startsWith(prefix) }
                .map { Scored(WordDictionary.Entry(it, LEARNED_FREQUENCY), score(LEARNED_FREQUENCY, 0f)) }
        return fromDictionary + learned
    }

    /**
     * Fixing only accents is always safe. A typo fix needs a long enough word, a small edit,
     * and a clear winner over the second candidate. Words in the dictionary are never fixed,
     * even rare ones: correcting a valid word would be forcing the user.
     */
    private fun pickAutoCorrect(
        typed: String,
        ranked: List<Scored>,
    ): String? {
        if (typed.length < MIN_AUTOCORRECT_LENGTH) return null
        // Same letters, only accents/apostrophes missing: that is what the user meant.
        val accentFix = ranked.firstOrNull { it.entry.cost == 0f }
        val maxCost = if (typed.length >= LONG_WORD) 2f else 1f
        val runnerUp = ranked.getOrNull(1)?.score ?: Double.NEGATIVE_INFINITY
        val typoFix =
            ranked.firstOrNull()?.takeIf {
                typed.length >= MIN_TYPO_LENGTH && it.entry.cost <= maxCost && it.score - runnerUp >= AUTO_MARGIN
            }
        return (accentFix ?: typoFix)?.let { matchCase(it.entry.word, typed) }
    }

    private fun score(
        frequency: Long,
        cost: Float,
    ): Double = ln(frequency.toDouble() + 1) - COST_WEIGHT * cost

    companion object {
        private const val SHORT_WORD = 4
        private const val LONG_WORD = 7
        private const val MIN_AUTOCORRECT_LENGTH = 2

        /** Shorter words are too ambiguous (and often chat abbreviations) to fix typos in. */
        private const val MIN_TYPO_LENGTH = 3

        /** One full edit is worth about e^4.5 ≈ 90× the frequency. */
        private const val COST_WEIGHT = 4.5

        /** The winner must be at least e^1 ≈ 2.7× more likely than the runner-up. */
        private const val AUTO_MARGIN = 1.0
        private const val COMPLETION_PENALTY = 1.5
        private const val COMPLETIONS = 3
        private const val LEARNED_FREQUENCY = 5_000L

        /** Copies the capitalization pattern of [typed] onto [word] ("Cancion" → "Canción"). */
        fun matchCase(
            word: String,
            typed: String,
        ): String {
            val letters = typed.filter { it.isLetter() }
            return when {
                letters.length > 1 && letters.all { it.isUpperCase() } -> word.uppercase()
                typed.firstOrNull()?.isUpperCase() == true -> word.replaceFirstChar { it.uppercaseChar() }
                else -> word
            }
        }
    }
}
