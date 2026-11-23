package com.onehandkeyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.onehandkeyboard.R
import com.onehandkeyboard.emoji.EmojiGroup
import com.onehandkeyboard.emoji.EmojiKitchen
import com.onehandkeyboard.emoji.EmojiMix
import com.onehandkeyboard.memes.Meme
import com.onehandkeyboard.memes.MemeThumbnails
import kotlin.math.max

/**
 * Emoji panel shown in place of the keys.
 *
 * - Bar: back to the keys, one tab per group (tap to jump to it) and a delete key.
 * - Tap an emoji to type it. With [kitchen] (builds with Internet), the strip under the bar
 *   offers the Emoji Kitchen mix of the last two emojis tapped; tapping it sends the mix as a
 *   sticker ([onMixPicked]).
 */
@SuppressLint("ViewConstructor") // created in code by the service, never inflated
class EmojiPanelView(
    context: Context,
    private val thumbnails: MemeThumbnails,
) : LinearLayout(context) {
    /** Back to the keys. */
    var onBack: () -> Unit = {}

    /** An emoji was tapped: type it. */
    var onEmojiPicked: (String) -> Unit = {}

    /** The delete key. */
    var onBackspace: () -> Unit = {}

    /** The mix in the strip was tapped: send it. */
    var onMixPicked: (EmojiMix) -> Unit = {}

    /** Which emojis mix; null hides the strip (builds without Internet, or not loaded yet). */
    var kitchen: EmojiKitchen? = null
        set(value) {
            field = value
            mixStrip.visibility = if (value != null) VISIBLE else GONE
            updateMix()
        }

    private val density = resources.displayMetrics.density
    private var palette: KeyboardPalette? = null
    private var rows: List<Row> = emptyList()

    /** Position of each group's title in [rows]. */
    private var headers: List<Int> = emptyList()

    /** The last emojis tapped (at most two), the ones the strip mixes. */
    private val picked = ArrayList<String>(2)

    /** The mix the strip offers, or null. */
    var mix: EmojiMix? = null
        private set

    private val bar = LinearLayout(context)
    private val backButton = textButton("ABC") { onBack() }
    private val tabs = LinearLayout(context)
    private val tabScroll = HorizontalScrollView(context)
    private var tabViews: List<TextView> = emptyList()
    private var activeTab = -1
    private val deleteButton = ImageView(context)
    private val mixStrip = LinearLayout(context)
    private val mixImage = ImageView(context)
    private val mixText = TextView(context)
    private val adapter = EmojiAdapter()
    private val layoutManager = GridLayoutManager(context, MIN_COLUMNS)
    private val grid = RecyclerView(context)

    init {
        orientation = VERTICAL
        bar.orientation = HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.addView(backButton, LayoutParams(dp(BACK_WIDTH_DP), LayoutParams.MATCH_PARENT))
        tabs.orientation = HORIZONTAL
        tabScroll.isHorizontalScrollBarEnabled = false
        tabScroll.addView(tabs, ViewGroup.LayoutParams(WRAP_CONTENT, MATCH_PARENT))
        bar.addView(tabScroll, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        deleteButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_key_backspace)?.mutate())
        deleteButton.scaleType = ImageView.ScaleType.CENTER_INSIDE
        deleteButton.setPadding(dp(DELETE_PADDING_DP), 0, dp(DELETE_PADDING_DP), 0)
        deleteButton.contentDescription = "borrar"
        deleteButton.isClickable = true
        deleteButton.isFocusable = true
        deleteButton.setOnClickListener { delete() }
        bar.addView(deleteButton, LayoutParams(dp(DELETE_WIDTH_DP), LayoutParams.MATCH_PARENT))
        addView(bar, LayoutParams(LayoutParams.MATCH_PARENT, dp(BAR_HEIGHT_DP)))

        mixStrip.orientation = HORIZONTAL
        mixStrip.gravity = Gravity.CENTER_VERTICAL
        mixStrip.setPadding(dp(MIX_PADDING_DP), 0, dp(MIX_PADDING_DP), 0)
        mixStrip.visibility = GONE
        mixStrip.setOnClickListener { mix?.let(::sendMix) }
        mixImage.scaleType = ImageView.ScaleType.FIT_CENTER
        mixStrip.addView(mixImage, LayoutParams(dp(MIX_IMAGE_DP), dp(MIX_IMAGE_DP)))
        mixText.setTextSize(TypedValue.COMPLEX_UNIT_SP, MIX_TEXT_SP)
        mixText.maxLines = 1
        mixText.ellipsize = android.text.TextUtils.TruncateAt.END
        mixText.setPadding(dp(MIX_PADDING_DP), 0, 0, 0)
        mixStrip.addView(mixText, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        addView(mixStrip, LayoutParams(LayoutParams.MATCH_PARENT, dp(MIX_HEIGHT_DP)))

        layoutManager.spanSizeLookup =
            object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int) =
                    if (rows.getOrNull(position) is Row.Header) layoutManager.spanCount else 1
            }
        grid.layoutManager = layoutManager
        grid.adapter = adapter
        grid.clipToPadding = false
        grid.setPadding(dp(GAP_DP), 0, dp(GAP_DP), dp(GAP_DP))
        grid.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(
                    recyclerView: RecyclerView,
                    dx: Int,
                    dy: Int,
                ) {
                    val first = layoutManager.findFirstVisibleItemPosition()
                    if (first >= 0) setActive(groupAt(first))
                }
            },
        )
        addView(grid, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        updateMix()
    }

    fun applyPalette(palette: KeyboardPalette) {
        this.palette = palette
        setBackgroundColor(palette.root)
        bar.setBackgroundColor(palette.secondaryFill)
        backButton.setTextColor(palette.secondaryText)
        deleteButton.setColorFilter(palette.secondaryText)
        mixStrip.background = null
        mixText.setTextColor(palette.secondaryText)
        adapter.refresh()
        styleTabs()
    }

    /** Shows [groups] from the top; forgets the emojis tapped before. */
    fun show(groups: List<EmojiGroup>) {
        val list = ArrayList<Row>()
        val starts = ArrayList<Int>()
        groups.forEach { group ->
            starts.add(list.size)
            list.add(Row.Header(group.name))
            group.emojis.mapTo(list, Row::Emoji)
        }
        rows = list
        headers = starts
        adapter.refresh()
        rebuildTabs(groups)
        grid.scrollToPosition(0)
        picked.clear()
        updateMix()
    }

    /** Forgets the emojis tapped (the panel is being hidden). */
    fun reset() {
        picked.clear()
        updateMix()
    }

    private fun pick(emoji: String) {
        onEmojiPicked(emoji)
        picked.add(emoji)
        while (picked.size > 2) picked.removeAt(0)
        updateMix()
    }

    private fun delete() {
        onBackspace()
        picked.removeLastOrNull()
        updateMix()
    }

    private fun sendMix(mix: EmojiMix) {
        onMixPicked(mix)
        picked.clear()
        updateMix()
    }

    /** The strip: the mix of the last two emojis tapped, or what to do to get one. */
    private fun updateMix() {
        val kitchen = kitchen
        val first = picked.getOrNull(0)
        val second = picked.getOrNull(1)
        mix = if (kitchen != null && first != null && second != null) kitchen.mix(first, second) else null
        val found = mix
        mixText.text =
            when {
                found != null -> "${found.title} · toca para enviar"
                kitchen == null -> ""
                first == null -> "🧪 Toca dos emojis para mezclarlos"
                second == null && kitchen.supports(first) -> "$first + … toca otro emoji para mezclarlo"
                second == null -> "$first no se puede mezclar"
                else -> "$first + $second no se pueden mezclar"
            }
        mixStrip.isClickable = found != null
        mixStrip.isFocusable = found != null
        mixStrip.contentDescription = found?.let { "Enviar la mezcla de ${it.first} y ${it.second}" }
        mixImage.visibility = if (found != null) VISIBLE else GONE
        if (found != null) {
            thumbnails.load(Meme(found.url, MIX_FILE_NAME, "image/png"), dp(MIX_IMAGE_DP), mixImage)
        } else {
            mixImage.tag = null
            mixImage.setImageDrawable(null)
        }
    }

    // ------------------------------------------------------------------
    // Tabs
    // ------------------------------------------------------------------

    private fun rebuildTabs(groups: List<EmojiGroup>) {
        tabs.removeAllViews()
        tabViews =
            groups.mapIndexed { index, group ->
                textButton(group.icon) { jumpTo(index) }.also {
                    it.contentDescription = group.name
                    it.setPadding(dp(TAB_PADDING_DP), 0, dp(TAB_PADDING_DP), 0)
                    tabs.addView(it, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
                }
            }
        activeTab = -1
        setActive(0)
    }

    private fun jumpTo(group: Int) {
        val position = headers.getOrNull(group) ?: return
        layoutManager.scrollToPositionWithOffset(position, 0)
        setActive(group)
    }

    /** The group [position] belongs to. */
    private fun groupAt(position: Int): Int = headers.indexOfLast { it <= position }.coerceAtLeast(0)

    private fun setActive(group: Int) {
        if (group == activeTab || tabViews.isEmpty()) return
        activeTab = group
        styleTabs()
        val tab = tabViews.getOrNull(group) ?: return
        tabScroll.post { tabScroll.smoothScrollTo(tab.left - (tabScroll.width - tab.width) / 2, 0) }
    }

    private fun styleTabs() {
        val colors = palette ?: return
        tabViews.forEachIndexed { index, tab ->
            val active = index == activeTab
            tab.background = if (active) rounded(colors.primaryFill) else null
            tab.isSelected = active
        }
    }

    // ------------------------------------------------------------------
    // Layout helpers
    // ------------------------------------------------------------------

    override fun onSizeChanged(
        w: Int,
        h: Int,
        oldw: Int,
        oldh: Int,
    ) {
        super.onSizeChanged(w, h, oldw, oldh)
        val columns = max(MIN_COLUMNS, (w / density / CELL_DP).toInt())
        layoutManager.spanCount = columns
        adapter.cellSize = (w - 2 * dp(GAP_DP)) / columns
    }

    private fun textButton(
        text: String,
        onClick: () -> Unit,
    ) = TextView(context).apply {
        this.text = text
        gravity = Gravity.CENTER
        maxLines = 1
        setTextSize(TypedValue.COMPLEX_UNIT_SP, BAR_TEXT_SP)
        isClickable = true
        isFocusable = true
        setOnClickListener { onClick() }
    }

    private fun rounded(color: Int) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(CORNER_DP).toFloat()
        }

    private fun dp(value: Float) = (value * density).toInt()

    private sealed interface Row {
        class Header(
            val name: String,
        ) : Row

        class Emoji(
            val emoji: String,
        ) : Row
    }

    private class RowHolder(
        view: View,
    ) : RecyclerView.ViewHolder(view)

    private inner class EmojiAdapter : RecyclerView.Adapter<RowHolder>() {
        var cellSize = 0

        @SuppressLint("NotifyDataSetChanged") // the whole list is replaced at once
        fun refresh() = notifyDataSetChanged()

        override fun getItemCount() = rows.size

        override fun getItemViewType(position: Int) = if (rows[position] is Row.Header) HEADER else EMOJI

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): RowHolder {
            val view =
                TextView(parent.context).apply {
                    maxLines = 1
                    isFocusable = true
                    if (viewType == HEADER) {
                        gravity = Gravity.CENTER_VERTICAL
                        typeface = Typeface.DEFAULT_BOLD
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, HEADER_TEXT_SP)
                        setPadding(dp(HEADER_PADDING_DP), 0, dp(HEADER_PADDING_DP), 0)
                        layoutParams = RecyclerView.LayoutParams(MATCH_PARENT, dp(HEADER_HEIGHT_DP))
                    } else {
                        gravity = Gravity.CENTER
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, EMOJI_TEXT_SP)
                        isClickable = true
                    }
                }
            return RowHolder(view)
        }

        override fun onBindViewHolder(
            holder: RowHolder,
            position: Int,
        ) {
            val view = holder.itemView as TextView
            when (val row = rows[position]) {
                is Row.Header -> {
                    view.text = row.name
                    view.setTextColor(palette?.secondaryText ?: view.currentTextColor)
                    ViewCompat.setAccessibilityHeading(view, true)
                }
                is Row.Emoji -> {
                    val size = if (cellSize > 0) cellSize else dp(CELL_DP)
                    view.layoutParams = RecyclerView.LayoutParams(size, size)
                    view.text = row.emoji
                    view.setOnClickListener { pick(row.emoji) }
                }
            }
        }
    }

    private companion object {
        const val HEADER = 0
        const val EMOJI = 1
        const val MIX_FILE_NAME = "Mezcla de emojis.png"
        const val BAR_HEIGHT_DP = 44f
        const val BACK_WIDTH_DP = 64f
        const val DELETE_WIDTH_DP = 56f
        const val DELETE_PADDING_DP = 16f
        const val BAR_TEXT_SP = 15f
        const val TAB_PADDING_DP = 10f
        const val CORNER_DP = 10f
        const val MIX_HEIGHT_DP = 48f
        const val MIX_IMAGE_DP = 40f
        const val MIX_PADDING_DP = 8f
        const val MIX_TEXT_SP = 14f
        const val GAP_DP = 3f
        const val CELL_DP = 44f
        const val MIN_COLUMNS = 7
        const val EMOJI_TEXT_SP = 26f
        const val HEADER_HEIGHT_DP = 30f
        const val HEADER_PADDING_DP = 8f
        const val HEADER_TEXT_SP = 13f
    }
}
