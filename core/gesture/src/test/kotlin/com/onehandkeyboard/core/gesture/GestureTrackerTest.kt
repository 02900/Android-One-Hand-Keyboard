package com.onehandkeyboard.core.gesture

import com.onehandkeyboard.core.domain.model.Direction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GestureTrackerTest {
    private val tracker = GestureTracker()
    private val all = Direction.values().filter { it != Direction.CENTER }.toSet()

    @Test
    fun `roll-over typing keeps each finger's origin`() {
        // Finger 0 lands on cell 1, finger 1 lands on cell 2 far to the right before finger 0 lifts.
        tracker.down(0, cellIndex = 1, x = 100f, y = 100f, time = 0, thresholdPx = 40f, configured = all)
        tracker.down(1, cellIndex = 2, x = 300f, y = 100f, time = 50, thresholdPx = 40f, configured = all)

        val first = tracker.up(0, x = 102f, y = 101f, time = 80)
        val second = tracker.up(1, x = 301f, y = 99f, time = 120)

        assertEquals(GestureTracker.Result(1, Direction.CENTER, 80), first)
        assertEquals(GestureTracker.Result(2, Direction.CENTER, 70), second)
    }

    @Test
    fun `move reports direction changes only once`() {
        tracker.down(0, 5, 0f, 0f, 0, 40f, all)

        assertNull(tracker.move(0, 10f, 0f))
        assertEquals(Direction.E, tracker.move(0, 60f, 0f))
        assertNull(tracker.move(0, 80f, 2f))
        assertEquals(Direction.CENTER, tracker.move(0, 5f, 0f))
    }

    @Test
    fun `cancel discards the gesture`() {
        tracker.down(0, 5, 0f, 0f, 0, 40f, all)
        tracker.cancel(0)

        assertNull(tracker.up(0, 60f, 0f, 10))
    }
}
