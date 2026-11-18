package com.onehandkeyboard.ui

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import com.onehandkeyboard.GestureIntegration
import com.onehandkeyboard.KeyboardLayoutManager
import com.onehandkeyboard.config.CellConfiguration
import com.onehandkeyboard.config.KeyboardConfiguration
import com.onehandkeyboard.core.design.theme.ThemeManager
import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.GestureType

/**
 * Owns the keyboard view tree and updates it **in place**:
 * - case change → only label texts change,
 * - layer change → the cached grid of that layer is shown (each layer is built once),
 * - height change → cell layout params are updated,
 * - theme change → grids are rebuilt inside the same root (no `setInputView`).
 *
 * Also renders the pressed state requested by [GestureIntegration].
 */
class KeyboardViewController(
    private val context: Context,
    private val layoutManager: KeyboardLayoutManager,
    private val themeManager: ThemeManager,
    private val styleProvider: ButtonStyleProvider,
    private val hintProvider: GestureHintProvider,
    private val layoutCalculator: LayoutCalculator,
    private val gestureIntegration: GestureIntegration,
) : GestureIntegration.CellListener {
    private class CellHolder(
        val config: CellConfiguration,
        val view: FrameLayout,
        val background: ButtonStyleProvider.CellBackground,
        val center: TextView?,
        val directional: Map<Direction, TextView>,
    ) {
        var releaseAnimator: ValueAnimator? = null
    }

    private class Grid(
        val view: GridLayout,
        val cells: Map<Int, CellHolder>,
    )

    private var root: LinearLayout? = null
    private val grids = LinkedHashMap<String, Grid>()
    private var visibleGrid: Grid? = null

    init {
        gestureIntegration.cellListener = this
    }

    /** Creates a fresh root view (called from `onCreateInputView`). */
    fun createView(): View {
        grids.clear()
        visibleGrid = null
        val newRoot =
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
            }
        root = newRoot
        applyRootTheme()
        showCurrentLayout()
        return newRoot
    }

    /** Shows the grid of the active language/mode, building it the first time. */
    fun showCurrentLayout() {
        val root = root ?: return
        val config = layoutManager.keyboardConfiguration
        val key = keyOf(config)
        val grid = grids.getOrPut(key) { buildGrid(config).also { root.addView(it.view) } }
        grids.values.forEach { it.view.visibility = if (it === grid) View.VISIBLE else View.GONE }
        visibleGrid = grid
        refreshLabels()
    }

    /** Updates label texts (case mode, shift indicator...). No view is created. */
    fun refreshLabels() {
        visibleGrid?.cells?.values?.forEach { holder ->
            holder.center?.text = hintProvider.getCenterLabel(holder.config)
            holder.directional.forEach { (direction, label) ->
                label.text = hintProvider.getDirectionalLabel(holder.config, direction)
            }
        }
    }

    fun applyCellHeight() {
        val height = layoutCalculator.calculateCellHeight()
        grids.values.flatMap { it.cells.values }.forEach { holder ->
            holder.view.layoutParams = holder.view.layoutParams.apply { this.height = height }
        }
    }

    /** Re-applies the theme by rebuilding the grids inside the existing root. */
    fun applyTheme() {
        val root = root ?: return
        gestureIntegration.cancelAll()
        root.removeAllViews()
        grids.clear()
        visibleGrid = null
        applyRootTheme()
        showCurrentLayout()
    }

    override fun onCellPressed(cellIndex: Int) {
        val holder = visibleGrid?.cells?.get(cellIndex) ?: return
        holder.releaseAnimator?.cancel()
        holder.background.fill.color = holder.background.pressedColor
    }

    override fun onCellReleased(cellIndex: Int) {
        val holder = visibleGrid?.cells?.get(cellIndex) ?: return
        val bg = holder.background
        holder.releaseAnimator?.cancel()
        holder.releaseAnimator =
            ValueAnimator.ofArgb(bg.fill.color, bg.normalColor).apply {
                duration = RELEASE_FADE_MS
                addUpdateListener { bg.fill.color = it.animatedValue as Int }
                start()
            }
    }

    private fun applyRootTheme() {
        root?.setBackgroundColor(themeManager.currentTheme.value.colors.backgroundRoot.toArgb())
    }

    private fun buildGrid(config: KeyboardConfiguration): Grid {
        val gridView =
            GridLayout(context).apply {
                columnCount = GRID_SIZE
                rowCount = GRID_SIZE
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
            }
        val cells = config.cells.associate { cellConfig -> cellConfig.cellIndex to buildCell(cellConfig) }
        cells.values.forEach { gridView.addView(it.view) }
        return Grid(gridView, cells)
    }

    @SuppressLint("ClickableViewAccessibility") // accessibility is handled in fase 4 (KeyboardView)
    private fun buildCell(config: CellConfiguration): CellHolder {
        val background =
            styleProvider.createCellBackground(
                cellIndex = config.cellIndex,
                row = config.row,
                lastColumn = config.col + config.colSpan - 1,
            )
        val cellView =
            FrameLayout(context).apply {
                this.background = background.drawable
                layoutParams =
                    GridLayout.LayoutParams().apply {
                        width = 0
                        height = layoutCalculator.calculateCellHeight()
                        columnSpec = GridLayout.spec(config.col, config.colSpan, 1f)
                        rowSpec = GridLayout.spec(config.row, config.rowSpan)
                    }
                setOnTouchListener { view, event -> gestureIntegration.onTouch(view, event, config.cellIndex) }
            }

        val directional =
            config.getDirectionsFor(GestureType.SWIPE).associateWith { direction ->
                createDirectionalLabel(direction).also { cellView.addView(it) }
            }
        val center =
            config.getActionFor(GestureType.TAP, Direction.CENTER)?.let {
                createCenterLabel(config.cellIndex).also { cellView.addView(it) }
            }
        return CellHolder(config, cellView, background, center, directional)
    }

    private fun createCenterLabel(cellIndex: Int) =
        TextView(context).apply {
            textSize = styleProvider.getCenterTextSize()
            setTextColor(styleProvider.getTextColor(isGridCell = true, cellIndex = cellIndex))
            gravity = Gravity.CENTER
            layoutParams =
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER,
                )
        }

    private fun createDirectionalLabel(direction: Direction): TextView {
        val margin = (LABEL_MARGIN_DP * context.resources.displayMetrics.density).toInt()
        val gravity =
            when (direction) {
                Direction.N -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
                Direction.NE -> Gravity.TOP or Gravity.END
                Direction.E -> Gravity.CENTER_VERTICAL or Gravity.END
                Direction.SE -> Gravity.BOTTOM or Gravity.END
                Direction.S -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                Direction.SW -> Gravity.BOTTOM or Gravity.START
                Direction.W -> Gravity.CENTER_VERTICAL or Gravity.START
                Direction.NW -> Gravity.TOP or Gravity.START
                Direction.CENTER -> Gravity.CENTER
            }
        return TextView(context).apply {
            textSize = styleProvider.getDirectionalLabelTextSize()
            setTextColor(styleProvider.getDirectionalLabelColor())
            this.gravity = Gravity.CENTER
            layoutParams =
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    gravity,
                ).apply { setMargins(margin, margin, margin, margin) }
        }
    }

    private fun keyOf(config: KeyboardConfiguration) = "${config.language}_${config.mode}"

    private companion object {
        const val GRID_SIZE = 4
        const val RELEASE_FADE_MS = 80L
        const val LABEL_MARGIN_DP = 2f
    }
}
