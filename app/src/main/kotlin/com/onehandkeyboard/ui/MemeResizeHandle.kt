package com.onehandkeyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.math.roundToInt

/**
 * The grab bar on the meme panel's top edge: drag it up to make the panel taller, down to
 * make it shorter. The panel keeps its size while the finger is down (the bar shows the size it
 * will take) and changes once, when the finger is lifted ([onHeightChosen]).
 *
 * The finger is tracked in screen coordinates, so it keeps working when it leaves the keyboard
 * window. TalkBack resizes it with the scroll actions.
 */
@SuppressLint("ViewConstructor") // created in code by MemePanelView
class MemeResizeHandle(
    context: Context,
    /** Current height of the panel being resized. */
    private val panelHeight: () -> Int,
) : View(context) {
    /** The height the panel should take, when the finger is lifted (already within [heightRange]). */
    var onHeightChosen: (heightPx: Int) -> Unit = {}

    /** Heights the panel can take: from the keys' height to the tallest allowed. */
    var heightRange: IntRange = 0..Int.MAX_VALUE

    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = LABEL_TEXT_SP * resources.displayMetrics.scaledDensity
            typeface = Typeface.DEFAULT_BOLD
        }
    private val pill = RectF()
    private var downRawY = 0f
    private var startHeight = 0

    /** Height the panel will take when released, or null when not dragging. */
    private var target: Int? = null

    init {
        contentDescription = "Tamaño del panel: arrastra hacia arriba o abajo y suelta"
        isFocusable = true
    }

    fun setColor(color: Int) {
        paint.color = color
        textPaint.color = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val dragged = target
        val width = dp(if (dragged != null) PILL_WIDTH_DRAGGING_DP else PILL_WIDTH_DP)
        val height = dp(PILL_HEIGHT_DP)
        pill.set(
            (this.width - width) / 2,
            (this.height - height) / 2,
            (this.width + width) / 2,
            (this.height + height) / 2,
        )
        canvas.drawRoundRect(pill, height / 2, height / 2, paint)
        if (dragged != null) {
            val baseline = this.height / 2f - (textPaint.ascent() + textPaint.descent()) / 2
            canvas.drawText(label(dragged), pill.right + dp(LABEL_GAP_DP), baseline, textPaint)
        }
    }

    /** "▲ 45 %": where the size is going (0 % = the keyboard's height, 100 % = the tallest). */
    internal fun label(height: Int): String {
        val span = (heightRange.last - heightRange.first).coerceAtLeast(1)
        val percent = ((height - heightRange.first) * PERCENT / span).coerceIn(0, PERCENT)
        val arrow =
            when {
                height > startHeight -> "▲ "
                height < startHeight -> "▼ "
                else -> ""
            }
        return "$arrow$percent %"
    }

    @SuppressLint("ClickableViewAccessibility") // TalkBack resizes it with the scroll actions below
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                downRawY = event.rawY
                startHeight = panelHeight()
                target = startHeight
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> if (target != null) target = heightFor(event).also { invalidate() }
            MotionEvent.ACTION_UP -> {
                if (target != null) {
                    val chosen = heightFor(event)
                    if (chosen != startHeight) onHeightChosen(chosen)
                }
                endDrag()
            }
            MotionEvent.ACTION_CANCEL -> endDrag()
        }
        return true
    }

    /** Moving the finger up (smaller rawY) makes the panel taller. */
    private fun heightFor(event: MotionEvent): Int =
        (startHeight + downRawY - event.rawY).roundToInt().coerceIn(heightRange.first, heightRange.last)

    private fun endDrag() {
        target = null
        invalidate()
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD)
    }

    override fun performAccessibilityAction(
        action: Int,
        arguments: Bundle?,
    ): Boolean {
        val step = dp(ACCESSIBILITY_STEP_DP).roundToInt()
        val delta =
            when (action) {
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD -> step
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD -> -step
                else -> return super.performAccessibilityAction(action, arguments)
            }
        onHeightChosen((panelHeight() + delta).coerceIn(heightRange.first, heightRange.last))
        return true
    }

    private fun dp(value: Float) = value * density

    companion object {
        const val HEIGHT_DP = 22f
        private const val PILL_WIDTH_DP = 40f
        private const val PILL_WIDTH_DRAGGING_DP = 64f
        private const val PILL_HEIGHT_DP = 4f
        private const val LABEL_GAP_DP = 10f
        private const val LABEL_TEXT_SP = 12f
        private const val PERCENT = 100
        private const val ACCESSIBILITY_STEP_DP = 80f
    }
}
