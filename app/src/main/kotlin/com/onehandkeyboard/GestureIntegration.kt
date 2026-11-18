package com.onehandkeyboard

import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewConfiguration
import com.onehandkeyboard.actions.ActionConfig
import com.onehandkeyboard.actions.toKeyboardOutput
import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.GestureType
import com.onehandkeyboard.core.domain.model.KeyAction
import com.onehandkeyboard.core.domain.model.KeyboardOutput
import com.onehandkeyboard.core.gesture.GestureTracker
import com.onehandkeyboard.enums.Language
import com.onehandkeyboard.gestures.GestureAction
import com.onehandkeyboard.ui.KeyFeedback
import kotlin.math.abs
import kotlin.math.max

/**
 * Gesture logic of the keyboard keys, fed one pointer at a time by the view layer.
 *
 * - One [GestureTracker] track per pointer: roll-over typing never mixes fingers.
 * - Swipe threshold scales with the key size (and never goes below the system touch slop).
 * - Swipes snap to the closest configured direction, or fall back to a tap.
 * - Instant feedback on pointer down (pressed state, haptic, optional sound) and live
 *   direction preview through [CellListener.onDirectionChanged].
 * - Holding backspace repeats with acceleration.
 * - Space bar: horizontal drag moves the cursor (trackpad), vertical swipe switches language.
 *   A hurried tap that slides a little but never moves the cursor still types the space.
 * - Long-press on the layout key starts a live height resize (vertical drag).
 *
 * Privacy: nothing that reveals what the user types is logged.
 */
