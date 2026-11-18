package com.onehandkeyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.os.SystemClock
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.customview.widget.ExploreByTouchHelper
import com.onehandkeyboard.GestureIntegration
import com.onehandkeyboard.KeyboardLayoutManager
import com.onehandkeyboard.R
import com.onehandkeyboard.actions.ActionConfig
import com.onehandkeyboard.config.CellConfiguration
import com.onehandkeyboard.config.KeyboardConfiguration
import com.onehandkeyboard.core.design.theme.ThemeManager
import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.GestureType
import com.onehandkeyboard.core.domain.model.KeyAction
import com.onehandkeyboard.prefs.KeyboardPreferences
import com.onehandkeyboard.spell.Suggestion
import com.onehandkeyboard.spell.Suggestions
import java.util.EnumMap
import kotlin.math.max
import kotlin.math.min

/**
 * The whole keyboard drawn on a single Canvas.
 *
 * - No child views: a layer, case or theme change only updates labels/colors and redraws.
 * - Every pointer is hit-tested here and forwarded to [GestureIntegration], so roll-over
 *   typing, swipes, backspace repeat, trackpad and resize keep their per-pointer logic.
 * - Touch feedback (pressed color, 0.96 press scale, highlighted target label, preview
 *   bubble, layer fade) is interpolated inside [onDraw]; nothing allocates while drawing.
 * - One-hand mode narrows the grid to 80 % and draws a side panel (⇆ switch side, ⤢ exit).
 * - A suggestion strip on top shows left / center / right suggestions from the autocorrector.
 * - Accessibility: [ExploreByTouchHelper] exposes one virtual node per key with its tap
 *   action and one custom action per swipe, so TalkBack users can reach every character.
 */
