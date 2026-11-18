package com.onehandkeyboard.ui

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import com.onehandkeyboard.GestureIntegration
import com.onehandkeyboard.KeyboardLayoutManager
import com.onehandkeyboard.actions.ActionConfig
import com.onehandkeyboard.config.CellConfiguration
import com.onehandkeyboard.config.KeyboardConfiguration
import com.onehandkeyboard.core.design.theme.ThemeManager
import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.GestureType
import com.onehandkeyboard.core.domain.model.KeyAction
import com.onehandkeyboard.prefs.KeyboardPreferences

/**
 * Owns the keyboard view tree and updates it **in place**:
 * - case change → only label texts change,
 * - layer change → the cached grid of that layer fades in (each layer is built once),
 * - height change → cell layout params are updated (also live while resizing),
 * - theme change → grids are rebuilt inside the same root (no `setInputView`).
 *
 * It also renders the touch feedback requested by [GestureIntegration]: pressed state,
 * press scale, highlighted target label and a floating preview bubble.
 */
class KeyboardViewController(
    private val context: Context,
    private val layoutManager: KeyboardLayoutManager,
    private val themeManager: ThemeManager,
    private val styleProvider: ButtonStyleProvider,
    private val hintProvider: GestureHintProvider,
    private val layoutCalculator: LayoutCalculator,
    private val gestureIntegration: GestureIntegration,
    private val prefs: KeyboardPreferences,
) : GestureIntegration.CellListener {
    private class CellHolder(
        val config: CellConfiguration,
        val view: FrameLayout,
        val background: ButtonStyleProvider.CellBackground,
        val center: TextView?,
        val directional: Map<Direction, TextView>,
    ) {
        var releaseAnimator: ValueAnimator? = null

        fun label(direction: Direction): TextView? =
            if (direction == Direction.CENTER) center else directional[direction]

        fun allLabels(): List<TextView> = listOfNotNull(center) + directional.values
    }

    private class Grid(
        val view: GridLayout,
        val cells: Map<Int, CellHolder>,
    )

    /** Show the preview bubble (disabled for password fields). */
    var previewEnabled = true

    private var root: FrameLayout? = null
    private var row: LinearLayout? = null
    private var column: LinearLayout? = null
    private var sidePanel: LinearLayout? = null
    private var bubble: TextView? = null
    private val grids = LinkedHashMap<String, Grid>()
    private var visibleGrid: Grid? = null
    private var resizeStartHeightDp = 0f
    private val density = context.resources.displayMetrics.density

    init {
        gestureIntegration.cellListener = this
    }

    /** Creates a fresh root view (called from `onCreateInputView`). */
    fun createView(): View {
        grids.clear()
        visibleGrid = null
        val newColumn = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val newRow =
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams =
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                    )
            }
        val newBubble = createBubble()
        val newRoot =
            FrameLayout(context).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
                clipChildren = false
                addView(newRow)
                addView(newBubble)
            }
        root = newRoot
        row = newRow
        column = newColumn
        bubble = newBubble
        sidePanel = createSidePanel()
        applyRootTheme()
        applyOneHandLayout()
        showCurrentLayout(animate = false)
        return newRoot
    }

    /** Shows the grid of the active language/mode, building it the first time. */
    fun showCurrentLayout(animate: Boolean = true) {
        val column = column ?: return
        val config = layoutManager.keyboardConfiguration
        val key = keyOf(config)
        val grid = grids.getOrPut(key) { buildGrid(config).also { column.addView(it.view) } }
        val changed = visibleGrid !== grid
        grids.values.forEach { it.view.visibility = if (it === grid) View.VISIBLE else View.GONE }
        visibleGrid = grid
        refreshLabels()
        if (changed && animate) {
            grid.view.alpha = 0f
            grid.view.animate().alpha(1f).setDuration(LAYER_FADE_MS).start()
        }
    }

    /** Updates label texts (case mode, shift indicator, dead key, enter action...). */
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
        val column = column ?: return
        gestureIntegration.cancelAll()
        column.removeAllViews()
        grids.clear()
        visibleGrid = null
        bubble?.let { styleBubble(it) }
        sidePanel = createSidePanel()
        applyRootTheme()
        applyOneHandLayout()
        showCurrentLayout(animate = false)
    }

    /** Narrow keyboard aligned to one side (persisted), with a side panel to move or exit it. */
    fun toggleOneHand() {
        prefs.oneHandEnabled = !prefs.oneHandEnabled
        applyOneHandLayout()
    }

    // ------------------------------------------------------------------
    // Touch feedback (GestureIntegration.CellListener)
    // ------------------------------------------------------------------

    override fun onCellPressed(cellIndex: Int) {
        val holder = visibleGrid?.cells?.get(cellIndex) ?: return
        holder.releaseAnimator?.cancel()
        holder.background.fill.color = holder.background.pressedColor
        holder.view.animate().scaleX(PRESS_SCALE).scaleY(PRESS_SCALE).setDuration(PRESS_ANIM_MS).start()
        highlight(holder, Direction.CENTER)
    }

    override fun onDirectionChanged(
        cellIndex: Int,
        direction: Direction,
    ) {
        val holder = visibleGrid?.cells?.get(cellIndex) ?: return
        highlight(holder, direction)
    }

    override fun onCellReleased(cellIndex: Int) {
        val holder = visibleGrid?.cells?.get(cellIndex) ?: return
        val bg = holder.background
        holder.releaseAnimator?.cancel()
        holder.releaseAnimator =
            ValueAnimator.ofArgb(bg.fill.color, bg.normalColor).apply {
                duration = RELEASE_ANIM_MS
                addUpdateListener { bg.fill.color = it.animatedValue as Int }
                start()
            }
        holder.view.animate().scaleX(1f).scaleY(1f).setDuration(RELEASE_ANIM_MS).start()
        holder.allLabels().forEach { label ->
            label.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(RELEASE_ANIM_MS).start()
        }
        bubble?.visibility = View.GONE
    }

    override fun onResizeStarted() {
        resizeStartHeightDp = layoutCalculator.getCurrentCellHeightDp()
        bubble?.visibility = View.GONE
    }

    override fun onResizeDrag(totalDyPx: Float) {
        // Dragging up makes every row taller; the drag is spread over the rows.
        val newHeight = resizeStartHeightDp - totalDyPx / (GRID_SIZE * density)
        layoutCalculator.setCellHeightDp(newHeight, persist = false)
        applyCellHeight()
    }

    override fun onResizeEnded() {
        layoutCalculator.commitLiveHeight()
        applyCellHeight()
    }

    /** Emphasises the label the gesture will produce and previews it in the bubble. */
    private fun highlight(
        holder: CellHolder,
        direction: Direction,
    ) {
        val target = holder.label(direction)
        holder.allLabels().forEach { label ->
            val isTarget = label === target
            val scale = if (isTarget && direction != Direction.CENTER) TARGET_SCALE else 1f
            val alpha = if (target == null || isTarget) 1f else DIMMED_ALPHA
            label.animate().scaleX(scale).scaleY(scale).alpha(alpha).setDuration(PRESS_ANIM_MS).start()
        }
        showBubble(holder, direction)
    }

    private fun showBubble(
        holder: CellHolder,
        direction: Direction,
    ) {
        val bubble = bubble ?: return
        val root = root ?: return
        val gestureType = if (direction == Direction.CENTER) GestureType.TAP else GestureType.SWIPE
        val action = holder.config.getActionFor(gestureType, direction)
        val text = holder.label(direction)?.text
        if (!previewEnabled || !isPreviewable(action) || text.isNullOrEmpty()) {
            bubble.visibility = View.GONE
            return
        }
        bubble.text = text

        val cellRect = Rect(0, 0, holder.view.width, holder.view.height)
        root.offsetDescendantRectToMyCoords(holder.view, cellRect)
        val size = (BUBBLE_SIZE_DP * density).toInt()
        val lp = bubble.layoutParams as FrameLayout.LayoutParams
        lp.width = size
        lp.height = size
        lp.leftMargin = (cellRect.centerX() - size / 2).coerceIn(0, (root.width - size).coerceAtLeast(0))
        lp.topMargin = (cellRect.top - size + size / BUBBLE_OVERLAP_DIVISOR).coerceAtLeast(0)
        bubble.layoutParams = lp
        bubble.visibility = View.VISIBLE
    }

    private fun isPreviewable(action: ActionConfig?): Boolean =
        when (action) {
            is ActionConfig.Character, is ActionConfig.Text -> true
            is ActionConfig.Action -> action.keyAction is KeyAction.DeadKey
            else -> false
        }

    // ------------------------------------------------------------------
    // Building
    // ------------------------------------------------------------------

    private fun applyRootTheme() {
        root?.setBackgroundColor(themeManager.currentTheme.value.colors.backgroundRoot.toArgb())
    }

    private fun applyOneHandLayout() {
        val row = row ?: return
        val column = column ?: return
        val panel = sidePanel ?: return
        row.removeAllViews()
        if (!prefs.oneHandEnabled) {
            row.addView(column, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            return
        }
        val columnParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, ONE_HAND_WEIGHT)
        val panelParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f - ONE_HAND_WEIGHT)
        if (prefs.oneHandRight) {
            row.addView(panel, panelParams)
            row.addView(column, columnParams)
        } else {
            row.addView(column, columnParams)
            row.addView(panel, panelParams)
        }
    }

    private fun createSidePanel(): LinearLayout {
        val colors = themeManager.currentTheme.value.colors

        fun panelButton(
            symbol: String,
            description: String,
            onClick: () -> Unit,
        ) = TextView(context).apply {
            text = symbol
            contentDescription = description
            textSize = SIDE_PANEL_TEXT_SP
            gravity = Gravity.CENTER
            setTextColor(colors.contentSecondary.toArgb())
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(colors.backgroundSecondary.toArgb())
            addView(
                panelButton("⇆", "Mover teclado al otro lado") {
                    prefs.oneHandRight = !prefs.oneHandRight
                    applyOneHandLayout()
                },
            )
            addView(panelButton("⤢", "Teclado a ancho completo") { toggleOneHand() })
        }
    }

    private fun createBubble() =
        TextView(context).apply {
            gravity = Gravity.CENTER
            textSize = BUBBLE_TEXT_SP
            visibility = View.GONE
            elevation = BUBBLE_ELEVATION_DP * density
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            layoutParams = FrameLayout.LayoutParams(0, 0)
            styleBubble(this)
        }

    private fun styleBubble(view: TextView) {
        val colors = themeManager.currentTheme.value.colors
        view.setTextColor(colors.backgroundPrimary.toArgb())
        view.background =
            GradientDrawable().apply {
                cornerRadius = BUBBLE_RADIUS_DP * density
                setColor(colors.contentPrimary.toArgb())
            }
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
        val margin = (LABEL_MARGIN_DP * density).toInt()
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
        const val LABEL_MARGIN_DP = 2f
        const val PRESS_SCALE = 0.96f
        const val TARGET_SCALE = 1.4f
        const val DIMMED_ALPHA = 0.35f
        const val PRESS_ANIM_MS = 60L
        const val RELEASE_ANIM_MS = 80L
        const val LAYER_FADE_MS = 100L
        const val BUBBLE_SIZE_DP = 52f
        const val BUBBLE_TEXT_SP = 26f
        const val BUBBLE_RADIUS_DP = 12f
        const val BUBBLE_ELEVATION_DP = 6f
        const val BUBBLE_OVERLAP_DIVISOR = 4
        const val ONE_HAND_WEIGHT = 0.8f
        const val SIDE_PANEL_TEXT_SP = 22f
    }
}
