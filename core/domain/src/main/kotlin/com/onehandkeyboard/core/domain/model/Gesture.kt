package com.onehandkeyboard.core.domain.model

/**
 * Represents a complete gesture performed on the keyboard
 *
 * A gesture is the result of a touch interaction that starts with ACTION_DOWN,
 * optionally includes ACTION_MOVE events, and ends with ACTION_UP.
 */
data class Gesture(
    /**
     * The cell where the gesture started (0-8 for 3x3 grid)
     */
    val startCell: Int,
    /**
     * The cell where the gesture ended (may be different from startCell)
     */
    val endCell: Int,
    /**
     * The primary direction of the gesture
     */
    val direction: Direction,
    /**
     * Duration of the gesture in milliseconds
     */
    val durationMs: Long,
    /**
     * Average speed of the gesture in pixels per second
     */
    val speed: Float,
    /**
     * Total distance traveled in pixels
     */
    val distance: Float,
    /**
     * Starting position in screen coordinates
     */
    val startPosition: Position,
    /**
     * Ending position in screen coordinates
     */
    val endPosition: Position,
    /**
     * Type of gesture detected
     */
    val type: GestureType,
) {
    /**
     * Whether this gesture crosses cell boundaries
     */
    val isCrossCellGesture: Boolean
        get() = startCell != endCell

    /**
     * Whether this is a long gesture (duration-based)
     */
    val isLongGesture: Boolean
        get() = durationMs >= LONG_PRESS_THRESHOLD_MS

    /**
     * Whether this is a fast gesture (speed-based)
     */
    val isFastGesture: Boolean
        get() = speed >= FAST_GESTURE_THRESHOLD_PPS

    companion object {
        const val LONG_PRESS_THRESHOLD_MS = 500L
        const val FAST_GESTURE_THRESHOLD_PPS = 1000f // pixels per second
    }
}

/**
 * Represents a 2D position in screen coordinates
 */
data class Position(
    val x: Float,
    val y: Float,
) {
    /**
     * Calculate distance to another position
     */
    fun distanceTo(other: Position): Float {
        val dx = x - other.x
        val dy = y - other.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    /**
     * Calculate vector to another position
     */
    fun vectorTo(other: Position): Pair<Float, Float> {
        return (other.x - x) to (other.y - y)
    }
}

/**
 * Types of gestures that can be detected
 */
enum class GestureType {
    /**
     * Simple tap - quick press and release without movement
     */
    TAP,

    /**
     * Swipe - press, move, and release
     */
    SWIPE,

    /**
     * Drag - press, move across cells, and release
     */
    DRAG,

    /**
     * Flick - fast swipe gesture
     */
    FLICK,
}

/**
 * Configuration for gesture detection thresholds
 */
data class GestureConfig(
    /**
     * Minimum distance in pixels to register as a swipe
     */
    val minSwipeDistance: Float = 24f,
    /**
     * Maximum distance in pixels to still be considered a tap
     */
    val maxTapDistance: Float = 12f,
    /**
     * Minimum duration in ms to register as a long press
     */
    val longPressThreshold: Long = 500L,
    /**
     * Minimum speed in pixels/second to register as a flick
     */
    val flickSpeedThreshold: Float = 1000f,
    /**
     * Dead zone angle in degrees for direction detection
     */
    val directionDeadZone: Float = 10f,
    /**
     * Hysteresis angle in degrees to prevent direction oscillation
     */
    val directionHysteresis: Float = 6f,
)
