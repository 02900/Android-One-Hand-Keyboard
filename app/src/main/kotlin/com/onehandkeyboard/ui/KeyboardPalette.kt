package com.onehandkeyboard.ui

import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.onehandkeyboard.core.design.tokens.DesignTokens

/**
 * Colors used by [KeyboardView], resolved once per theme (no allocations while drawing).
 *
 * Letter keys (the 3×3 block) use the primary colors; the control column and the bottom
 * row use the secondary ones.
 */
class KeyboardPalette(
    val root: Int,
    val primaryFill: Int,
    val secondaryFill: Int,
    val primaryText: Int,
    val secondaryText: Int,
) {
    val primaryPressed: Int = ColorUtils.blendARGB(primaryFill, primaryText, PRESSED_BLEND)
    val secondaryPressed: Int = ColorUtils.blendARGB(secondaryFill, secondaryText, PRESSED_BLEND)

    /** The preview bubble inverts the primary key colors so it stands out. */
    val bubbleFill: Int get() = primaryText
    val bubbleText: Int get() = primaryFill

    fun fill(primary: Boolean) = if (primary) primaryFill else secondaryFill

    fun pressed(primary: Boolean) = if (primary) primaryPressed else secondaryPressed

    fun text(primary: Boolean) = if (primary) primaryText else secondaryText

    companion object {
        private const val PRESSED_BLEND = 0.18f

        fun from(tokens: DesignTokens) =
            KeyboardPalette(
                root = tokens.colors.backgroundRoot.toArgb(),
                primaryFill = tokens.colors.backgroundPrimary.toArgb(),
                secondaryFill = tokens.colors.backgroundSecondary.toArgb(),
                primaryText = tokens.colors.contentPrimary.toArgb(),
                secondaryText = tokens.colors.contentSecondary.toArgb(),
            )

        /** Letter block: rows 0–2, columns 0–2. */
        fun isPrimaryCell(cellIndex: Int): Boolean = cellIndex / 4 < 3 && cellIndex % 4 < 3
    }
}
