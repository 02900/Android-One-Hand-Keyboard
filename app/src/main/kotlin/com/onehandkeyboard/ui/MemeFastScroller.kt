package com.onehandkeyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.onehandkeyboard.memes.MemeSections

/**
 * Fast scroll for the meme list: a thin thumb on the right edge that follows the scroll and
 * can be dragged; while dragging, a bubble next to it shows the category being passed.
 *
 * It lies over the whole list but only takes touches that start on the right edge, so the
 * rest of the list scrolls and taps as usual.
 */
@SuppressLint("ViewConstructor") // created in code by MemePanelView
class MemeFastScroller(
    context: Context,
    private val list: RecyclerView,
) : View(context) {
    /** Name of the category of the row at a position (the bubble's text). */
    var sectionNameAt: (Int) -> String? = { null }

    private val density = resources.displayMetrics.density
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = BUBBLE_TEXT_SP * resources.displayMetrics.scaledDensity
            typeface = Typeface.DEFAULT_BOLD
        }
    private val rect = RectF()

    /** Thumb position while dragging (0..1), or null when following the list. */
    private var dragFraction: Float? = null
    private var announced: String? = null

    val isDragging: Boolean get() = dragFraction != null

    init {
        list.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(
                    recyclerView: RecyclerView,
                    dx: Int,
                    dy: Int,
                ) = invalidate()
            },
        )
    }

    fun setColors(
        thumb: Int,
        bubble: Int,
        text: Int,
    ) {
        thumbPaint.color = thumb
        bubblePaint.color = bubble
        textPaint.color = text
        invalidate()
    }

    private fun scrollable() = list.computeVerticalScrollRange() > list.computeVerticalScrollExtent()

    private fun listFraction(): Float {
        val range = list.computeVerticalScrollRange() - list.computeVerticalScrollExtent()
        return if (range <= 0) 0f else (list.computeVerticalScrollOffset().toFloat() / range).coerceIn(0f, 1f)
    }

    private fun thumbHeight() = dp(THUMB_HEIGHT_DP)

    private fun thumbTop(fraction: Float) = fraction * (height - thumbHeight())

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!scrollable()) return
        val fraction = dragFraction ?: listFraction()
        val top = thumbTop(fraction)
        val thumbWidth = dp(if (isDragging) THUMB_WIDTH_DRAGGING_DP else THUMB_WIDTH_DP)
        val right = width - dp(EDGE_MARGIN_DP)
        rect.set(right - thumbWidth, top, right, top + thumbHeight())
        canvas.drawRoundRect(rect, thumbWidth / 2, thumbWidth / 2, thumbPaint)
        if (isDragging) drawBubble(canvas, top + thumbHeight() / 2)
    }

    private fun drawBubble(
        canvas: Canvas,
        centerY: Float,
    ) {
        val name = sectionNameAt(currentPosition()) ?: return
        val padding = dp(BUBBLE_PADDING_DP)
        val bubbleHeight = dp(BUBBLE_HEIGHT_DP)
        val textWidth = textPaint.measureText(name)
        val right = width - dp(BUBBLE_GAP_DP)
        val left = (right - textWidth - 2 * padding).coerceAtLeast(0f)
        val top = (centerY - bubbleHeight / 2).coerceIn(0f, (height - bubbleHeight).coerceAtLeast(0f))
        rect.set(left, top, right, top + bubbleHeight)
        canvas.drawRoundRect(rect, bubbleHeight / 2, bubbleHeight / 2, bubblePaint)
        val baseline = rect.centerY() - (textPaint.ascent() + textPaint.descent()) / 2
        canvas.drawText(name, left + padding, baseline, textPaint)
    }

    private fun currentPosition(): Int {
        val count = list.adapter?.itemCount ?: 0
        return MemeSections.positionForFraction(dragFraction ?: listFraction(), count)
    }

    @SuppressLint("ClickableViewAccessibility") // the list itself stays accessible; this only speeds up dragging
    override fun onTouchEvent(event: MotionEvent): Boolean =
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN ->
                (scrollable() && event.x >= width - dp(TOUCH_WIDTH_DP)).also { onEdge ->
                    if (onEdge) {
                        parent?.requestDisallowInterceptTouchEvent(true)
                        dragTo(event.y)
                    }
                }
            MotionEvent.ACTION_MOVE -> isDragging.also { if (it) dragTo(event.y) }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> isDragging.also { if (it) stopDrag() }
            else -> isDragging
        }

    private fun stopDrag() {
        dragFraction = null
        announced = null
        invalidate()
    }

    private fun dragTo(y: Float) {
        val travel = (height - thumbHeight()).coerceAtLeast(1f)
        val fraction = ((y - thumbHeight() / 2) / travel).coerceIn(0f, 1f)
        dragFraction = fraction
        val position = currentPosition()
        (list.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(position, 0)
        val name = sectionNameAt(position)
        if (name != null && name != announced) {
            announced = name
            announceForAccessibility(name)
        }
        invalidate()
    }

    private fun dp(value: Float) = value * density

    private companion object {
        const val THUMB_HEIGHT_DP = 44f
        const val THUMB_WIDTH_DP = 4f
        const val THUMB_WIDTH_DRAGGING_DP = 8f
        const val EDGE_MARGIN_DP = 3f
        const val TOUCH_WIDTH_DP = 28f
        const val BUBBLE_HEIGHT_DP = 40f
        const val BUBBLE_PADDING_DP = 16f
        const val BUBBLE_GAP_DP = 24f
        const val BUBBLE_TEXT_SP = 16f
    }
}
