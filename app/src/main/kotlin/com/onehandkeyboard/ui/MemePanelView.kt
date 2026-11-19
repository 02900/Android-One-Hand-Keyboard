package com.onehandkeyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.onehandkeyboard.memes.Meme
import com.onehandkeyboard.memes.MemeCategory
import com.onehandkeyboard.memes.MemeLibrary
import com.onehandkeyboard.memes.MemeRow
import com.onehandkeyboard.memes.MemeSections
import com.onehandkeyboard.memes.MemeThumbnails
import kotlin.math.max
import kotlin.math.min

/**
 * Meme library panel shown in place of the keys.
 *
 * - Top edge: a grab bar; drag it up or down to change the panel's height ([onHeightDragged]).
 * - Bar: back to the keys and one tab per category. Tapping a tab jumps to that category; the
 *   tab of the category being browsed is highlighted.
 * - One vertical list with every category: its title, then its memes in a grid. A fast-scroll
 *   thumb on the right edge jumps through it, with a bubble naming the category.
 * - Tap a meme to send it. Hold it to see it big; slide to see others; lifting just closes it.
 */
@SuppressLint("ViewConstructor") // created in code by the service, never inflated
class MemePanelView(
    context: Context,
    private val thumbnails: MemeThumbnails,
) : LinearLayout(context) {
    /** Back to the keys. */
    var onBack: () -> Unit = {}

    var onMemePicked: (Meme) -> Unit = {}

    /** Opens the keyboard settings (to pick the folder). */
    var onOpenSettings: () -> Unit = {}

    /** The grab bar was dragged: the height the panel should take. [final] when released. */
    var onHeightDragged: (heightPx: Int, final: Boolean) -> Unit = { _, _ -> }

    private val density = resources.displayMetrics.density
    private var palette: KeyboardPalette? = null
    private var sections = MemeSections(emptyList())

    private val handle = MemeResizeHandle(context) { height }
    private val bar = LinearLayout(context)
    private val backButton = barButton("ABC") { onBack() }
    private val tabs = LinearLayout(context)
    private val tabScroll = HorizontalScrollView(context)
    private var tabViews: List<TextView> = emptyList()
    private var activeSection = -1
    private val adapter = MemeAdapter()
    private val layoutManager = GridLayoutManager(context, MIN_COLUMNS)
    private val grid = RecyclerView(context)
    private val scroller = MemeFastScroller(context, grid)
    private val message = TextView(context)
    private val messageAction = barButton("Abrir ajustes") { onOpenSettings() }
    private val messageBox = LinearLayout(context)
    private val preview = LinearLayout(context)
    private val previewImage = ImageView(context)
    private val previewTitle = TextView(context)

    /** The meme shown big while a finger holds the list, or null. */
    private var previewing: Meme? = null

    init {
        orientation = VERTICAL
        handle.onHeightDragged = { height, final -> onHeightDragged(height, final) }
        addView(handle, LayoutParams(LayoutParams.MATCH_PARENT, dp(MemeResizeHandle.HEIGHT_DP)))
        bar.orientation = HORIZONTAL
        bar.gravity = Gravity.CENTER_VERTICAL
        bar.addView(backButton, LayoutParams(dp(BACK_WIDTH_DP), LayoutParams.MATCH_PARENT))
        tabs.orientation = HORIZONTAL
        tabScroll.isHorizontalScrollBarEnabled = false
        tabScroll.addView(tabs, ViewGroup.LayoutParams(WRAP_CONTENT, MATCH_PARENT))
        bar.addView(tabScroll, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        addView(bar, LayoutParams(LayoutParams.MATCH_PARENT, dp(BAR_HEIGHT_DP)))

        layoutManager.spanSizeLookup =
            object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int) =
                    if (sections.rows.getOrNull(position) is MemeRow.Header) layoutManager.spanCount else 1
            }
        grid.layoutManager = layoutManager
        grid.adapter = adapter
        grid.clipToPadding = false
        grid.isVerticalScrollBarEnabled = false
        grid.setPadding(dp(GAP_DP), dp(GAP_DP), dp(SCROLLER_GUTTER_DP), dp(GAP_DP))
        grid.addOnItemTouchListener(PreviewTouch())
        grid.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(
                    recyclerView: RecyclerView,
                    dx: Int,
                    dy: Int,
                ) = updateActiveTab()
            },
        )
        scroller.sectionNameAt = { position -> sections.sectionNames.getOrNull(sections.sectionAt(position)) }

        messageBox.orientation = VERTICAL
        messageBox.gravity = Gravity.CENTER
        val padding = dp(MESSAGE_PADDING_DP)
        messageBox.setPadding(padding, padding, padding, padding)
        message.gravity = Gravity.CENTER
        message.setTextSize(TypedValue.COMPLEX_UNIT_SP, MESSAGE_TEXT_SP)
        messageBox.addView(message)
        messageBox.addView(messageAction, LayoutParams(LayoutParams.WRAP_CONTENT, dp(BAR_HEIGHT_DP)))

        preview.orientation = VERTICAL
        preview.gravity = Gravity.CENTER
        preview.visibility = GONE
        preview.isClickable = true // also closes it when opened from TalkBack (no finger to lift)
        preview.setOnClickListener { endPreview() }
        previewImage.scaleType = ImageView.ScaleType.FIT_CENTER
        preview.addView(previewImage, LayoutParams(0, 0))
        previewTitle.gravity = Gravity.CENTER
        previewTitle.maxLines = 1
        previewTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, MESSAGE_TEXT_SP)
        preview.addView(previewTitle, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        val content = FrameLayout(context)
        content.addView(grid, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        content.addView(scroller, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        content.addView(messageBox, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        content.addView(preview, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
    }

    fun applyPalette(palette: KeyboardPalette) {
        this.palette = palette
        setBackgroundColor(palette.root)
        handle.setBackgroundColor(palette.secondaryFill)
        handle.setColor(palette.secondaryText)
        bar.setBackgroundColor(palette.secondaryFill)
        backButton.setTextColor(palette.secondaryText)
        message.setTextColor(palette.secondaryText)
        messageAction.setTextColor(palette.secondaryText)
        messageAction.background = rounded(palette.secondaryFill)
        scroller.setColors(palette.secondaryText, palette.primaryFill, palette.primaryText)
        preview.setBackgroundColor(ColorUtils.setAlphaComponent(palette.root, PREVIEW_DIM_ALPHA))
        previewTitle.setTextColor(palette.secondaryText)
        adapter.notifyDataSetChanged()
        styleTabs()
    }

    fun showLoading() {
        showMessage("Cargando memes…", withAction = false)
    }

    fun show(result: MemeLibrary.Result) {
        when (result) {
            MemeLibrary.Result.NotConfigured ->
                showMessage("Elige la carpeta de tus memes en los ajustes del teclado.", withAction = true)
            MemeLibrary.Result.NoAccess ->
                showMessage(
                    "No se puede leer la carpeta de memes. Vuelve a elegirla en los ajustes.",
                    withAction = true,
                )
            is MemeLibrary.Result.Loaded ->
                if (result.categories.all { it.memes.isEmpty() }) {
                    showMessage("La carpeta no tiene GIFs ni stickers (gif, webp, png, jpg).", withAction = true)
                } else {
                    showCategories(result.categories)
                }
        }
    }

    /** Releases the previews (the panel is being hidden). */
    fun clear() {
        endPreview()
        sections = MemeSections(emptyList())
        adapter.refresh()
        rebuildTabs()
        thumbnails.clear()
    }

    /**
     * Shows the categories. When the list is already showing (e.g. the trending category arrives
     * later), the row at the top stays where it was instead of jumping.
     */
    private fun showCategories(list: List<MemeCategory>) {
        val first = layoutManager.findFirstVisibleItemPosition()
        val offset = layoutManager.findViewByPosition(first)?.let { it.top - grid.paddingTop } ?: 0
        val atTop = first <= 0 && offset >= 0
        val anchor = if (atTop) null else sections.keyAt(first)
        sections = MemeSections(list)
        messageBox.visibility = GONE
        grid.visibility = VISIBLE
        scroller.visibility = VISIBLE
        adapter.refresh()
        rebuildTabs()
        val position = sections.positionOfKey(anchor)
        if (position >= 0) layoutManager.scrollToPositionWithOffset(position, offset) else grid.scrollToPosition(0)
        setActive(sections.sectionAt(position.coerceAtLeast(0)))
    }

    private fun showMessage(
        text: String,
        withAction: Boolean,
    ) {
        endPreview()
        sections = MemeSections(emptyList())
        adapter.refresh()
        rebuildTabs()
        message.text = text
        messageAction.visibility = if (withAction) VISIBLE else GONE
        grid.visibility = GONE
        scroller.visibility = GONE
        messageBox.visibility = VISIBLE
    }

    // ------------------------------------------------------------------
    // Tabs
    // ------------------------------------------------------------------

    private fun rebuildTabs() {
        tabs.removeAllViews()
        tabViews =
            sections.sectionNames.mapIndexed { index, name ->
                barButton(name) { jumpTo(index) }.also {
                    tabs.addView(it, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
                }
            }
        activeSection = -1
        setActive(0)
    }

    /** Scrolls the list so that [section]'s title is at the top. */
    private fun jumpTo(section: Int) {
        val position = sections.headerPosition(section)
        if (position < 0) return
        layoutManager.scrollToPositionWithOffset(position, 0)
        setActive(section)
    }

    private fun updateActiveTab() {
        val first = layoutManager.findFirstVisibleItemPosition()
        if (first >= 0) setActive(sections.sectionAt(first))
    }

    private fun setActive(section: Int) {
        if (section == activeSection || tabViews.isEmpty()) return
        activeSection = section
        styleTabs()
        val tab = tabViews.getOrNull(section) ?: return
        tabScroll.post { tabScroll.smoothScrollTo(tab.left - (tabScroll.width - tab.width) / 2, 0) }
    }

    private fun styleTabs() {
        val colors = palette ?: return
        tabViews.forEachIndexed { index, tab ->
            val active = index == activeSection
            tab.setTextColor(if (active) colors.primaryText else colors.secondaryText)
            tab.background = if (active) rounded(colors.primaryFill) else null
            tab.isSelected = active
        }
    }

    // ------------------------------------------------------------------
    // Big preview
    // ------------------------------------------------------------------

    private fun startPreview(meme: Meme) {
        grid.parent?.requestDisallowInterceptTouchEvent(true)
        showPreview(meme)
    }

    private fun showPreview(meme: Meme) {
        if (previewing == meme) return
        previewing = meme
        val content = preview.parent as ViewGroup
        val size = (min(content.width, content.height - dp(PREVIEW_TITLE_DP)) * PREVIEW_SCALE).toInt().coerceAtLeast(1)
        previewImage.layoutParams = LayoutParams(size, size)
        previewTitle.text = meme.title
        preview.visibility = VISIBLE
        preview.announceForAccessibility(meme.title)
        thumbnails.load(meme, size, previewImage)
    }

    private fun endPreview() {
        if (previewing == null) return
        previewing = null
        preview.visibility = GONE
        previewImage.setImageDrawable(null)
        previewImage.tag = null
    }

    private fun memeAt(
        x: Float,
        y: Float,
    ): Meme? {
        val child = grid.findChildViewUnder(x, y) ?: return null
        return (sections.rows.getOrNull(grid.getChildAdapterPosition(child)) as? MemeRow.Item)?.meme
    }

    /** While a meme is shown big, the finger moves the preview instead of scrolling the list. */
    private inner class PreviewTouch : RecyclerView.SimpleOnItemTouchListener() {
        override fun onInterceptTouchEvent(
            rv: RecyclerView,
            e: MotionEvent,
        ): Boolean {
            if (previewing == null) return false
            handle(e)
            return true
        }

        override fun onTouchEvent(
            rv: RecyclerView,
            e: MotionEvent,
        ) = handle(e)

        private fun handle(e: MotionEvent) {
            when (e.actionMasked) {
                MotionEvent.ACTION_MOVE -> memeAt(e.x, e.y)?.let(::showPreview)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> endPreview()
            }
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
        val columns = max(MIN_COLUMNS, (w / density / COLUMN_WIDTH_DP).toInt())
        layoutManager.spanCount = columns
        adapter.cellSize = (w - dp(GAP_DP) - dp(SCROLLER_GUTTER_DP)) / columns
    }

    private fun barButton(
        text: String,
        onClick: () -> Unit,
    ) = TextView(context).apply {
        this.text = text
        gravity = Gravity.CENTER
        maxLines = 1
        setTextSize(TypedValue.COMPLEX_UNIT_SP, BAR_TEXT_SP)
        val padding = dp(TAB_PADDING_DP)
        setPadding(padding, 0, padding, 0)
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

    private class RowHolder(
        view: android.view.View,
    ) : RecyclerView.ViewHolder(view)

    private inner class MemeAdapter : RecyclerView.Adapter<RowHolder>() {
        var cellSize = 0

        @SuppressLint("NotifyDataSetChanged") // the whole list is replaced at once
        fun refresh() = notifyDataSetChanged()

        override fun getItemCount() = sections.size

        override fun getItemViewType(position: Int) = if (sections.rows[position] is MemeRow.Header) HEADER else ITEM

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): RowHolder {
            if (viewType == HEADER) {
                val title =
                    TextView(parent.context).apply {
                        gravity = Gravity.CENTER_VERTICAL
                        maxLines = 1
                        typeface = Typeface.DEFAULT_BOLD
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, HEADER_TEXT_SP)
                        setPadding(dp(HEADER_PADDING_DP), 0, dp(HEADER_PADDING_DP), 0)
                        isFocusable = true
                        layoutParams = RecyclerView.LayoutParams(MATCH_PARENT, dp(HEADER_HEIGHT_DP))
                    }
                return RowHolder(title)
            }
            val image =
                ImageView(parent.context).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    val inset = dp(GAP_DP)
                    setPadding(inset, inset, inset, inset)
                    isClickable = true
                    isFocusable = true
                    isLongClickable = true
                }
            return RowHolder(image)
        }

        override fun onBindViewHolder(
            holder: RowHolder,
            position: Int,
        ) {
            when (val row = sections.rows[position]) {
                is MemeRow.Header -> {
                    val title = holder.itemView as TextView
                    title.text = row.name
                    title.setTextColor(palette?.secondaryText ?: title.currentTextColor)
                    ViewCompat.setAccessibilityHeading(title, true)
                }
                is MemeRow.Item -> bindMeme(holder.itemView as ImageView, row.meme)
            }
        }

        private fun bindMeme(
            image: ImageView,
            meme: Meme,
        ) {
            val size = if (cellSize > 0) cellSize else dp(COLUMN_WIDTH_DP)
            image.layoutParams = RecyclerView.LayoutParams(size, size)
            image.background = palette?.let { rounded(it.secondaryFill) }
            image.contentDescription = meme.title
            image.setOnClickListener { onMemePicked(meme) }
            image.setOnLongClickListener {
                startPreview(meme)
                true
            }
            thumbnails.load(meme, size, image)
        }
    }

    private companion object {
        const val HEADER = 0
        const val ITEM = 1
        const val BAR_HEIGHT_DP = 44f
        const val BACK_WIDTH_DP = 64f
        const val BAR_TEXT_SP = 15f
        const val TAB_PADDING_DP = 14f
        const val CORNER_DP = 10f
        const val GAP_DP = 3f
        const val SCROLLER_GUTTER_DP = 14f
        const val COLUMN_WIDTH_DP = 96f
        const val MIN_COLUMNS = 3
        const val HEADER_HEIGHT_DP = 34f
        const val HEADER_PADDING_DP = 8f
        const val HEADER_TEXT_SP = 14f
        const val MESSAGE_PADDING_DP = 24f
        const val MESSAGE_TEXT_SP = 15f
        const val PREVIEW_SCALE = 0.85f
        const val PREVIEW_TITLE_DP = 32f
        const val PREVIEW_DIM_ALPHA = 0xE6
    }
}