class GestureIntegration(
    context: android.content.Context,
    private val layoutManager: KeyboardLayoutManager,
    private val feedback: KeyFeedback,
    private val tracker: GestureTracker = GestureTracker(),
) {
    /** UI hooks implemented by the view layer. */
    interface CellListener {
        fun onCellPressed(cellIndex: Int)

        fun onCellReleased(cellIndex: Int)

        fun onDirectionChanged(
            cellIndex: Int,
            direction: Direction,
        ) {}

        fun onResizeStarted() {}

        /** [totalDyPx]: vertical drag on screen since the resize started (negative = up = taller). */
        fun onResizeDrag(totalDyPx: Float) {}

        fun onResizeEnded() {}
    }

    private enum class Mode { NORMAL, SPACE, LAYOUT_KEY }

    private class PointerState(
        val mode: Mode,
        val view: View,
    ) {
        var trackpadActive = false
        var trackpadAnchorX = 0f
        var trackpadMoved = false
        var verticalSwipe: Direction = Direction.CENTER
        var resizing = false
        var resizeStartScreenY = 0f
        var lastScreenY = 0f
    }

    var onOutput: (KeyboardOutput) -> Unit = {}
    var cellListener: CellListener? = null

    private val density = context.resources.displayMetrics.density
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
    private val handler = Handler(Looper.getMainLooper())
    private val pointers = HashMap<Int, PointerState>()

    private var repeatPointerId = NO_POINTER
    private var repeatFired = false
    private var repeatInterval = REPEAT_START_INTERVAL_MS
    private val repeatRunnable =
        object : Runnable {
            override fun run() {
                repeatFired = true
                onOutput(KeyboardOutput.Action(KeyAction.Backspace))
                repeatInterval = max(REPEAT_MIN_INTERVAL_MS, repeatInterval - REPEAT_ACCELERATION_MS)
                handler.postDelayed(this, repeatInterval)
            }
        }

    private var longPressPointerId = NO_POINTER
    private val longPressRunnable = Runnable { startResize(longPressPointerId) }

    /**
     * A finger lands on [cellIndex]. Coordinates are in the view; [screenY] is the same point
     * in screen coordinates (used by the resize, because the view moves while it grows).
     */
    @Suppress("LongParameterList")
    fun pointerDown(
        view: View,
        pointerId: Int,
        cellIndex: Int,
        x: Float,
        y: Float,
        screenY: Float,
        time: Long,
        cellMinSizePx: Float,
    ) {
        val cell = layoutManager.getCellConfiguration(cellIndex)
        val tapAction = cell?.getActionFor(GestureType.TAP, Direction.CENTER)
        val mode =
            when (tapAction) {
                ActionConfig.Space -> Mode.SPACE
                ActionConfig.SwitchLayout, ActionConfig.ShowNumbers, ActionConfig.ShowLetters -> Mode.LAYOUT_KEY
                else -> Mode.NORMAL
            }
        pointers[pointerId] = PointerState(mode, view).apply { lastScreenY = screenY }
        // The space bar handles its own directions (trackpad / language); other keys snap.
        val configured = if (mode == Mode.SPACE) null else cell?.getDirectionsFor(GestureType.SWIPE)?.toSet().orEmpty()
        tracker.down(
            pointerId = pointerId,
            cellIndex = cellIndex,
            x = x,
            y = y,
            time = time,
            thresholdPx = max(touchSlop * SLOP_MULTIPLIER, cellMinSizePx * THRESHOLD_CELL_RATIO),
            configured = configured,
        )
        cellListener?.onCellPressed(cellIndex)

        feedback.keyDown(view, soundFor(tapAction))
        if (tapAction == ActionConfig.Backspace) startRepeat(pointerId)
        if (mode == Mode.LAYOUT_KEY) scheduleLongPress(pointerId)
    }

    fun pointerMove(
        pointerId: Int,
        x: Float,
        y: Float,
        screenY: Float,
    ) {
        val state = pointers[pointerId] ?: return
        state.lastScreenY = screenY
        when {
            state.resizing -> cellListener?.onResizeDrag(screenY - state.resizeStartScreenY)
            state.mode == Mode.SPACE -> moveOnSpace(pointerId, state, x, y)
            else -> moveOnKey(pointerId, state, x, y)
        }
    }

    fun pointerUp(
        pointerId: Int,
        x: Float,
        y: Float,
        time: Long,
    ) {
        val state = pointers.remove(pointerId)
        val result = tracker.up(pointerId, x, y, time) ?: return
        val consumedByRepeat = pointerId == repeatPointerId && repeatFired
        if (pointerId == repeatPointerId) stopRepeat()
        if (pointerId == longPressPointerId) cancelLongPress()
        cellListener?.onCellReleased(result.cellIndex)

        when {
            state?.resizing == true -> cellListener?.onResizeEnded()
            state?.mode == Mode.SPACE -> {
                when {
                    state.trackpadMoved -> Unit
                    state.verticalSwipe != Direction.CENTER && Language.canSwitch ->
                        onOutput(KeyboardOutput.Action(KeyAction.SwitchLanguage))
                    else -> perform(result.cellIndex, Direction.CENTER)
                }
            }
            consumedByRepeat && result.direction == Direction.CENTER -> Unit
            else -> perform(result.cellIndex, result.direction)
        }
    }

    /** Emits the action of a tap ([Direction.CENTER]) or swipe; also used by accessibility. */
    fun perform(
        cellIndex: Int,
        direction: Direction,
    ) {
        val gestureType = if (direction == Direction.CENTER) GestureType.TAP else GestureType.SWIPE
        resolveOutput(cellIndex, gestureType, direction)?.let(onOutput)
    }

    /** Drops every in-flight gesture (e.g. when the keyboard view is replaced or hidden). */
    fun cancelAll() {
        tracker.activePointers.toList().forEach { pointerId ->
            tracker.track(pointerId)?.let { cellListener?.onCellReleased(it.cellIndex) }
        }
        if (pointers.values.any { it.resizing }) cellListener?.onResizeEnded()
        tracker.cancelAll()
        pointers.clear()
        stopRepeat()
        cancelLongPress()
    }

    /** Maps a gesture on a cell to its output using the active configuration. */
    fun resolveOutput(
        cellIndex: Int,
        gestureType: GestureType,
        direction: Direction,
    ): KeyboardOutput? {
        if (!GestureAction.isValidGestureAction(gestureType, direction)) return null
        val cell = layoutManager.getCellConfiguration(cellIndex) ?: return null
        val actionConfig = cell.getActionFor(gestureType, direction) ?: return null
        return actionConfig.toKeyboardOutput(layoutManager::transformChar)
    }

    private fun moveOnKey(
        pointerId: Int,
        state: PointerState,
        x: Float,
        y: Float,
    ) {
        val changed = tracker.move(pointerId, x, y) ?: return
        val track = tracker.track(pointerId) ?: return
        if (changed != Direction.CENTER) {
            if (pointerId == repeatPointerId) stopRepeat()
            if (pointerId == longPressPointerId) cancelLongPress()
            feedback.directionTick(state.view)
        }
        cellListener?.onDirectionChanged(track.cellIndex, changed)
    }

    private fun moveOnSpace(
        pointerId: Int,
        state: PointerState,
        x: Float,
        y: Float,
    ) {
        val track = tracker.track(pointerId) ?: return
        val dx = x - track.startX
        val dy = y - track.startY
        if (!state.trackpadActive) {
            val raw = tracker.move(pointerId, x, y)
            val horizontal = abs(dx) >= track.thresholdPx && abs(dx) > abs(dy)
            if (horizontal) {
                state.trackpadActive = true
                state.trackpadAnchorX = x
                state.verticalSwipe = Direction.CENTER
                feedback.directionTick(state.view)
                return
            }
            if (raw != null) {
                state.verticalSwipe = if (raw == Direction.N || raw == Direction.S) raw else Direction.CENTER
                if (state.verticalSwipe != Direction.CENTER) feedback.directionTick(state.view)
            }
            return
        }
        val stepPx = TRACKPAD_STEP_DP * density
        val steps = ((x - state.trackpadAnchorX) / stepPx).toInt()
        if (steps != 0) {
            state.trackpadMoved = true
            state.trackpadAnchorX += steps * stepPx
            val action = if (steps < 0) KeyAction.CursorLeft else KeyAction.CursorRight
            repeat(abs(steps)) { onOutput(KeyboardOutput.Action(action)) }
        }
    }

    private fun startRepeat(pointerId: Int) {
        stopRepeat()
        repeatPointerId = pointerId
        repeatInterval = REPEAT_START_INTERVAL_MS
        handler.postDelayed(repeatRunnable, REPEAT_INITIAL_DELAY_MS)
    }

    private fun stopRepeat() {
        handler.removeCallbacks(repeatRunnable)
        repeatPointerId = NO_POINTER
        repeatFired = false
    }

    private fun scheduleLongPress(pointerId: Int) {
        cancelLongPress()
        longPressPointerId = pointerId
        handler.postDelayed(longPressRunnable, longPressTimeout)
    }

    private fun cancelLongPress() {
        handler.removeCallbacks(longPressRunnable)
        longPressPointerId = NO_POINTER
    }

    private fun startResize(pointerId: Int) {
        val state = pointers[pointerId] ?: return
        longPressPointerId = NO_POINTER
        state.resizing = true
        state.resizeStartScreenY = state.lastScreenY
        feedback.longPress(state.view)
        cellListener?.onResizeStarted()
    }

    private fun soundFor(action: ActionConfig?): KeyFeedback.Sound =
        when (action) {
            ActionConfig.Backspace -> KeyFeedback.Sound.DELETE
            ActionConfig.Space -> KeyFeedback.Sound.SPACE
            ActionConfig.Enter -> KeyFeedback.Sound.RETURN
            else -> KeyFeedback.Sound.STANDARD
        }

    fun cleanup() {
        cancelAll()
    }

    private companion object {
        const val NO_POINTER = -1
        const val SLOP_MULTIPLIER = 1.5f
        const val THRESHOLD_CELL_RATIO = 0.22f
        const val TRACKPAD_STEP_DP = 12f
        const val REPEAT_INITIAL_DELAY_MS = 350L
        const val REPEAT_START_INTERVAL_MS = 80L
        const val REPEAT_MIN_INTERVAL_MS = 30L
        const val REPEAT_ACCELERATION_MS = 5L
    }
}
