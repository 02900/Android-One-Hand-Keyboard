package com.onehandkeyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.onehandkeyboard.memes.Meme
import com.onehandkeyboard.memes.MemeCategory
import com.onehandkeyboard.memes.MemeLibrary
import com.onehandkeyboard.memes.MemeThumbnails
import kotlin.math.max

/**
 * Meme library panel shown in place of the keys: category tabs on top, a grid of animated
 * previews below. It takes the keyboard's size, so the app being typed in does not jump.
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

    private val density = resources.displayMetrics.density
    private var palette: KeyboardPalette? = null
    private var categories: List<MemeCategory> = emptyList()
    private var selected = 0

    private val bar = LinearLayout(context)
    private val tabs = LinearLayout(context)
    private val tabScroll = HorizontalScrollView(context)
    private val backButton = barButton("ABC") { onBack() }
    private val adapter = MemeAdapter()
    private val grid = RecyclerView(context)
    private val message = TextView(context)
    private val messageAction = barButton("Abrir ajustes") { onOpenSettings() }
    private val messageBox = LinearLayout(context)

    init {
        orientation = VERTICAL
        bar.orientation = HORIZONTAL
        bar.addView(backButton, LayoutParams(dp(BACK_WIDTH_DP), LayoutParams.MATCH_PARENT))
        tabs.orientation = HORIZONTAL
        tabScroll.isHorizontalScrollBarEnabled = false
        tabScroll.addView(tabs, ViewGroup.LayoutParams(WRAP_CONTENT, MATCH_PARENT))
        bar.addView(tabScroll, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        addView(bar, LayoutParams(LayoutParams.MATCH_PARENT, dp(BAR_HEIGHT_DP)))

        grid.layoutManager = GridLayoutManager(context, MIN_COLUMNS)
        grid.adapter = adapter
        grid.setHasFixedSize(true)
        grid.clipToPadding = false
        grid.setPadding(dp(GAP_DP), dp(GAP_DP), dp(GAP_DP), dp(GAP_DP))

        messageBox.orientation = VERTICAL
        messageBox.gravity = Gravity.CENTER
        val padding = dp(MESSAGE_PADDING_DP)
        messageBox.setPadding(padding, padding, padding, padding)
        message.gravity = Gravity.CENTER
        message.setTextSize(TypedValue.COMPLEX_UNIT_SP, MESSAGE_TEXT_SP)
        messageBox.addView(message)
        messageBox.addView(messageAction, LayoutParams(LayoutParams.WRAP_CONTENT, dp(BAR_HEIGHT_DP)))

        val content = FrameLayout(context)
        content.addView(grid, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        content.addView(messageBox, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
    }

    fun applyPalette(palette: KeyboardPalette) {
        this.palette = palette
        setBackgroundColor(palette.root)
        bar.setBackgroundColor(palette.secondaryFill)
        backButton.setTextColor(palette.secondaryText)
        message.setTextColor(palette.secondaryText)
        messageAction.setTextColor(palette.secondaryText)
        messageAction.background = rounded(palette.secondaryFill)
        updateTabs()
        adapter.notifyDataSetChanged()
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
                if (result.categories.isEmpty()) {
                    showMessage("La carpeta no tiene GIFs ni stickers (gif, webp, png, jpg).", withAction = true)
                } else {
                    showCategories(result.categories)
                }
        }
    }

    /** Releases the previews (the panel is being hidden). */
    fun clear() {
        categories = emptyList()
        adapter.submit(emptyList())
        updateTabs()
        thumbnails.clear()
    }

    private fun showCategories(list: List<MemeCategory>) {
        val previous = categories.getOrNull(selected)?.name
        categories = list
        selected = list.indexOfFirst { it.name == previous }.takeIf { it >= 0 } ?: 0
        messageBox.visibility = GONE
        grid.visibility = VISIBLE
        updateTabs()
        adapter.submit(list[selected].memes)
    }

    private fun showMessage(
        text: String,
        withAction: Boolean,
    ) {
        categories = emptyList()
        adapter.submit(emptyList())
        updateTabs()
        message.text = text
        messageAction.visibility = if (withAction) VISIBLE else GONE
        grid.visibility = GONE
        messageBox.visibility = VISIBLE
    }

    private fun select(index: Int) {
        if (index == selected) return
        selected = index
        updateTabs()
        thumbnails.clear()
        adapter.submit(categories[index].memes)
        grid.scrollToPosition(0)
    }

    private fun updateTabs() {
        tabs.removeAllViews()
        val colors = palette ?: return
        categories.forEachIndexed { index, category ->
            val tab = barButton(category.name) { select(index) }
            val active = index == selected
            tab.setTextColor(if (active) colors.primaryText else colors.secondaryText)
            tab.background = if (active) rounded(colors.primaryFill) else null
            tab.isSelected = active
            tabs.addView(tab, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
        }
    }

    override fun onSizeChanged(
        w: Int,
        h: Int,
        oldw: Int,
        oldh: Int,
    ) {
        super.onSizeChanged(w, h, oldw, oldh)
        val columns = max(MIN_COLUMNS, (w / density / COLUMN_WIDTH_DP).toInt())
        (grid.layoutManager as GridLayoutManager).spanCount = columns
        adapter.cellSize = (w - 2 * dp(GAP_DP)) / columns
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

    private class MemeHolder(
        val image: ImageView,
    ) : RecyclerView.ViewHolder(image)

    private inner class MemeAdapter : RecyclerView.Adapter<MemeHolder>() {
        private var items: List<Meme> = emptyList()
        var cellSize = 0

        @SuppressLint("NotifyDataSetChanged") // a whole tab is replaced at once
        fun submit(memes: List<Meme>) {
            items = memes
            notifyDataSetChanged()
        }

        override fun getItemCount() = items.size

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int,
        ): MemeHolder {
            val image =
                ImageView(parent.context).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    val inset = dp(GAP_DP)
                    setPadding(inset, inset, inset, inset)
                    isClickable = true
                    isFocusable = true
                }
            return MemeHolder(image)
        }

        override fun onBindViewHolder(
            holder: MemeHolder,
            position: Int,
        ) {
            val meme = items[position]
            val size = if (cellSize > 0) cellSize else dp(COLUMN_WIDTH_DP)
            holder.image.layoutParams = RecyclerView.LayoutParams(size, size)
            holder.image.background = palette?.let { rounded(it.secondaryFill) }
            holder.image.contentDescription = meme.title
            holder.image.setOnClickListener { onMemePicked(meme) }
            thumbnails.load(meme, size, holder.image)
        }
    }

    private companion object {
        const val BAR_HEIGHT_DP = 44f
        const val BACK_WIDTH_DP = 64f
        const val BAR_TEXT_SP = 15f
        const val TAB_PADDING_DP = 14f
        const val CORNER_DP = 10f
        const val GAP_DP = 3f
        const val COLUMN_WIDTH_DP = 96f
        const val MIN_COLUMNS = 3
        const val MESSAGE_PADDING_DP = 24f
        const val MESSAGE_TEXT_SP = 15f
    }
}