@SuppressLint("ViewConstructor") // created in code by the service, never inflated
@Suppress("LongParameterList") // explicit wiring of the service components (views are not injected)
class KeyboardView(
    context: Context,
    private val layoutManager: KeyboardLayoutManager,
    private val themeManager: ThemeManager,
    private val labels: GestureHintProvider,
    private val layoutCalculator: LayoutCalculator,
    private val gestures: GestureIntegration,
    private val prefs: KeyboardPreferences,
) : View(context),
    GestureIntegration.CellListener {
    private class Key(
        val config: CellConfiguration,
    ) {
        val index get() = config.cellIndex
        val primary = KeyboardPalette.isPrimaryCell(config.cellIndex)
        val swipeDirections = config.getDirectionsFor(GestureType.SWIPE)
        val rect = RectF()
        var center = ""
        val directional = EnumMap<Direction, String>(Direction::class.java)

        // Animated state, each value moves towards its target in onDraw.
        var press = 0f
        var pressTarget = 0f
        var target: Direction? = null
        var emphasis = 0f // dims the labels that are not the target
        var emphasisTarget = 0f
        var grow = 0f // scales the target swipe label up
        var growTarget = 0f

        fun label(direction: Direction): String? = if (direction == Direction.CENTER) center else directional[direction]

        fun settled() = press == pressTarget && emphasis == emphasisTarget && grow == growTarget
    }

    private enum class PanelButton(
        val symbol: String,
        val description: String,
        val virtualId: Int,
    ) {
        SWAP_SIDE("⇆", "Mover teclado al otro lado", PANEL_VIRTUAL_ID_BASE),
        FULL_WIDTH("⤢", "Teclado a ancho completo", PANEL_VIRTUAL_ID_BASE + 1),
    }

    /** Show the preview bubble (disabled for password fields). */
    var previewEnabled = true

    /** Called when the user taps a suggestion in the strip. */
    var onSuggestionPicked: (Suggestion) -> Unit = {}

    /** Contents of the suggestion strip. */
    var suggestions: Suggestions
        get() = strip.suggestions
        set(value) {
            if (strip.suggestions == value) return
            strip.suggestions = value
            invalidate()
            accessibilityHelper.invalidateRoot()
        }

    private val density = resources.displayMetrics.density
    private var palette = KeyboardPalette.from(themeManager.currentTheme.value)
    private var configuration: KeyboardConfiguration? = null
    private var keys: List<Key> = emptyList()
    private val keysByIndex = HashMap<Int, Key>()
    private val gridRect = RectF()
    private val panelRect = RectF()
    private val strip = SuggestionStrip(resources)

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centerPaint = textPaint(CENTER_TEXT_SP)
    private val labelPaint = textPaint(LABEL_TEXT_SP)
    private val bubblePaint = textPaint(BUBBLE_TEXT_SP)
    private val panelPaint = textPaint(PANEL_TEXT_SP)
    private val centerMetrics = centerPaint.fontMetrics
    private val labelMetrics = labelPaint.fontMetrics
    private val bubbleMetrics = bubblePaint.fontMetrics
    private val panelMetrics = panelPaint.fontMetrics
    private val drawRect = RectF()
    private val boundsRect = Rect()
    private val screenLocation = IntArray(2)

    private var animating = false
    private var lastFrameMs = 0L
    private var layerAlpha = 1f

    private var bubbleKey: Key? = null
    private var bubbleText: String? = null

    private val gesturePointers = HashSet<Int>()
    private val panelPointers = HashMap<Int, PanelButton>()
    private val stripPointers = HashMap<Int, Int>()
    private var resizeStartHeightDp = 0f

    private val accessibilityHelper = AccessibilityHelper()

    init {
        gestures.cellListener = this
        ViewCompat.setAccessibilityDelegate(this, accessibilityHelper)
        showCurrentLayout(animate = false)
    }

    // ------------------------------------------------------------------
    // State updates (called by the service)
    // ------------------------------------------------------------------

    /** Shows the active language/mode; [animate] fades the new layer in. */
    fun showCurrentLayout(animate: Boolean = true) {
        val config = layoutManager.keyboardConfiguration
        if (config !== configuration) {
            configuration = config
            keys = config.cells.map(::Key)
            keysByIndex.clear()
            keys.forEach { keysByIndex[it.index] = it }
            bubbleKey = null
            layoutKeys()
            if (animate) {
                layerAlpha = 0f
                startAnimating()
            }
        }
        refreshLabels()
    }

    /** Updates label texts (case mode, shift indicator, dead key, enter action...). */
    fun refreshLabels() {
        val ctx = labels.labelContext()
        keys.forEach { key ->
            key.center = labels.getCenterLabel(key.config, ctx)
            key.swipeDirections.forEach { key.directional[it] = labels.getDirectionalLabel(key.config, it, ctx) }
        }
        bubbleKey?.let { key -> key.target?.let { bubbleText = key.label(it) } }
        invalidate()
        accessibilityHelper.invalidateRoot()
    }

    fun applyCellHeight() {
        requestLayout()
        invalidate()
    }

    /** Shows or hides the suggestion strip after the setting changed. */
    fun applySuggestionsSetting() {
        requestLayout()
        invalidate()
    }

    fun applyTheme() {
        palette = KeyboardPalette.from(themeManager.currentTheme.value)
        invalidate()
    }

    /** Narrow keyboard aligned to one side (persisted). */
    fun toggleOneHand() {
        prefs.oneHandEnabled = !prefs.oneHandEnabled
        onGeometryChanged()
    }

    // ------------------------------------------------------------------
    // Measure / layout
    // ------------------------------------------------------------------

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val width =
            if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
                resources.displayMetrics.widthPixels
            } else {
                MeasureSpec.getSize(widthMeasureSpec)
            }
        setMeasuredDimension(width, layoutCalculator.calculateCellHeight() * GRID_ROWS + stripHeightPx().toInt())
    }

    override fun onSizeChanged(
        w: Int,
        h: Int,
        oldw: Int,
        oldh: Int,
    ) {
        super.onSizeChanged(w, h, oldw, oldh)
        layoutKeys()
    }

    private fun onGeometryChanged() {
        layoutKeys()
        invalidate()
        accessibilityHelper.invalidateRoot()
    }

    private fun layoutKeys() {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val top = min(stripHeightPx(), h / 2)
        strip.layout(w, top)
        val oneHand = prefs.oneHandEnabled
        val gridWidth = if (oneHand) w * ONE_HAND_WIDTH else w
        val gridLeft = if (oneHand && prefs.oneHandRight) w - gridWidth else 0f
        gridRect.set(gridLeft, top, gridLeft + gridWidth, h)
        when {
            !oneHand -> panelRect.setEmpty()
            prefs.oneHandRight -> panelRect.set(0f, top, gridLeft, h)
            else -> panelRect.set(gridWidth, top, w, h)
        }
        val columnWidth = gridWidth / GRID_COLUMNS
        val rowHeight = (h - top) / GRID_ROWS
        keys.forEach { key ->
            val c = key.config
            key.rect.set(
                gridLeft + c.col * columnWidth,
                top + c.row * rowHeight,
                gridLeft + (c.col + c.colSpan) * columnWidth,
                top + (c.row + c.rowSpan) * rowHeight,
            )
        }
    }

    private fun stripHeightPx(): Float = strip.heightPx(prefs.suggestionsEnabled)

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    override fun onDraw(canvas: Canvas) {
        advanceAnimations()
        canvas.drawColor(palette.root)
        val border = max(1f, BORDER_DP * density)
        val fading = layerAlpha < 1f
        if (fading) canvas.saveLayerAlpha(gridRect, (layerAlpha * OPAQUE).toInt())
        keys.forEach { drawKey(canvas, it, border) }
        if (fading) canvas.restore()
        if (!panelRect.isEmpty) drawPanel(canvas, border)
        strip.draw(canvas, palette, border)
        drawBubble(canvas)
        if (animating) postInvalidateOnAnimation()
    }

    private fun drawKey(
        canvas: Canvas,
        key: Key,
        border: Float,
    ) {
        val c = key.config
        val rightBorder = if (c.col + c.colSpan < GRID_COLUMNS) border else 0f
        val bottomBorder = if (c.row + c.rowSpan < GRID_ROWS) border else 0f
        val r = drawRect
        r.set(key.rect.left, key.rect.top, key.rect.right - rightBorder, key.rect.bottom - bottomBorder)

        val scale = 1f - (1f - PRESS_SCALE) * key.press
        canvas.save()
        canvas.scale(scale, scale, r.centerX(), r.centerY())
        fillPaint.color = ColorUtils.blendARGB(palette.fill(key.primary), palette.pressed(key.primary), key.press)
        canvas.drawRect(r, fillPaint)

        val textColor = palette.text(key.primary)
        val margin = LABEL_MARGIN_DP * density
        drawLabel(
            canvas,
            key,
            Direction.CENTER,
            key.center,
            centerPaint,
            r.centerX(),
            centeredBaseline(r, centerMetrics),
            textColor,
        )
        for ((direction, text) in key.directional) {
            if (text.isEmpty()) continue
            val halfWidth = labelPaint.measureText(text) / 2
            val x =
                when (direction) {
                    Direction.W, Direction.NW, Direction.SW -> r.left + margin + halfWidth
                    Direction.E, Direction.NE, Direction.SE -> r.right - margin - halfWidth
                    else -> r.centerX()
                }
            val baseline =
                when (direction) {
                    Direction.N, Direction.NE, Direction.NW -> r.top + margin - labelMetrics.ascent
                    Direction.S, Direction.SE, Direction.SW -> r.bottom - margin - labelMetrics.descent
                    else -> centeredBaseline(r, labelMetrics)
                }
            drawLabel(canvas, key, direction, text, labelPaint, x, baseline, textColor, LABEL_ALPHA)
        }
        canvas.restore()
    }

    @Suppress("LongParameterList")
    private fun drawLabel(
        canvas: Canvas,
        key: Key,
        direction: Direction,
        text: String,
        paint: Paint,
        x: Float,
        baseline: Float,
        color: Int,
        baseAlpha: Float = 1f,
    ) {
        if (text.isEmpty()) return
        val target = key.target
        val isTarget = target == direction
        val alpha = if (target == null || isTarget) 1f else 1f - (1f - DIMMED_ALPHA) * key.emphasis
        paint.color = color
        paint.alpha = (Color.alpha(color) * alpha * baseAlpha).toInt()
        val scale = if (isTarget && direction != Direction.CENTER) 1f + (TARGET_SCALE - 1f) * key.grow else 1f
        if (scale != 1f) {
            canvas.save()
            val metrics = if (paint === centerPaint) centerMetrics else labelMetrics
            canvas.scale(scale, scale, x, baseline + (metrics.ascent + metrics.descent) / 2)
        }
        canvas.drawText(text, x, baseline, paint)
        if (scale != 1f) canvas.restore()
    }

    private fun drawPanel(
        canvas: Canvas,
        border: Float,
    ) {
        fillPaint.color = palette.secondaryFill
        canvas.drawRect(panelRect, fillPaint)
        PanelButton.values().forEach { button ->
            panelButtonRect(button, drawRect)
            if (button.ordinal > 0) {
                fillPaint.color = palette.root
                canvas.drawRect(drawRect.left, drawRect.top, drawRect.right, drawRect.top + border, fillPaint)
            }
            panelPaint.color = palette.secondaryText
            canvas.drawText(button.symbol, drawRect.centerX(), centeredBaseline(drawRect, panelMetrics), panelPaint)
        }
    }

    private fun drawBubble(canvas: Canvas) {
        val key = bubbleKey ?: return
        val text = bubbleText ?: return
        val size = BUBBLE_SIZE_DP * density
        val left = (key.rect.centerX() - size / 2).coerceIn(0f, max(0f, width - size))
        val top = (key.rect.top - size + size / BUBBLE_OVERLAP_DIVISOR).coerceAtLeast(0f)
        drawRect.set(left, top, left + size, top + size)
        val radius = BUBBLE_RADIUS_DP * density
        fillPaint.color = palette.bubbleFill
        canvas.drawRoundRect(drawRect, radius, radius, fillPaint)
        bubblePaint.color = palette.bubbleText
        canvas.drawText(text, drawRect.centerX(), centeredBaseline(drawRect, bubbleMetrics), bubblePaint)
    }

    private fun centeredBaseline(
        rect: RectF,
        metrics: Paint.FontMetrics,
    ) = rect.centerY() - (metrics.ascent + metrics.descent) / 2

    // ------------------------------------------------------------------
    // Animation
    // ------------------------------------------------------------------

    private fun startAnimating() {
        if (!animating) {
            animating = true
            lastFrameMs = SystemClock.uptimeMillis()
        }
        postInvalidateOnAnimation()
    }

    private fun advanceAnimations() {
        if (!animating) return
        val now = SystemClock.uptimeMillis()
        val dt = (now - lastFrameMs).coerceIn(0L, MAX_FRAME_MS).toFloat()
        lastFrameMs = now
        var settled = true
        keys.forEach { key ->
            val pressDuration = if (key.pressTarget > key.press) PRESS_ANIM_MS else RELEASE_ANIM_MS
            key.press = approach(key.press, key.pressTarget, dt, pressDuration)
            key.emphasis = approach(key.emphasis, key.emphasisTarget, dt, PRESS_ANIM_MS)
            key.grow = approach(key.grow, key.growTarget, dt, PRESS_ANIM_MS)
            if (key.emphasis == 0f && key.emphasisTarget == 0f && key.grow == 0f) key.target = null
            if (!key.settled()) settled = false
        }
        layerAlpha = approach(layerAlpha, 1f, dt, LAYER_FADE_MS)
        if (layerAlpha < 1f) settled = false
        if (settled) animating = false
    }

    private fun approach(
        value: Float,
        target: Float,
        dtMs: Float,
        durationMs: Float,
    ): Float {
        val step = dtMs / durationMs
        return if (value < target) min(target, value + step) else max(target, value - step)
    }

    // ------------------------------------------------------------------
    // Touch feedback (GestureIntegration.CellListener)
    // ------------------------------------------------------------------

    override fun onCellPressed(cellIndex: Int) {
        val key = keysByIndex[cellIndex] ?: return
        key.pressTarget = 1f
        key.press = max(key.press, PRESS_INSTANT) // visible on the very first frame
        key.target = Direction.CENTER
        key.emphasisTarget = 1f
        key.grow = 0f
        key.growTarget = 0f
        showBubble(key, Direction.CENTER)
        startAnimating()
    }

    override fun onDirectionChanged(
        cellIndex: Int,
        direction: Direction,
    ) {
        val key = keysByIndex[cellIndex] ?: return
        key.target = direction
        key.emphasisTarget = 1f
        key.grow = 0f
        key.growTarget = if (direction == Direction.CENTER) 0f else 1f
        showBubble(key, direction)
        startAnimating()
    }

    override fun onCellReleased(cellIndex: Int) {
        val key = keysByIndex[cellIndex] ?: return
        key.pressTarget = 0f
        key.emphasisTarget = 0f
        key.growTarget = 0f
        if (bubbleKey === key) hideBubble()
        startAnimating()
    }

    override fun onResizeStarted() {
        resizeStartHeightDp = layoutCalculator.getCurrentCellHeightDp()
        hideBubble()
    }

    override fun onResizeDrag(totalDyPx: Float) {
        // Dragging up makes every row taller; the drag is spread over the rows so the top
        // edge follows the finger.
        layoutCalculator.setCellHeightDp(resizeStartHeightDp - totalDyPx / (GRID_ROWS * density), persist = false)
        requestLayout()
    }

    override fun onResizeEnded() {
        layoutCalculator.commitLiveHeight()
        requestLayout()
    }

    private fun showBubble(
        key: Key,
        direction: Direction,
    ) {
        val gestureType = if (direction == Direction.CENTER) GestureType.TAP else GestureType.SWIPE
        val action = key.config.getActionFor(gestureType, direction)
        val text = key.label(direction)
        if (!previewEnabled || !isPreviewable(action) || text.isNullOrEmpty()) {
            if (bubbleKey === key || bubbleKey == null) hideBubble()
            return
        }
        bubbleKey = key
        bubbleText = text
        invalidate()
    }

    private fun hideBubble() {
        if (bubbleKey == null) return
        bubbleKey = null
        bubbleText = null
        invalidate()
    }

    private fun isPreviewable(action: ActionConfig?): Boolean =
        when (action) {
            is ActionConfig.Character, is ActionConfig.Text -> true
            is ActionConfig.Action -> action.keyAction is KeyAction.DeadKey
            else -> false
        }

    // ------------------------------------------------------------------
    // Touch routing
    // ------------------------------------------------------------------

    @SuppressLint("ClickableViewAccessibility") // accessibility goes through ExploreByTouchHelper
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> onPointerDown(event, event.actionIndex)
            MotionEvent.ACTION_MOVE -> onPointersMove(event)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> onPointerUp(event, event.actionIndex)
            MotionEvent.ACTION_CANCEL -> cancelTouches()
        }
        return true
    }

    private fun onPointerDown(
        event: MotionEvent,
        index: Int,
    ) {
        val pointerId = event.getPointerId(index)
        val x = event.getX(index)
        val y = event.getY(index)
        val inStrip = strip.contains(y)
        val slot = if (inStrip) strip.slotAt(x, y) else null
        val button = if (inStrip) null else panelButtonAt(x, y)
        val key = if (inStrip || button != null) null else keyAt(x, y)
        when {
            slot != null -> {
                stripPointers[pointerId] = slot
                tapHaptic()
            }
            button != null -> {
                panelPointers[pointerId] = button
                tapHaptic()
            }
            key != null -> startGesture(event, pointerId, key, x, y)
        }
    }

    private fun tapHaptic() {
        if (prefs.hapticsEnabled) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    private fun startGesture(
        event: MotionEvent,
        pointerId: Int,
        key: Key,
        x: Float,
        y: Float,
    ) {
        getLocationOnScreen(screenLocation)
        gesturePointers += pointerId
        gestures.pointerDown(
            view = this,
            pointerId = pointerId,
            cellIndex = key.index,
            x = x,
            y = y,
            screenY = y + screenLocation[1],
            time = event.eventTime,
            cellMinSizePx = min(key.rect.width(), key.rect.height()),
        )
    }

    private fun onPointersMove(event: MotionEvent) {
        if (gesturePointers.isEmpty()) return
        getLocationOnScreen(screenLocation)
        for (i in 0 until event.pointerCount) {
            val pointerId = event.getPointerId(i)
            if (pointerId in gesturePointers) {
                gestures.pointerMove(pointerId, event.getX(i), event.getY(i), event.getY(i) + screenLocation[1])
            }
        }
    }

    private fun onPointerUp(
        event: MotionEvent,
        index: Int,
    ) {
        val pointerId = event.getPointerId(index)
        val x = event.getX(index)
        val y = event.getY(index)
        panelPointers.remove(pointerId)?.let { button ->
            if (panelButtonAt(x, y) == button) onPanelButton(button)
            return
        }
        stripPointers.remove(pointerId)?.let { slot ->
            if (strip.slotAt(x, y) == slot) pickSlot(slot)
            return
        }
        if (gesturePointers.remove(pointerId)) gestures.pointerUp(pointerId, x, y, event.eventTime)
    }

    private fun pickSlot(index: Int) {
        suggestions.slots.getOrNull(index)?.let(onSuggestionPicked)
    }

    private fun cancelTouches() {
        gesturePointers.clear()
        panelPointers.clear()
        stripPointers.clear()
        gestures.cancelAll()
    }

    override fun onDetachedFromWindow() {
        cancelTouches()
        if (gestures.cellListener === this) gestures.cellListener = null
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        gestures.cellListener = this
    }

    /** Key under a point; points on the outer edge belong to the closest key. */
    private fun keyAt(
        x: Float,
        y: Float,
    ): Key? {
        val outside = x < gridRect.left || x > gridRect.right || y < gridRect.top
        if (gridRect.isEmpty || outside) return null
        val cx = x.coerceIn(gridRect.left, gridRect.right - EDGE_EPSILON)
        val cy = y.coerceIn(gridRect.top, gridRect.bottom - EDGE_EPSILON)
        return keys.firstOrNull { it.rect.contains(cx, cy) }
    }

    private fun panelButtonAt(
        x: Float,
        y: Float,
    ): PanelButton? {
        if (panelRect.isEmpty || !panelRect.contains(x, y.coerceAtMost(height - EDGE_EPSILON))) return null
        return if (y < panelRect.centerY()) PanelButton.SWAP_SIDE else PanelButton.FULL_WIDTH
    }

    private fun panelButtonRect(
        button: PanelButton,
        out: RectF,
    ) {
        val half = panelRect.height() / 2
        val top = panelRect.top + half * button.ordinal
        out.set(panelRect.left, top, panelRect.right, top + half)
    }

    private fun onPanelButton(button: PanelButton) {
        when (button) {
            PanelButton.SWAP_SIDE -> prefs.oneHandRight = !prefs.oneHandRight
            PanelButton.FULL_WIDTH -> prefs.oneHandEnabled = false
        }
        onGeometryChanged()
    }

    // ------------------------------------------------------------------
    // Accessibility
    // ------------------------------------------------------------------

    override fun dispatchHoverEvent(event: MotionEvent): Boolean =
        accessibilityHelper.dispatchHoverEvent(event) || super.dispatchHoverEvent(event)

    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        accessibilityHelper.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)

    override fun onFocusChanged(
        gainFocus: Boolean,
        direction: Int,
        previouslyFocusedRect: Rect?,
    ) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        accessibilityHelper.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
    }

    private inner class AccessibilityHelper : ExploreByTouchHelper(this@KeyboardView) {
        override fun getVirtualViewAt(
            x: Float,
            y: Float,
        ): Int =
            strip.slotAt(x, y)?.let { STRIP_VIRTUAL_ID_BASE + it }
                ?: panelButtonAt(x, y)?.virtualId
                ?: keyAt(x, y)?.index
                ?: INVALID_ID

        override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>) {
            suggestions.slots.forEachIndexed {
                    index,
                    slot,
                ->
                if (slot != null) virtualViewIds += STRIP_VIRTUAL_ID_BASE + index
            }
            keys.forEach { virtualViewIds += it.index }
            if (!panelRect.isEmpty) PanelButton.values().forEach { virtualViewIds += it.virtualId }
        }

        @Suppress("DEPRECATION") // ExploreByTouchHelper still requires setBoundsInParent
        override fun onPopulateNodeForVirtualView(
            virtualViewId: Int,
            node: AccessibilityNodeInfoCompat,
        ) {
            val key = keysByIndex[virtualViewId]
            val button = panelButtonFor(virtualViewId)
            val slot = slotFor(virtualViewId)
            when {
                slot != null -> {
                    node.contentDescription = GestureHintProvider.describeSuggestion(slot)
                    strip.slotRect(virtualViewId - STRIP_VIRTUAL_ID_BASE, drawRect)
                    drawRect.roundOut(boundsRect)
                }
                key != null -> {
                    node.contentDescription = labels.describe(key.config)
                    key.rect.roundOut(boundsRect)
                    key.swipeDirections.forEach { direction ->
                        node.addAction(
                            AccessibilityActionCompat(
                                swipeActionId(direction),
                                labels.describeSwipe(key.config, direction),
                            ),
                        )
                    }
                }
                button != null -> {
                    node.contentDescription = button.description
                    panelButtonRect(button, drawRect)
                    drawRect.roundOut(boundsRect)
                }
                else -> {
                    node.contentDescription = ""
                    boundsRect.setEmpty()
                }
            }
            if (boundsRect.isEmpty) boundsRect.set(0, 0, 1, 1)
            node.className = Button::class.java.name
            node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
            node.setBoundsInParent(boundsRect)
        }

        override fun onPerformActionForVirtualView(
            virtualViewId: Int,
            action: Int,
            arguments: Bundle?,
        ): Boolean {
            val button = panelButtonFor(virtualViewId)
            val isClick = action == AccessibilityNodeInfoCompat.ACTION_CLICK
            val direction =
                if (isClick) Direction.CENTER else SWIPE_ACTIONS.entries.firstOrNull { it.value == action }?.key
            return when {
                slotFor(virtualViewId) != null && isClick -> {
                    pickSlot(virtualViewId - STRIP_VIRTUAL_ID_BASE)
                    true
                }
                button != null && isClick -> {
                    onPanelButton(button)
                    true
                }
                button == null && direction != null && keysByIndex.containsKey(virtualViewId) -> {
                    gestures.perform(virtualViewId, direction)
                    true
                }
                else -> false
            }
        }

        private fun slotFor(virtualViewId: Int): Suggestion? =
            if (virtualViewId < STRIP_VIRTUAL_ID_BASE) {
                null
            } else {
                suggestions.slots.getOrNull(
                    virtualViewId - STRIP_VIRTUAL_ID_BASE,
                )
            }

        private fun panelButtonFor(virtualViewId: Int) =
            if (panelRect.isEmpty) null else PanelButton.values().firstOrNull { it.virtualId == virtualViewId }

        private fun swipeActionId(direction: Direction) = SWIPE_ACTIONS.getValue(direction)
    }

    private fun textPaint(sizeSp: Float) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sizeSp, resources.displayMetrics)
        }

    private companion object {
        const val GRID_ROWS = 4
        const val GRID_COLUMNS = 4
        const val BORDER_DP = 1f
        const val LABEL_MARGIN_DP = 3f
        const val CENTER_TEXT_SP = 16f
        const val LABEL_TEXT_SP = 11f
        const val LABEL_ALPHA = 0.85f
        const val PRESS_SCALE = 0.96f
        const val PRESS_INSTANT = 0.6f
        const val TARGET_SCALE = 1.4f
        const val DIMMED_ALPHA = 0.35f
        const val PRESS_ANIM_MS = 60f
        const val RELEASE_ANIM_MS = 80f
        const val LAYER_FADE_MS = 100f
        const val MAX_FRAME_MS = 32L
        const val OPAQUE = 255
        const val BUBBLE_SIZE_DP = 52f
        const val BUBBLE_TEXT_SP = 26f
        const val BUBBLE_RADIUS_DP = 12f
        const val BUBBLE_OVERLAP_DIVISOR = 4
        const val ONE_HAND_WIDTH = 0.8f
        const val PANEL_TEXT_SP = 22f
        const val PANEL_VIRTUAL_ID_BASE = 100
        const val EDGE_EPSILON = 0.5f
        const val STRIP_VIRTUAL_ID_BASE = 200

        val SWIPE_ACTIONS =
            mapOf(
                Direction.N to R.id.a11y_swipe_n,
                Direction.NE to R.id.a11y_swipe_ne,
                Direction.E to R.id.a11y_swipe_e,
                Direction.SE to R.id.a11y_swipe_se,
                Direction.S to R.id.a11y_swipe_s,
                Direction.SW to R.id.a11y_swipe_sw,
                Direction.W to R.id.a11y_swipe_w,
                Direction.NW to R.id.a11y_swipe_nw,
            )
    }
}
