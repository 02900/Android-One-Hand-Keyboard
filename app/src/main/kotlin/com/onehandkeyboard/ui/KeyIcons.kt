package com.onehandkeyboard.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.onehandkeyboard.R

/**
 * Vector icons drawn instead of the symbol labels of control keys (emoji look different on
 * every phone and don't follow the theme). The labels stay the single source of truth
 * (ActionLabels): this only decides how a label is painted.
 */
class KeyIcons(
    private val context: Context,
) {
    private val cache = HashMap<Int, Drawable>()

    fun has(label: String): Boolean = label in ICONS

    /** Draws the icon for [label] centered on ([cx], [cy]); false if the label has no icon. */
    @Suppress("LongParameterList")
    fun draw(
        canvas: Canvas,
        label: String,
        cx: Float,
        cy: Float,
        size: Float,
        color: Int,
    ): Boolean {
        val res = ICONS[label] ?: return false
        val icon = cache.getOrPut(res) { checkNotNull(ContextCompat.getDrawable(context, res)).mutate() }
        val half = size / 2
        icon.setBounds((cx - half).toInt(), (cy - half).toInt(), (cx + half).toInt(), (cy + half).toInt())
        icon.setTint(color)
        icon.draw(canvas)
        return true
    }

    companion object {
        /** Label (see ActionLabels and GestureHintProvider.enterLabelFor) → icon. */
        val ICONS: Map<String, Int> =
            mapOf(
                "⌫" to R.drawable.ic_key_backspace,
                "␣" to R.drawable.ic_key_space,
                "↵" to R.drawable.ic_key_enter,
                "🔍" to R.drawable.ic_key_search,
                "→" to R.drawable.ic_key_go,
                "⇥" to R.drawable.ic_key_next,
                "⇤" to R.drawable.ic_key_previous,
                "✓" to R.drawable.ic_key_done,
                "➤" to R.drawable.ic_key_send,
                "📤" to R.drawable.ic_key_send,
                "📋" to R.drawable.ic_key_paste,
                "📄" to R.drawable.ic_key_copy,
                "✂" to R.drawable.ic_key_cut,
                "🗑️" to R.drawable.ic_key_delete,
                "↶" to R.drawable.ic_key_undo,
                "🖼️" to R.drawable.ic_key_memes,
                "◂" to R.drawable.ic_key_cursor_left,
                "▸" to R.drawable.ic_key_cursor_right,
                "⇧" to R.drawable.ic_key_shift,
                "⬆" to R.drawable.ic_key_shift_on,
                "⇪" to R.drawable.ic_key_caps_lock,
            )
    }
}
