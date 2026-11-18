package com.onehandkeyboard.spell

import android.text.InputType
import android.view.inputmethod.EditorInfo
import com.onehandkeyboard.input.InputCommitter

/** What the autocorrector may do in the focused field. */
data class FieldPolicy(
    /** Show the suggestion strip contents. */
    val suggestions: Boolean,
    /** Replace unknown words on space / punctuation. */
    val autoCorrect: Boolean,
    /** Remember words the user keeps (off in incognito fields). */
    val learn: Boolean,
) {
    companion object {
        val NONE = FieldPolicy(suggestions = false, autoCorrect = false, learn = false)

        /**
         * Only prose text fields get suggestions: never passwords, URLs, emails, numbers or
         * fields that ask for no suggestions. Names and filter fields get suggestions without
         * automatic replacement.
         */
        fun from(
            info: EditorInfo?,
            suggestionsEnabled: Boolean,
            autoCorrectEnabled: Boolean,
        ): FieldPolicy {
            val type = info?.inputType ?: return NONE
            if ((type and InputType.TYPE_MASK_CLASS) != InputType.TYPE_CLASS_TEXT) return NONE
            if (InputCommitter.isPassword(info)) return NONE
            if ((type and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0) return NONE
            val variation = type and InputType.TYPE_MASK_VARIATION
            if (variation in NO_TEXT_VARIATIONS) return NONE

            val replaceable =
                variation != InputType.TYPE_TEXT_VARIATION_FILTER &&
                    variation != InputType.TYPE_TEXT_VARIATION_PERSON_NAME
            val incognito = (info.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0
            return FieldPolicy(
                suggestions = suggestionsEnabled,
                autoCorrect = autoCorrectEnabled && replaceable,
                learn = !incognito,
            )
        }

        private val NO_TEXT_VARIATIONS =
            setOf(
                InputType.TYPE_TEXT_VARIATION_URI,
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
            )
    }
}
