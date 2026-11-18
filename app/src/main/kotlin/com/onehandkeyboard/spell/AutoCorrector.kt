package com.onehandkeyboard.spell

import android.view.inputmethod.InputConnection
import java.util.concurrent.Executor

/**
 * Applies corrections to the field and lets the user take them back.
 *
 * - [beforeSeparator] runs when a space or punctuation closes a word: an unknown word with a
 *   confident fix is replaced; the original is remembered.
 * - [onBackspace] right after a correction restores the original word (keeping the separator)
 *   and learns it, so the same word is never corrected again.
 * - Picking a suggestion in the strip ([pick]) replaces the word being typed.
 * - [refresh] recomputes the strip for the word at the cursor, off the main thread.
 *
 * Never logs what the user types.
 */
class AutoCorrector(
    private val suggester: () -> Suggester?,
    private val userDictionary: () -> UserDictionary?,
    private val background: Executor,
    private val main: Executor,
) {
    private data class Correction(
        val original: String,
        val corrected: String,
        val separator: String,
    )

    /** Called with the strip contents (on the main thread). */
    var onSuggestions: (Suggestions) -> Unit = {}

    var policy: FieldPolicy = FieldPolicy.NONE
        private set

    private var lastCorrection: Correction? = null

    /** Words the user rejected a correction for in this session (kept even when learning is off). */
    private val rejected = HashSet<String>()
    private var generation = 0

    fun startInput(policy: FieldPolicy) {
        this.policy = policy
        lastCorrection = null
        publish(Suggestions.EMPTY)
    }

    /** Any key other than a separator or the backspace that follows a correction. */
    fun invalidate() {
        lastCorrection = null
    }

    /**
     * A separator is about to be committed. If the word before the cursor must be corrected,
     * replaces it and returns true. The caller commits [separator] afterwards either way.
     */
    fun beforeSeparator(
        ic: InputConnection?,
        separator: String,
    ): Boolean {
        lastCorrection = null
        val engine = suggester()
        val context = if (ic != null && (policy.autoCorrect || policy.learn)) readContext(ic) else null
        val word = context?.word?.takeIf { it.isNotEmpty() && engine?.isKnown(it) == false }
        val canFix = context != null && mayAutoCorrect(context)
        if (ic == null || engine == null || word == null) return false

        val fix = if (canFix) engine.suggest(word).autoCorrect else null
        if (fix != null) {
            replaceBeforeCursor(ic, word.length, fix)
            lastCorrection = Correction(word, fix, separator)
        } else if (policy.learn) {
            userDictionary()?.recordUnknown(word)
        }
        return fix != null
    }

    /** Undoes the correction just made, if the cursor is still right after it. */
    fun onBackspace(ic: InputConnection?): Boolean {
        val correction = lastCorrection
        lastCorrection = null
        if (ic == null || correction == null) return false
        val applies = ic.getSelectedText(0).isNullOrEmpty() && isRightAfter(ic, correction)
        if (applies) {
            replaceBeforeCursor(
                ic,
                correction.corrected.length + correction.separator.length,
                correction.original + correction.separator,
            )
            keep(correction.original)
            publish(Suggestions.EMPTY)
        }
        return applies
    }

    /** Applies a strip suggestion: restores, keeps or replaces the word at the cursor. */
    fun pick(
        ic: InputConnection?,
        suggestion: Suggestion,
    ) {
        ic ?: return
        if (suggestion.kind == Suggestion.Kind.REVERT) {
            onBackspace(ic)
            return
        }
        lastCorrection = null
        val word = readContext(ic)?.word.orEmpty()
        replaceBeforeCursor(ic, word.length, suggestion.text + " ")
        if (suggestion.kind == Suggestion.Kind.TYPED) keep(suggestion.text)
        publish(Suggestions.EMPTY)
    }

    /** Recomputes the strip for the current cursor position. */
    fun refresh(ic: InputConnection?) {
        val correction = lastCorrection
        val connection = ic?.takeIf { policy.suggestions }
        val context = connection?.let(::readContext)
        val engine = suggester()
        when {
            connection == null -> publish(Suggestions.EMPTY)
            correction != null && isRightAfter(connection, correction) ->
                publish(
                    Suggestions.revert(correction.original),
                )
            context == null || context.word.isEmpty() || engine == null -> publish(Suggestions.EMPTY)
            else -> {
                val token = ++generation
                val allowAuto = mayAutoCorrect(context)
                background.execute {
                    val result = engine.suggest(context.word, allowAuto)
                    main.execute { if (token == generation) onSuggestions(result) }
                }
            }
        }
    }

    private fun mayAutoCorrect(context: WordContext) =
        policy.autoCorrect && context.mayAutoCorrect && context.word.lowercase() !in rejected

    /** True if the text right before the cursor is still the correction plus its separator. */
    private fun isRightAfter(
        ic: InputConnection,
        correction: Correction,
    ): Boolean {
        val expected = correction.corrected + correction.separator
        return ic.getTextBeforeCursor(expected.length, 0)?.toString() == expected
    }

    private fun keep(word: String) {
        rejected += word.lowercase()
        if (policy.learn) userDictionary()?.learn(word)
    }

    private fun publish(suggestions: Suggestions) {
        generation++
        onSuggestions(suggestions)
    }

    private fun replaceBeforeCursor(
        ic: InputConnection,
        length: Int,
        text: String,
    ) {
        ic.beginBatchEdit()
        if (length > 0) ic.deleteSurroundingText(length, 0)
        ic.commitText(text, 1)
        ic.endBatchEdit()
    }

    private fun readContext(ic: InputConnection): WordContext? {
        val before = ic.getTextBeforeCursor(LOOKBACK, 0) ?: return null
        return WordContext.of(before, ic.getTextAfterCursor(1, 0))
    }

    private companion object {
        const val LOOKBACK = 48
    }
}
