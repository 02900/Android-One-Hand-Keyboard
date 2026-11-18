package com.onehandkeyboard.ui

import android.content.Context

/** Keyboard dimensions: every row of the grid has the same fixed height. */
class LayoutCalculator(
    private val context: Context,
) {
    /** Cell height in pixels. */
    fun calculateCellHeight(): Int = (CELL_HEIGHT_DP * context.resources.displayMetrics.density).toInt()

    companion object {
        const val CELL_HEIGHT_DP = 60f
    }
}
