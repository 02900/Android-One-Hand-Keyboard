package com.onehandkeyboard.ui

import android.content.Context
import android.graphics.Color
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.onehandkeyboard.memes.Meme
import com.onehandkeyboard.memes.MemeCategory
import com.onehandkeyboard.memes.MemeLibrary
import com.onehandkeyboard.memes.MemeThumbnails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.Executor

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MemePanelViewTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val sent = mutableListOf<Meme>()

    private val heights = mutableListOf<Pair<Int, Boolean>>()

    private val panel =

        MemePanelView(context, MemeThumbnails(context, Executor { it.run() })).apply {

            applyPalette(KeyboardPalette(Color.BLACK, Color.BLUE, Color.DKGRAY, Color.WHITE, Color.LTGRAY))

            onMemePicked = { sent += it }

            onHeightDragged = { height, final -> heights += height to final }
        }

    private fun meme(name: String) = Meme("content://memes/$name", "$name.gif", "image/gif")

    private fun category(
        name: String,
        count: Int,
    ) = MemeCategory(name, (1..count).map { meme("$name-$it") })

    private fun layOut() {
        panel.measure(
            View.MeasureSpec.makeMeasureSpec(WIDTH, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(HEIGHT, View.MeasureSpec.EXACTLY),
        )

        panel.layout(0, 0, WIDTH, HEIGHT)
    }

    private fun show(vararg categories: MemeCategory) {
        panel.show(MemeLibrary.Result.Loaded(categories.toList()))

        layOut()
    }

    private fun <T : View> find(type: Class<T>): T = findIn(panel, type)!!

    private fun <T : View> findIn(
        view: View,
        type: Class<T>,
    ): T? {
        if (type.isInstance(view)) return type.cast(view)

        if (view !is ViewGroup) return null

        return (0 until view.childCount).firstNotNullOfOrNull { findIn(view.getChildAt(it), type) }
    }

    private val list get() = find(RecyclerView::class.java)

    private fun touch(
        target: View,
        action: Int,
        x: Float = 10f,
        y: Float = 10f,
    ) {
        val now = SystemClock.uptimeMillis()

        target.dispatchTouchEvent(MotionEvent.obtain(now, now, action, x, y, 0))
    }

    @Test
    fun `categories are titles over full-width rows, memes fill the grid`() {
        show(category("Risa", 4), category("Amor", 2))

        val grid = list.layoutManager as GridLayoutManager

        assertEquals(8, list.adapter!!.itemCount)

        assertEquals(grid.spanCount, grid.spanSizeLookup.getSpanSize(0))

        assertEquals(1, grid.spanSizeLookup.getSpanSize(1))

        assertEquals(grid.spanCount, grid.spanSizeLookup.getSpanSize(5))
    }

    @Test
    fun `a tap sends the meme`() {
        show(category("Risa", 2))

        val first = list.findViewHolderForAdapterPosition(1)!!.itemView

        assertTrue(first is ImageView)

        first.performClick()

        assertEquals(listOf(meme("Risa-1")), sent)
    }

    @Test
    fun `holding a meme shows it big and lifting the finger closes it without sending`() {
        show(category("Risa", 2))

        val first = list.findViewHolderForAdapterPosition(1)!!.itemView

        first.performLongClick()

        val preview = previewTitle()

        assertEquals(View.VISIBLE, (preview.parent as View).visibility)

        assertEquals("Risa-1", preview.text)

        touch(list, MotionEvent.ACTION_UP)

        assertEquals(View.GONE, (preview.parent as View).visibility)

        assertTrue(sent.isEmpty())
    }

    /** The big preview's title: the TextView showing a meme name. */

    private fun previewTitle(): android.widget.TextView {
        val all = mutableListOf<android.widget.TextView>()

        fun collect(view: View) {
            if (view is android.widget.TextView) all += view

            if (view is ViewGroup && view !is RecyclerView) {
                (0 until view.childCount).forEach {
                    collect(
                        view.getChildAt(it),
                    )
                }
            }
        }

        collect(panel)

        return all.first { it.text.startsWith("Risa-") }
    }

    @Test
    fun `dragging the grab bar up asks for a taller panel, down for a shorter one`() {
        show(category("Risa", 2))

        val handle = find(MemeResizeHandle::class.java)

        touch(handle, MotionEvent.ACTION_DOWN, y = 100f)

        touch(handle, MotionEvent.ACTION_MOVE, y = 40f)

        touch(handle, MotionEvent.ACTION_MOVE, y = 130f)

        touch(handle, MotionEvent.ACTION_UP, y = 130f)

        assertEquals(listOf(HEIGHT + 60 to false, HEIGHT - 30 to false, HEIGHT - 30 to true), heights)
    }

    @Test
    fun `tabs jump to their category and follow the scroll`() {
        show(category("Risa", 300), category("Amor", 300))

        val tabs = (find(HorizontalScrollView::class.java).getChildAt(0) as ViewGroup)

        val names = (0 until tabs.childCount).map { (tabs.getChildAt(it) as TextView).text.toString() }

        assertEquals(listOf("Risa", "Amor"), names)

        assertTrue(tabs.getChildAt(0).isSelected)

        tabs.getChildAt(1).performClick()

        layOut()

        assertEquals(301, (list.layoutManager as GridLayoutManager).findFirstVisibleItemPosition())

        assertTrue(tabs.getChildAt(1).isSelected)

        assertTrue(!tabs.getChildAt(0).isSelected)

        list.scrollToPosition(0)

        layOut()

        assertTrue(tabs.getChildAt(0).isSelected)
    }

    @Test
    fun `a category arriving later does not move the list being browsed`() {
        show(category("Risa", 300), category("Amor", 300))

        val grid = list.layoutManager as GridLayoutManager

        grid.scrollToPositionWithOffset(301, 0) // "Amor"'s title: Risa is 0 (title) + 300 memes

        layOut()

        assertEquals(301, grid.findFirstVisibleItemPosition())

        show(category("🔥 Tendencias", 6), category("Risa", 300), category("Amor", 300))

        assertEquals(7 + 301, grid.findFirstVisibleItemPosition()) // still "Amor"'s title
    }

    @Test
    fun `at the top, a category arriving later shows up first`() {
        show(category("Risa", 3))

        show(category("🔥 Tendencias", 3), category("Risa", 3))

        assertEquals(0, (list.layoutManager as GridLayoutManager).findFirstVisibleItemPosition())
    }

    private companion object {
        const val WIDTH = 1080

        const val HEIGHT = 900
    }
}
