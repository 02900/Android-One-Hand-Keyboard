package com.onehandkeyboard.ui

import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.test.core.app.ApplicationProvider
import com.onehandkeyboard.GestureIntegration
import com.onehandkeyboard.KeyboardLayoutManager
import com.onehandkeyboard.R
import com.onehandkeyboard.config.KeyboardConfigurationFactory
import com.onehandkeyboard.core.design.theme.ThemeManager
import com.onehandkeyboard.core.domain.model.KeyAction
import com.onehandkeyboard.core.domain.model.KeyboardOutput
import com.onehandkeyboard.enums.Language
import com.onehandkeyboard.prefs.KeyboardPreferences
import com.onehandkeyboard.spell.Suggestion
import com.onehandkeyboard.spell.Suggestions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class KeyboardViewTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val prefs = KeyboardPreferences(context)
    private val layoutManager = KeyboardLayoutManager(KeyboardConfigurationFactory())
    private val gestures = GestureIntegration(context, layoutManager, KeyFeedback(context, prefs))
    private val outputs = mutableListOf<KeyboardOutput>()

    private lateinit var view: KeyboardView
    private val downTime = SystemClock.uptimeMillis()

    private fun createView(): KeyboardView {
        gestures.onOutput = { outputs += it }
        return KeyboardView(
            context = context,
            layoutManager = layoutManager,
            themeManager = ThemeManager(context),
            labels = GestureHintProvider(layoutManager),
            layoutCalculator = LayoutCalculator(context),
            gestures = gestures,
            prefs = prefs,
        ).apply {
            measure(
                View.MeasureSpec.makeMeasureSpec(WIDTH, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            )
            layout(0, 0, measuredWidth, measuredHeight)
        }.also { view = it }
    }

    private val gridHeight get() = LayoutCalculator(context).calculateCellHeight() * 4

    /** Height of the suggestion strip above the grid. */
    private val stripHeight get() = view.height - gridHeight
    private val columnWidth get() = view.width / 4f
    private val rowHeight get() = gridHeight / 4f

    /** Center of a grid cell (full-width keyboard). */
    private fun cellCenter(cellIndex: Int) =
        floatArrayOf(columnWidth * (cellIndex % 4 + 0.5f), stripHeight + rowHeight * (cellIndex / 4 + 0.5f))

    private fun send(
        action: Int,
        pointers: List<Pair<Int, FloatArray>>,
        actionIndex: Int = 0,
    ) {
        val properties =
            pointers.map { (id, _) ->
                MotionEvent.PointerProperties().apply {
                    this.id = id
                    toolType = MotionEvent.TOOL_TYPE_FINGER
                }
            }.toTypedArray()
        val coords =
            pointers.map { (_, xy) ->
                MotionEvent.PointerCoords().apply {
                    x = xy[0]
                    y = xy[1]
                    pressure = 1f
                    size = 1f
                }
            }.toTypedArray()
        val fullAction = action or (actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        val event =
            MotionEvent.obtain(
                downTime,
                SystemClock.uptimeMillis(),
                fullAction,
                pointers.size,
                properties,
                coords,
                0, 0, 1f, 1f, 0, 0, 0, 0,
            )
        view.onTouchEvent(event)
        event.recycle()
    }

    private fun tap(xy: FloatArray) {
        send(MotionEvent.ACTION_DOWN, listOf(0 to xy))
        send(MotionEvent.ACTION_UP, listOf(0 to xy))
    }

    @Test
    fun `expert mode still types but shows no preview`() {
        createView()
        val a = cellCenter(0)
        send(MotionEvent.ACTION_DOWN, listOf(0 to a))
        assertTrue(view.previewShowing)
        send(MotionEvent.ACTION_UP, listOf(0 to a))

        view.expertMode = true
        send(MotionEvent.ACTION_DOWN, listOf(0 to a))
        assertFalse(view.previewShowing)
        send(MotionEvent.ACTION_UP, listOf(0 to a))

        assertEquals(List(2) { KeyboardOutput.Character("a") }, outputs)
    }

    @Test
    fun `tap on a key types its character`() {
        createView()
        tap(cellCenter(0))
        assertEquals(listOf<KeyboardOutput>(KeyboardOutput.Character("a")), outputs)
    }

    @Test
    fun `measured height follows the configured cell height plus the strip`() {
        prefs.suggestionsEnabled = true
        createView()
        assertTrue(stripHeight > 0)
        prefs.suggestionsEnabled = false
        createView()
        assertEquals(gridHeight, view.measuredHeight)
        prefs.suggestionsEnabled = true
    }

    @Test
    fun `tapping a suggestion picks it and does not type a key`() {
        createView()
        val picked = mutableListOf<Suggestion>()
        view.onSuggestionPicked = { picked += it }
        val auto = Suggestion("canción", Suggestion.Kind.AUTO)
        val typed = Suggestion("cancion", Suggestion.Kind.TYPED)
        view.suggestions = Suggestions("cancion", "canción", listOf(typed, auto, null))

        tap(floatArrayOf(view.width / 2f, stripHeight / 2f))
        tap(floatArrayOf(view.width * 5f / 6f, stripHeight / 2f)) // empty slot: nothing

        assertEquals(listOf(auto), picked)
        assertTrue(outputs.isEmpty())
    }

    @Test
    fun `roll-over keeps each finger on its own key`() {
        createView()
        val a = cellCenter(0)
        val n = cellCenter(1)
        send(MotionEvent.ACTION_DOWN, listOf(0 to a))
        send(MotionEvent.ACTION_POINTER_DOWN, listOf(0 to a, 1 to n), actionIndex = 1)
        send(MotionEvent.ACTION_POINTER_UP, listOf(0 to a, 1 to n), actionIndex = 0) // first finger lifts first
        send(MotionEvent.ACTION_UP, listOf(1 to n))

        assertEquals(listOf(KeyboardOutput.Character("a"), KeyboardOutput.Character("n")), outputs)
    }

    @Test
    fun `swipe up on a key types its north character`() {
        createView()
        val start = cellCenter(1)
        val end = floatArrayOf(start[0], start[1] - rowHeight * 0.45f)
        send(MotionEvent.ACTION_DOWN, listOf(0 to start))
        send(MotionEvent.ACTION_MOVE, listOf(0 to end))
        send(MotionEvent.ACTION_UP, listOf(0 to end))

        assertEquals(listOf<KeyboardOutput>(KeyboardOutput.Character("ñ")), outputs)
    }

    @Test
    fun `vertical swipe on the space bar types a space while English is disabled`() {
        createView()
        val start = cellCenter(13)
        tap(start)
        val tapOutput = outputs.toList()
        outputs.clear()

        val end = floatArrayOf(start[0], start[1] - rowHeight * 0.45f)
        send(MotionEvent.ACTION_DOWN, listOf(0 to start))
        send(MotionEvent.ACTION_MOVE, listOf(0 to end))
        send(MotionEvent.ACTION_UP, listOf(0 to end))

        assertEquals(tapOutput, outputs)
        assertEquals(Language.SPANISH, layoutManager.currentLanguage)
    }

    @Test
    fun `cancel drops the gesture without typing`() {
        createView()
        send(MotionEvent.ACTION_DOWN, listOf(0 to cellCenter(0)))
        send(MotionEvent.ACTION_CANCEL, listOf(0 to cellCenter(0)))
        send(MotionEvent.ACTION_UP, listOf(0 to cellCenter(0)))
        assertTrue(outputs.isEmpty())
    }

    @Test
    fun `accessibility exposes keys with tap and swipe actions`() {
        createView()
        val provider = view.accessibilityNodeProvider!!

        val node = AccessibilityNodeInfoCompat.wrap(provider.createAccessibilityNodeInfo(1)!!)
        assertTrue(node.contentDescription.toString().startsWith("n"))
        assertTrue(node.actionList.any { it.id == R.id.a11y_swipe_n })

        provider.performAction(1, AccessibilityNodeInfoCompat.ACTION_CLICK, null)
        provider.performAction(1, R.id.a11y_swipe_n, null)
        assertEquals(listOf(KeyboardOutput.Character("n"), KeyboardOutput.Character("ñ")), outputs)
    }

    @Test
    fun `a tap on space that slides a little still types the space`() {
        createView()
        val start = cellCenter(13)
        val end = floatArrayOf(start[0] + rowHeight * 0.45f, start[1])
        send(MotionEvent.ACTION_DOWN, listOf(0 to start))
        send(MotionEvent.ACTION_MOVE, listOf(0 to end))
        send(MotionEvent.ACTION_UP, listOf(0 to end))

        assertEquals(listOf<KeyboardOutput>(KeyboardOutput.Action(KeyAction.Space)), outputs)
    }

    @Test
    fun `dragging on space moves the cursor without typing a space`() {
        createView()
        val start = cellCenter(13)
        val activated = floatArrayOf(start[0] + rowHeight * 0.45f, start[1])
        val end = floatArrayOf(activated[0] + columnWidth, start[1])
        send(MotionEvent.ACTION_DOWN, listOf(0 to start))
        send(MotionEvent.ACTION_MOVE, listOf(0 to activated))
        send(MotionEvent.ACTION_MOVE, listOf(0 to end))
        send(MotionEvent.ACTION_UP, listOf(0 to end))

        assertTrue(outputs.isNotEmpty())
        assertTrue(outputs.all { it == KeyboardOutput.Action(KeyAction.CursorRight) })
    }

    @Test
    fun `delete key swipe left deletes a word`() {
        createView()
        val start = cellCenter(11)
        val end = floatArrayOf(start[0] - columnWidth * 0.45f, start[1])
        send(MotionEvent.ACTION_DOWN, listOf(0 to start))
        send(MotionEvent.ACTION_MOVE, listOf(0 to end))
        send(MotionEvent.ACTION_UP, listOf(0 to end))

        assertEquals(listOf<KeyboardOutput>(KeyboardOutput.Action(KeyAction.DeleteWord)), outputs)
    }

    @Test
    fun `enter label follows the field action`() {
        fun info(
            options: Int,
            type: Int = android.text.InputType.TYPE_CLASS_TEXT,
        ) = EditorInfo().apply {
            imeOptions = options
            inputType = type
        }
        assertEquals("🔍", GestureHintProvider.enterLabelFor(info(EditorInfo.IME_ACTION_SEARCH)))
        assertEquals("➤", GestureHintProvider.enterLabelFor(info(EditorInfo.IME_ACTION_SEND)))
        assertEquals(
            "↵",
            GestureHintProvider.enterLabelFor(info(EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_ENTER_ACTION)),
        )
        val multiline = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        assertEquals("↵", GestureHintProvider.enterLabelFor(info(EditorInfo.IME_ACTION_SEND, multiline)))
    }

    private companion object {
        const val WIDTH = 400
    }
}
