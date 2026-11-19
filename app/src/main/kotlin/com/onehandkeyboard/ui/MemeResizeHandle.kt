package com.onehandkeyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.math.roundToInt

/**
 * The grab bar on the meme panel's top edge: drag it up to make the panel taller, down to
 * make it shorter. It reports the height the panel should have; the service limits it.
 *
 * The finger is tracked in screen coordinates because the handle itself moves as the panel
 * (and the keyboard window) grows. TalkBack can resize it with the scroll actions.
 */
@SuppressLint("ViewConstructor") // created in code by MemePanelView
class MemeResizeHandle(
    context: Context,
    /** Current height of the panel being resized. */
    private val panelHeight: () -> Int,
) : View(context) {
    /** Height the panel should take; [final] when the finger is lifted. */
    var onHeightDragged: (heightPx: Int, final: Boolean) -> Unit = { _, _ -> }

    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pill = RectF()
    private var downRawY = 0f
    private var startHeight = 0
    private var dragging = false

    init {
        contentDescription = "Tamaño del panel: arrastra hacia arriba o abajo"
        isFocusable = true
    }

    fun setColor(color: Int) {
        paint.color = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = dp(if (dragging) PILL_WIDTH_DRAGGING_DP else PILL_WIDTH_DP)
        val height = dp(PILL_HEIGHT_DP)
        pill.set(
            (this.width - width) / 2,
            (this.height - height) / 2,
            (this.width + width) / 2,
            (this.height + height) / 2,
        )
        canvas.drawRoundRect(pill, height / 2, height / 2, paint)
    }

    @SuppressLint("ClickableViewAccessibility") // TalkBack resizes it with the scroll actions below
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                downRawY = event.rawY
                startHeight = panelHeight()
                dragging = true
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> report(event, final = false)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                report(event, final = true)
                dragging = false
                invalidate()
            }
        }
        return true
    }

    /** Moving the finger up (smaller rawY) makes the panel taller. */
    private fun report(
        event: MotionEvent,
        final: Boolean,
    ) {
        if (dragging) onHeightDragged((startHeight + downRawY - event.rawY).roundToInt(), final)
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
        return when (action) {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD -> true.also { onHeightDragged(panelHeight() + step, true) }
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD -> true.also { onHeightDragged(panelHeight() - step, true) }
            else -> super.performAccessibilityAction(action, arguments)
        }
    }

    private fun dp(value: Float) = value * density

    companion object {
        const val HEIGHT_DP = 18f
        private const val PILL_WIDTH_DP = 40f
        private const val PILL_WIDTH_DRAGGING_DP = 64f
        private const val PILL_HEIGHT_DP = 4f
        private const val ACCESSIBILITY_STEP_DP = 80f
    }
}
