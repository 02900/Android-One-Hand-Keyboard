package com.onehandkeyboard.core.gesture

import com.onehandkeyboard.core.domain.model.Direction
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Pure, forgiving swipe-direction resolver.
 *
 * - Uniform 45° sectors computed with `atan2` (screen coordinates, y grows downwards).
 * - Below [thresholdPx] the gesture is a tap ([Direction.CENTER]). Once a swipe was
 *   recognised it only falls back to a tap below `thresholdPx * returnToTapRatio`, so the
 *   preview does not flicker around the threshold.
 * - Hysteresis: the previously resolved direction is kept until the finger is clearly
 *   inside another sector ([hysteresisDegrees] past the boundary).
 * - Snapping: if the raw direction has no action in the cell, the closest configured
 *   direction within [snapToleranceDegrees] is used; otherwise the gesture becomes a tap.
 *   A cell without any swipe always resolves to a tap.
 */
class SwipeResolver(
    private val hysteresisDegrees: Float = 8f,
    private val snapToleranceDegrees: Float = 67.5f,
    private val returnToTapRatio: Float = 0.75f,
) {
    /**
     * @param configured swipe directions that have an action in the cell, or `null` to
     *   return the raw direction without snapping.
     * @param previous the direction resolved for the previous event of the same gesture.
     */
    fun resolve(
        dx: Float,
        dy: Float,
        thresholdPx: Float,
        configured: Set<Direction>? = null,
        previous: Direction = Direction.CENTER,
    ): Direction {
        val effectiveThreshold = if (previous == Direction.CENTER) thresholdPx else thresholdPx * returnToTapRatio
        val angle = compassAngle(dx, dy)
        val raw = SECTORS[((angle / SECTOR).roundToInt()) % SECTORS.size]
        return when {
            configured != null && configured.none { it != Direction.CENTER } -> Direction.CENTER
            hypot(dx, dy) < effectiveThreshold -> Direction.CENTER
            keepsPrevious(angle, previous, configured) -> previous
            configured == null || raw in configured -> raw
            else -> snap(angle, configured)
        }
    }

    /** Hysteresis: stay on the previous direction until clearly inside another sector. */
    private fun keepsPrevious(
        angle: Float,
        previous: Direction,
        configured: Set<Direction>?,
    ): Boolean {
        if (previous == Direction.CENTER) return false
        val nearPrevious = angularDistance(angle, previous.angleDegrees) <= HALF_SECTOR + hysteresisDegrees
        return nearPrevious && (configured == null || previous in configured)
    }

    /** Closest configured direction within the snap tolerance, otherwise a tap. */
    private fun snap(
        angle: Float,
        configured: Set<Direction>,
    ): Direction =
        configured
            .filter { it != Direction.CENTER }
            .map { it to angularDistance(angle, it.angleDegrees) }
            .filter { (_, distanceDeg) -> distanceDeg <= snapToleranceDegrees }
            .minByOrNull { (_, distanceDeg) -> distanceDeg }
            ?.first
            ?: Direction.CENTER

    companion object {
        private const val FULL_TURN = 360f
        private const val HALF_TURN = 180f
        private const val SECTOR = 45f
        private const val HALF_SECTOR = SECTOR / 2
        private val SECTORS =
            listOf(
                Direction.N,
                Direction.NE,
                Direction.E,
                Direction.SE,
                Direction.S,
                Direction.SW,
                Direction.W,
                Direction.NW,
            )

        /** 0° = up (N), 90° = right (E), 180° = down (S), 270° = left (W). */
        fun compassAngle(
            dx: Float,
            dy: Float,
        ): Float {
            val degrees = Math.toDegrees(atan2(dx.toDouble(), (-dy).toDouble())).toFloat()
            return (degrees + FULL_TURN) % FULL_TURN
        }

        fun angularDistance(
            a: Float,
            b: Float,
        ): Float {
            val diff = abs(a - b) % FULL_TURN
            return if (diff > HALF_TURN) FULL_TURN - diff else diff
        }
    }
}
