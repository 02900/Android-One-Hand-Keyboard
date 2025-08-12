package com.onehandkeyboard.core.gesture

import android.view.MotionEvent
import com.onehandkeyboard.core.domain.model.Gesture
import com.onehandkeyboard.core.domain.model.GestureConfig
import kotlinx.coroutines.flow.Flow

/**
 * Interface for detecting gestures from touch events
 *
 * This component processes raw MotionEvent data and converts it into
 * structured Gesture objects that can be consumed by the mapping layer.
 */
interface GestureDetector {
    /**
     * Flow of detected gestures
     *
     * Emits complete gestures when they are recognized from touch events.
     * The flow is hot and will emit gestures as they are detected.
     */
    val gestures: Flow<Gesture>

    /**
     * Process a motion event
     *
     * @param event The MotionEvent to process
     * @param cellBounds Array of cell boundaries for the 3x3 grid
     * @return true if the event was consumed, false otherwise
     */
    fun onTouchEvent(
        event: MotionEvent,
        cellBounds: Array<CellBounds>,
    ): Boolean

    /**
     * Update gesture detection configuration
     *
     * @param config New configuration parameters
     */
    fun updateConfig(config: GestureConfig)

    /**
     * Reset the detector state
     *
     * Clears any ongoing gesture tracking and resets internal state.
     * Should be called when the keyboard is hidden or context changes.
     */
    fun reset()
}

/**
 * Represents the boundaries of a keyboard cell
 */
data class CellBounds(
    val cellIndex: Int,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    /**
     * Check if a point is within this cell
     */
    fun contains(
        x: Float,
        y: Float,
    ): Boolean {
        return x >= left && x <= right && y >= top && y <= bottom
    }

    /**
     * Get the center point of this cell
     */
    val center: Pair<Float, Float>
        get() = (left + right) / 2f to (top + bottom) / 2f

    /**
     * Get the width of this cell
     */
    val width: Float
        get() = right - left

    /**
     * Get the height of this cell
     */
    val height: Float
        get() = bottom - top
}
