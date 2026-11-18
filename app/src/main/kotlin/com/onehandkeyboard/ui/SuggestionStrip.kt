package com.onehandkeyboard.ui

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.TypedValue
import androidx.core.graphics.ColorUtils
import com.onehandkeyboard.spell.Suggestion
import com.onehandkeyboard.spell.Suggestions

/**
 * The suggestion strip drawn by [KeyboardView] above the keys: three slots (left, center,
 * right). The center one is what the next space will commit; the word as typed (or the word
 * an undo would restore) is shown between quotes, the pending correction in bold.
 */
internal class SuggestionStrip(
    resources: Resources,
) {
    private val density = resources.displayMetrics.density
    private val paint = textPaint(resources)
    private val boldPaint = textPaint(resources).apply { typeface = Typeface.DEFAULT_BOLD }
    private val metrics = paint.fontMetrics
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val slotRect = RectF()
    private val texts = arrayOfNulls<String>(SLOTS)

    val rect = RectF()

    var suggestions: Suggestions = Suggestions.EMPTY
        set(value) {
            field = value
            updateTexts()
        }

    /** A status line shown across the strip instead of the suggestions (e.g. dictation). */
    var status: String? = null

    val isEmpty: Boolean get() = rect.isEmpty

    fun heightPx(enabled: Boolean): Float = if (enabled) HEIGHT_DP * density else 0f

    fun layout(
        width: Float,
        height: Float,
    ) {
        if (height > 0f) rect.set(0f, 0f, width, height) else rect.setEmpty()
        updateTexts()
    }

    /** True if [y] falls on the strip (it spans the whole width). */
    fun contains(y: Float): Boolean = !rect.isEmpty && y < rect.bottom

    /** Index of the non-empty slot under the point, if any. */
    fun slotAt(
        x: Float,
        y: Float,
    ): Int? {
        if (rect.isEmpty || status != null || !rect.contains(x, y)) return null
        val index = ((x - rect.left) / (rect.width() / SLOTS)).toInt().coerceIn(0, SLOTS - 1)
        return index.takeIf { suggestions.slots.getOrNull(it) != null }
    }

    fun slotRect(
        index: Int,
        out: RectF,
    ) {
        val slotWidth = rect.width() / SLOTS
        out.set(rect.left + index * slotWidth, rect.top, rect.left + (index + 1) * slotWidth, rect.bottom)
    }

    fun draw(
        canvas: Canvas,
        palette: KeyboardPalette,
        border: Float,
    ) {
        if (rect.isEmpty) return
        fillPaint.color = palette.secondaryFill
        canvas.drawRect(rect.left, rect.top, rect.right, rect.bottom - border, fillPaint)
        status?.let { text ->
            boldPaint.color = palette.secondaryText
            canvas.drawText(text, rect.centerX(), rect.centerY() - (metrics.ascent + metrics.descent) / 2, boldPaint)
            return
        }
        for (index in 0 until SLOTS) {
            val text = texts[index]
            val slot = suggestions.slots.getOrNull(index)
            if (text != null && slot != null) drawSlot(canvas, palette, index, text, slot, border)
        }
    }

    @Suppress("LongParameterList")
    private fun drawSlot(
        canvas: Canvas,
        palette: KeyboardPalette,
        index: Int,
        text: String,
        slot: Suggestion,
        border: Float,
    ) {
        slotRect(index, slotRect)
        val textPaint = if (slot.kind == Suggestion.Kind.AUTO) boldPaint else paint
        textPaint.color = palette.secondaryText
        val baseline = slotRect.centerY() - (metrics.ascent + metrics.descent) / 2
        canvas.drawText(text, slotRect.centerX(), baseline, textPaint)
        if (index > 0 && suggestions.slots.getOrNull(index - 1) != null) {
            fillPaint.color = ColorUtils.setAlphaComponent(palette.secondaryText, DIVIDER_ALPHA)
            val inset = slotRect.height() * DIVIDER_INSET
            canvas.drawRect(
                slotRect.left,
                slotRect.top + inset,
                slotRect.left + border,
                slotRect.bottom - inset,
                fillPaint,
            )
        }
    }

    /** Display text of each slot, ellipsized to its width (computed outside onDraw). */
    private fun updateTexts() {
        val available = rect.width() / SLOTS - 2 * PADDING_DP * density
        for (index in 0 until SLOTS) {
            texts[index] = suggestions.slots.getOrNull(index)?.let { displayText(it, available) }
        }
    }

    private fun displayText(
        slot: Suggestion,
        available: Float,
    ): String {
        val quoted = slot.kind == Suggestion.Kind.TYPED || slot.kind == Suggestion.Kind.REVERT
        val text = if (quoted) "“${slot.text}”" else slot.text
        val textPaint = if (slot.kind == Suggestion.Kind.AUTO) boldPaint else paint
        if (available <= 0f || textPaint.measureText(text) <= available) return text
        return text.take(textPaint.breakText(text, true, available - textPaint.measureText(ELLIPSIS), null)) + ELLIPSIS
    }

    private fun textPaint(resources: Resources) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, TEXT_SP, resources.displayMetrics)
        }

    companion object {
        const val SLOTS = 3
        private const val HEIGHT_DP = 40f
        private const val TEXT_SP = 16f
        private const val PADDING_DP = 6f
        private const val DIVIDER_ALPHA = 0x40
        private const val DIVIDER_INSET = 0.25f
        private const val ELLIPSIS = "…"
    }
}
