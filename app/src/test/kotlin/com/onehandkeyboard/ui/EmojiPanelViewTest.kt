package com.onehandkeyboard.ui

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.onehandkeyboard.emoji.EmojiGroup
import com.onehandkeyboard.emoji.EmojiKitchen
import com.onehandkeyboard.emoji.EmojiMix
import com.onehandkeyboard.memes.MemeThumbnails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.Executor

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class EmojiPanelViewTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val typed = mutableListOf<String>()
    private val mixes = mutableListOf<EmojiMix>()
    private var deletes = 0

    private val panel =
        EmojiPanelView(context, MemeThumbnails(context, Executor { it.run() })).apply {
            applyPalette(KeyboardPalette(Color.BLACK, Color.BLUE, Color.DKGRAY, Color.WHITE, Color.LTGRAY))
            onEmojiPicked = { typed += it }
            onMixPicked = { mixes += it }
            onBackspace = { deletes++ }
        }

    private val kitchen by lazy { File("src/main/assets/emoji/kitchen.bin").inputStream().use(EmojiKitchen::read) }

    private fun show() {
        panel.show(GROUPS)
        panel.measure(
            View.MeasureSpec.makeMeasureSpec(WIDTH, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(HEIGHT, View.MeasureSpec.EXACTLY),
        )
        panel.layout(0, 0, WIDTH, HEIGHT)
    }

    private fun grid(): RecyclerView = views(panel).filterIsInstance<RecyclerView>().single()

    private fun views(view: View): List<View> {
        val children = if (view is ViewGroup) (0 until view.childCount).map(view::getChildAt) else emptyList()
        return listOf(view) + children.flatMap(::views)
    }

    /** Taps the emoji shown as [emoji] in the list. */
    private fun tap(emoji: String) {
        val cell = views(grid()).filterIsInstance<TextView>().first { it.text == emoji }
        assertTrue(cell.performClick())
    }

    /** The text of the mix strip (next to the mix's image). */
    private fun strip(): TextView =
        views(panel).filterIsInstance<TextView>().first { (it.parent as? ViewGroup)?.getChildAt(0) is ImageView }

    @Test
    fun `tapping emojis types them`() {
        show()
        tap("😂")
        tap("🐶")
        assertEquals(listOf("😂", "🐶"), typed)
    }

    @Test
    fun `without the mixes table there is no strip`() {
        show()
        assertEquals(View.GONE, (strip().parent as View).visibility)
    }

    @Test
    fun `two emojis that mix offer their mix, which is sent once tapped`() {
        show()
        panel.kitchen = kitchen
        assertEquals("🧪 Toca dos emojis para mezclarlos", strip().text.toString())
        tap("😀")
        assertTrue(strip().text.toString(), strip().text.startsWith("😀 + …"))
        tap("🐶")
        val mix = panel.mix
        assertEquals(kitchen.mix("😀", "🐶"), mix)
        assertTrue((strip().parent as View).performClick())
        assertEquals(listOf(mix), mixes)
        assertNull("the strip starts over", panel.mix)
    }

    @Test
    fun `the last two emojis are the ones mixed, and delete forgets the last one`() {
        show()
        panel.kitchen = kitchen
        tap("🐶")
        tap("😀")
        tap("😂")
        assertEquals(kitchen.mix("😀", "😂"), panel.mix)
        tap("🇦🇷")
        assertNull(panel.mix)
        assertEquals("😂 + 🇦🇷 no se pueden mezclar", strip().text.toString())
        views(panel).first { it.contentDescription == "borrar" }.performClick()
        assertEquals(1, deletes)
        assertTrue(strip().text.startsWith("😂 + …"))
    }

    private companion object {
        const val WIDTH = 1080
        const val HEIGHT = 900
        val GROUPS =
            listOf(
                EmojiGroup("😀", "Caras", listOf("😀", "😂", "🐶")),
                EmojiGroup("🔣", "Símbolos", listOf("🇦🇷")),
            )
    }
}
