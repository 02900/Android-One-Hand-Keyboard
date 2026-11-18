package com.onehandkeyboard.core.gesture

import com.onehandkeyboard.core.domain.model.Direction

/**
 * Tracks every finger independently (keyed by pointer id) so fast roll-over typing never
 * mixes the origin of one finger with the release of another.
 *
 * Pure Kotlin: coordinates and times come from the caller.
 */
class GestureTracker(
    private val resolver: SwipeResolver = SwipeResolver(),
) {
    data class Track(
        val pointerId: Int,
        val cellIndex: Int,
        val startX: Float,
        val startY: Float,
        val startTime: Long,
        val thresholdPx: Float,
        val configured: Set<Direction>?,
        var direction: Direction = Direction.CENTER,
        var lastX: Float = startX,
        var lastY: Float = startY,
    )

    /** Result of a finished gesture. [Direction.CENTER] means tap. */
    data class Result(
        val cellIndex: Int,
        val direction: Direction,
        val durationMs: Long,
    )

    private val tracks = LinkedHashMap<Int, Track>()

    val activePointers: Set<Int> get() = tracks.keys

    fun track(pointerId: Int): Track? = tracks[pointerId]

    @Suppress("LongParameterList") // one value per property of the new Track
    fun down(
        pointerId: Int,
        cellIndex: Int,
        x: Float,
        y: Float,
        time: Long,
        thresholdPx: Float,
        configured: Set<Direction>?,
    ): Track = Track(pointerId, cellIndex, x, y, time, thresholdPx, configured).also { tracks[pointerId] = it }

    /** Updates the finger position. Returns the new direction only when it changed. */
    fun move(
        pointerId: Int,
        x: Float,
        y: Float,
    ): Direction? {
        val track = tracks[pointerId] ?: return null
        track.lastX = x
        track.lastY = y
        val resolved = resolve(track, x, y)
        if (resolved == track.direction) return null
        track.direction = resolved
        return resolved
    }

    fun up(
        pointerId: Int,
        x: Float,
        y: Float,
        time: Long,
    ): Result? {
        val track = tracks.remove(pointerId) ?: return null
        val direction = resolve(track, x, y)
        return Result(track.cellIndex, direction, time - track.startTime)
    }

    fun cancel(pointerId: Int) {
        tracks.remove(pointerId)
    }

    fun cancelAll() {
        tracks.clear()
    }

    private fun resolve(
        track: Track,
        x: Float,
        y: Float,
    ): Direction =
        resolver.resolve(
            dx = x - track.startX,
            dy = y - track.startY,
            thresholdPx = track.thresholdPx,
            configured = track.configured,
            previous = track.direction,
        )
}
