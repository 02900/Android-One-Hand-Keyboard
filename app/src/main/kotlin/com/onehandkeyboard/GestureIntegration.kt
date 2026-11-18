package com.onehandkeyboard

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.onehandkeyboard.actions.ActionConfig
import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.GestureType
import com.onehandkeyboard.core.domain.model.KeyAction
import com.onehandkeyboard.core.domain.model.KeyboardOutput
import com.onehandkeyboard.core.gesture.GestureTracker
import com.onehandkeyboard.gestures.GestureAction
import com.onehandkeyboard.ui.KeyFeedback
import kotlin.math.max
import kotlin.math.min

/**
 * Touch controller for the keyboard cells.
 *
 * - One [GestureTracker] track per pointer: roll-over typing never mixes fingers.
 * - Swipe threshold scales with the cell size (and never goes below the system touch slop).
 * - Swipes snap to the closest configured direction, or fall back to a tap.
 * - Instant feedback on ACTION_DOWN (pressed state, haptic, optional sound).
 * - Holding backspace repeats with acceleration.
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
    }

    var onOutput: (KeyboardOutput) -> Unit = {}
    var cellListener: CellListener? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private val handler = Handler(Looper.getMainLooper())

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

    fun onTouch(
        view: View,
        event: MotionEvent,
        cellIndex: Int,
    ): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> onDown(view, event, cellIndex)
            MotionEvent.ACTION_MOVE -> onMove(view, event)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> onUp(event)
            MotionEvent.ACTION_CANCEL -> cancelAll()
        }
        return true
    }

    /** Drops every in-flight gesture (e.g. when the keyboard view is replaced or hidden). */
    fun cancelAll() {
        tracker.activePointers.toList().forEach { pointerId ->
            tracker.track(pointerId)?.let { cellListener?.onCellReleased(it.cellIndex) }
        }
        tracker.cancelAll()
        stopRepeat()
    }

    private fun onDown(
        view: View,
        event: MotionEvent,
        cellIndex: Int,
    ) {
        val index = event.actionIndex
        val pointerId = event.getPointerId(index)
        val cell = layoutManager.getCellConfiguration(cellIndex)
        tracker.down(
            pointerId = pointerId,
            cellIndex = cellIndex,
            x = event.getX(index),
            y = event.getY(index),
            time = event.eventTime,
            thresholdPx = swipeThreshold(view),
            configured = cell?.getDirectionsFor(GestureType.SWIPE)?.toSet() ?: emptySet(),
        )
        cellListener?.onCellPressed(cellIndex)

        val tapAction = cell?.getActionFor(GestureType.TAP, Direction.CENTER)
        feedback.keyDown(view, soundFor(tapAction))
        if (tapAction == ActionConfig.Backspace) startRepeat(pointerId)
    }

    private fun onMove(
        view: View,
        event: MotionEvent,
    ) {
        for (i in 0 until event.pointerCount) {
            val pointerId = event.getPointerId(i)
            val changed = tracker.move(pointerId, event.getX(i), event.getY(i)) ?: continue
            val track = tracker.track(pointerId) ?: continue
            if (pointerId == repeatPointerId && changed != Direction.CENTER) stopRepeat()
            if (changed != Direction.CENTER) feedback.directionTick(view)
            cellListener?.onDirectionChanged(track.cellIndex, changed)
        }
    }

    private fun onUp(event: MotionEvent) {
        val index = event.actionIndex
        val pointerId = event.getPointerId(index)
        val result = tracker.up(pointerId, event.getX(index), event.getY(index), event.eventTime) ?: return
        val consumedByRepeat = pointerId == repeatPointerId && repeatFired
        if (pointerId == repeatPointerId) stopRepeat()
        cellListener?.onCellReleased(result.cellIndex)

        if (consumedByRepeat && result.direction == Direction.CENTER) return
        val gestureType = if (result.direction == Direction.CENTER) GestureType.TAP else GestureType.SWIPE
        resolveOutput(result.cellIndex, gestureType, result.direction)?.let(onOutput)
    }

    /**
     * Maps a gesture on a cell to its output using the active configuration.
     */
    fun resolveOutput(
        cellIndex: Int,
        gestureType: GestureType,
        direction: Direction,
    ): KeyboardOutput? {
        if (!GestureAction.isValidGestureAction(gestureType, direction)) return null
        val cell = layoutManager.getCellConfiguration(cellIndex) ?: return null
        val actionConfig = cell.getActionFor(gestureType, direction) ?: return null
        return toKeyboardOutput(actionConfig)
    }

    private fun swipeThreshold(view: View): Float {
        val cellSize = min(view.width, view.height).toFloat()
        return max(touchSlop * SLOP_MULTIPLIER, cellSize * THRESHOLD_CELL_RATIO)
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

    private fun soundFor(action: ActionConfig?): KeyFeedback.Sound =
        when (action) {
            ActionConfig.Backspace -> KeyFeedback.Sound.DELETE
            ActionConfig.Space -> KeyFeedback.Sound.SPACE
            ActionConfig.Enter -> KeyFeedback.Sound.RETURN
            else -> KeyFeedback.Sound.STANDARD
        }

    private fun toKeyboardOutput(actionConfig: ActionConfig): KeyboardOutput? =
        when (actionConfig) {
            is ActionConfig.Character -> KeyboardOutput.Character(layoutManager.applyCase(actionConfig.char.toString()))
            is ActionConfig.Text -> KeyboardOutput.Character(layoutManager.applyCase(actionConfig.text))
            is ActionConfig.Action -> KeyboardOutput.Action(actionConfig.keyAction)
            is ActionConfig.SwitchLayout -> KeyboardOutput.Action(KeyAction.SwitchToNumbers)
            is ActionConfig.SwitchCase -> KeyboardOutput.Action(KeyAction.ToggleCase)
            is ActionConfig.Backspace -> KeyboardOutput.Action(KeyAction.Backspace)
            is ActionConfig.Space -> KeyboardOutput.Action(KeyAction.Space)
            is ActionConfig.Enter -> KeyboardOutput.Action(KeyAction.Enter)
            is ActionConfig.Submit -> KeyboardOutput.Action(KeyAction.Submit)
            is ActionConfig.ShowNumbers -> KeyboardOutput.Action(KeyAction.SwitchToNumbers)
            is ActionConfig.ShowLetters -> KeyboardOutput.Action(KeyAction.SwitchToLetters)
            is ActionConfig.Paste -> KeyboardOutput.Action(KeyAction.Paste)
            is ActionConfig.CopyAll -> KeyboardOutput.Action(KeyAction.CopyAll)
            is ActionConfig.DeleteAll -> KeyboardOutput.Action(KeyAction.DeleteAll)
            is ActionConfig.IncreaseCellHeight -> KeyboardOutput.Action(KeyAction.IncreaseCellHeight)
            is ActionConfig.DecreaseCellHeight -> KeyboardOutput.Action(KeyAction.DecreaseCellHeight)
            is ActionConfig.ResetCellHeight -> KeyboardOutput.Action(KeyAction.ResetCellHeight)
            is ActionConfig.CustomFunction -> null
        }

    fun cleanup() {
        cancelAll()
    }

    private companion object {
        const val NO_POINTER = -1
        const val SLOP_MULTIPLIER = 1.5f
        const val THRESHOLD_CELL_RATIO = 0.22f
        const val REPEAT_INITIAL_DELAY_MS = 350L
        const val REPEAT_START_INTERVAL_MS = 80L
        const val REPEAT_MIN_INTERVAL_MS = 30L
        const val REPEAT_ACCELERATION_MS = 5L
    }
}
