package com.onehandkeyboard.ui

import android.view.inputmethod.EditorInfo
import com.onehandkeyboard.KeyboardLayoutManager
import com.onehandkeyboard.actions.LabelContext
import com.onehandkeyboard.actions.label
import com.onehandkeyboard.actions.spokenName
import com.onehandkeyboard.config.CellConfiguration
import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.GestureType
import com.onehandkeyboard.input.InputCommitter

/**
 * Visible labels and accessibility descriptions of the keys, derived from the current
 * keyboard state. All the mapping lives in [com.onehandkeyboard.actions.label].
 */
class GestureHintProvider(private val layoutManager: KeyboardLayoutManager) {
    /** Label of the Enter key, set from the field's IME action (↵, 🔍, →, ✓, ➤). */
    var enterLabel: String = "↵"

    fun labelContext() =
        LabelContext(
            caseMode = layoutManager.currentCaseMode,
            layoutMode = layoutManager.currentMode,
            pendingDeadKey = layoutManager.pendingDeadKey,
            enterLabel = enterLabel,
            languageLabel = layoutManager.currentLanguage.label,
        )

    fun getCenterLabel(
        cell: CellConfiguration,
        ctx: LabelContext = labelContext(),
    ): String = cell.getActionFor(GestureType.TAP, Direction.CENTER)?.label(ctx).orEmpty()

    fun getDirectionalLabel(
        cell: CellConfiguration,
        direction: Direction,
        ctx: LabelContext = labelContext(),
    ): String = cell.getActionFor(GestureType.SWIPE, direction)?.label(ctx).orEmpty()

    /** "a. arriba: ñ, abajo: l" — read by TalkBack when the key gets focus. */
    fun describe(cell: CellConfiguration): String {
        val ctx = labelContext()
        val tap = cell.getActionFor(GestureType.TAP, Direction.CENTER)?.spokenName(ctx)
        val swipes =
            cell.getDirectionsFor(GestureType.SWIPE).mapNotNull { direction ->
                cell.getActionFor(
                    GestureType.SWIPE,
                    direction,
                )?.let { "${direction.spokenName()}: ${it.spokenName(ctx)}" }
            }
        return listOfNotNull(tap, swipes.takeIf { it.isNotEmpty() }?.joinToString(", ")).joinToString(". ")
    }

    /** Label of the accessibility action that performs a swipe ("Deslizar arriba: ñ"). */
    fun describeSwipe(
        cell: CellConfiguration,
        direction: Direction,
    ): String {
        val action = cell.getActionFor(GestureType.SWIPE, direction)?.spokenName(labelContext()).orEmpty()
        return "Deslizar ${direction.spokenName()}: $action"
    }

    companion object {
        /** Enter key label for the field's IME action. */
        fun enterLabelFor(info: EditorInfo?): String {
            if (InputCommitter.wantsNewline(info) || !InputCommitter.hasEditorAction(info)) return "↵"
            return when (InputCommitter.editorAction(info)) {
                EditorInfo.IME_ACTION_SEARCH -> "🔍"
                EditorInfo.IME_ACTION_GO -> "→"
                EditorInfo.IME_ACTION_NEXT -> "⇥"
                EditorInfo.IME_ACTION_PREVIOUS -> "⇤"
                EditorInfo.IME_ACTION_DONE -> "✓"
                EditorInfo.IME_ACTION_SEND -> "➤"
                else -> "↵"
            }
        }
    }
}
