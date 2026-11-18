package com.onehandkeyboard.actions

import com.onehandkeyboard.core.domain.model.CaseMode
import com.onehandkeyboard.enums.LayoutMode
import com.onehandkeyboard.input.DeadKeys

/**
 * Keyboard state that affects how keys are labelled.
 */
data class LabelContext(
    val caseMode: CaseMode = CaseMode.LOWERCASE,
    val layoutMode: LayoutMode = LayoutMode.LETTERS,
    val pendingDeadKey: Char? = null,
    val enterLabel: String = "↵",
    val languageLabel: String = "ES",
) {
    /** Applies the case mode and, with a pending dead key, previews the accented vowel. */
    fun letter(text: String): String {
        val cased =
            when (caseMode) {
                CaseMode.UPPERCASE -> text.uppercase()
                CaseMode.LOWERCASE -> text.lowercase()
                CaseMode.CAPITALIZE -> text.lowercase().replaceFirstChar { it.uppercase() }
            }
        val accent = pendingDeadKey
        val char = cased.singleOrNull()
        val composes = accent != null && char != null && DeadKeys.canCompose(accent, char)
        return if (composes) DeadKeys.compose(accent!!, cased) else cased
    }
}
