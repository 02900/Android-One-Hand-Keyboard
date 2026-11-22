package com.onehandkeyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.InputType
import android.text.Selection
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils

/**
 * The meme search box, shown above the keys while the query is typed.
 *
 * The keyboard's keys write into it through [connection] instead of into the app's field; the
 * Enter key (or the search button) runs [onSearch] and ✕ gives up ([onCancel]).
 */
@SuppressLint("ViewConstructor") // created in code by the service
class MemeSearchBar(
    context: Context,
) : LinearLayout(context) {
    var onSearch: (query: String) -> Unit = {}
    var onCancel: () -> Unit = {}

    /** What the keys type into while the box is shown. */
    val connection: InputConnection = QueryConnection()

    /** A search field: Enter searches, no autocorrection or double-space period. */
    val editorInfo =
        EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_FILTER
            imeOptions = EditorInfo.IME_ACTION_SEARCH
        }

    private val density = resources.displayMetrics.density
    private val query: Editable = Editable.Factory.getInstance().newEditable("")
    private val cancel = button("✕", "Cancelar búsqueda") { onCancel() }
    private val field = TextView(context)
    private val go = button("🔍", "Buscar") { search() }
    private var palette: KeyboardPalette? = null

    /** The query as typed. */
    val text: String get() = query.toString()

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val padding = dp(PADDING_DP)
        setPadding(padding, padding, padding, padding)
        addView(cancel, LayoutParams(dp(BUTTON_DP), LayoutParams.MATCH_PARENT))
        field.gravity = Gravity.CENTER_VERTICAL
        field.maxLines = 1
        field.setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SP)
        field.setPadding(dp(FIELD_PADDING_DP), 0, dp(FIELD_PADDING_DP), 0)
        addView(field, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        addView(go, LayoutParams(dp(BUTTON_DP), LayoutParams.MATCH_PARENT))
        render()
    }

    /** Opens the box with [initial] typed (the last query, to edit it). */
    fun start(initial: String) {
        query.replace(0, query.length, initial)
        BaseInputConnection.removeComposingSpans(query)
        Selection.setSelection(query, query.length)
        render()
    }

    fun applyPalette(palette: KeyboardPalette) {
        this.palette = palette
        setBackgroundColor(palette.secondaryFill)
        cancel.setTextColor(palette.secondaryText)
        go.setTextColor(palette.secondaryText)
        field.background =
            GradientDrawable().apply {
                setColor(palette.root)
                cornerRadius = dp(CORNER_DP).toFloat()
            }
        render()
    }

    private fun search() {
        val text = query.toString().trim()
        if (text.isNotEmpty()) onSearch(text)
    }

    /** Shows the query with a cursor, or a hint when it is empty. */
    private fun render() {
        val colors = palette
        if (query.isEmpty()) {
            field.text = HINT
            colors?.let { field.setTextColor(ColorUtils.setAlphaComponent(it.secondaryText, HINT_ALPHA)) }
        } else {
            val cursor = Selection.getSelectionEnd(query).coerceIn(0, query.length)
            field.text = StringBuilder(query).insert(cursor, CURSOR)
            colors?.let { field.setTextColor(it.secondaryText) }
        }
        field.contentDescription = if (query.isEmpty()) HINT else "Buscar: $query"
    }

    private fun button(
        text: String,
        description: String,
        onClick: () -> Unit,
    ) = TextView(context).apply {
        this.text = text
        contentDescription = description
        gravity = Gravity.CENTER
        setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SP)
        isClickable = true
        isFocusable = true
        setOnClickListener { onClick() }
    }

    private fun dp(value: Float) = (value * density).toInt()

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(dp(HEIGHT_DP), MeasureSpec.EXACTLY))
    }

    /** Edits [query] like a text field would, and repaints the box after each change. */
    private inner class QueryConnection : BaseInputConnection(this@MemeSearchBar, true) {
        override fun getEditable(): Editable = query

        override fun commitText(
            text: CharSequence?,
            newCursorPosition: Int,
        ) = super.commitText(text, newCursorPosition).also { render() }

        override fun setComposingText(
            text: CharSequence?,
            newCursorPosition: Int,
        ) = super.setComposingText(text, newCursorPosition).also { render() }

        override fun finishComposingText() = super.finishComposingText().also { render() }

        override fun deleteSurroundingText(
            beforeLength: Int,
            afterLength: Int,
        ) = super.deleteSurroundingText(beforeLength, afterLength).also { render() }

        override fun setSelection(
            start: Int,
            end: Int,
        ) = super.setSelection(start, end).also { render() }

        override fun endBatchEdit() = super.endBatchEdit().also { render() }

        override fun performEditorAction(actionCode: Int): Boolean {
            search()
            return true
        }

        /** The keyboard sends Delete, Enter and the arrows as key events. */
        override fun sendKeyEvent(event: KeyEvent): Boolean {
            if (event.action != KeyEvent.ACTION_DOWN) return true
            val start = Selection.getSelectionStart(query).coerceIn(0, query.length)
            val end = Selection.getSelectionEnd(query).coerceIn(0, query.length)
            when (event.keyCode) {
                KeyEvent.KEYCODE_DEL ->
                    when {
                        start != end -> query.delete(minOf(start, end), maxOf(start, end))
                        end > 0 -> query.delete(Character.offsetByCodePoints(query, end, -1), end)
                    }
                KeyEvent.KEYCODE_ENTER -> search()
                KeyEvent.KEYCODE_DPAD_LEFT ->
                    if (end > 0) Selection.setSelection(query, Character.offsetByCodePoints(query, end, -1))
                KeyEvent.KEYCODE_DPAD_RIGHT ->
                    if (end < query.length) Selection.setSelection(query, Character.offsetByCodePoints(query, end, 1))
            }
            render()
            return true
        }
    }

    companion object {
        const val HEIGHT_DP = 48f
        private const val HINT = "Buscar en Know Your Meme"
        private const val CURSOR = "│"
        private const val HINT_ALPHA = 0x99
        private const val PADDING_DP = 4f
        private const val BUTTON_DP = 48f
        private const val FIELD_PADDING_DP = 12f
        private const val TEXT_SP = 16f
        private const val CORNER_DP = 10f
    }
}
