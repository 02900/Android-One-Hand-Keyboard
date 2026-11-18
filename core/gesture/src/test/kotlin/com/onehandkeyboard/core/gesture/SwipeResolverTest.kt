package com.onehandkeyboard.core.gesture

import com.onehandkeyboard.core.domain.model.Direction
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class SwipeResolverTest {
    private val resolver = SwipeResolver()
    private val threshold = 40f
    private val all = Direction.values().filter { it != Direction.CENTER }.toSet()

    /** Vector of [length] px pointing at compass [degrees] (0 = up, 90 = right). */
    private fun vector(
        degrees: Float,
        length: Float = 100f,
    ): Pair<Float, Float> {
        val rad = Math.toRadians(degrees.toDouble())
        return (sin(rad) * length).toFloat() to (-cos(rad) * length).toFloat()
    }

    private fun resolve(
        degrees: Float,
        length: Float = 100f,
        configured: Set<Direction>? = all,
        previous: Direction = Direction.CENTER,
    ): Direction {
        val (dx, dy) = vector(degrees, length)
        return resolver.resolve(dx, dy, threshold, configured, previous)
    }

    @Test
    fun `short movement is a tap`() {
        assertEquals(Direction.CENTER, resolve(90f, length = 39f))
    }

    @Test
    fun `each 45 degree sector maps to its direction`() {
        val expected =
            mapOf(
                0f to Direction.N,
                45f to Direction.NE,
                90f to Direction.E,
                135f to Direction.SE,
                180f to Direction.S,
                225f to Direction.SW,
                270f to Direction.W,
                315f to Direction.NW,
            )
        expected.forEach { (angle, direction) ->
            assertEquals(direction, resolve(angle))
            assertEquals(direction, resolve(angle + 20f))
            assertEquals(direction, resolve(angle - 20f))
        }
    }

    @Test
    fun `diagonals get the same angular width as cardinals`() {
        // 30° from east used to be classified as a diagonal only past 26.6°; now 22.5° is the boundary.
        assertEquals(Direction.NE, resolve(90f - 30f))
        assertEquals(Direction.E, resolve(90f - 20f))
    }

    @Test
    fun `unconfigured direction snaps to the closest configured one`() {
        // Cell only has SE: a sloppy swipe to the east still produces SE.
        assertEquals(Direction.SE, resolve(90f, configured = setOf(Direction.SE)))
    }

    @Test
    fun `direction too far from any configured one becomes a tap`() {
        assertEquals(Direction.CENTER, resolve(315f, configured = setOf(Direction.SE)))
    }

    @Test
    fun `cell without swipes always taps`() {
        assertEquals(Direction.CENTER, resolve(90f, configured = emptySet()))
    }

    @Test
    fun `null configuration returns raw direction`() {
        assertEquals(Direction.W, resolve(270f, configured = null))
    }

    @Test
    fun `hysteresis keeps previous direction near the sector boundary`() {
        // 25° is past the N/NE boundary (22.5°) but within the 8° hysteresis band.
        assertEquals(Direction.N, resolve(25f, previous = Direction.N))
        assertEquals(Direction.NE, resolve(35f, previous = Direction.N))
    }

    @Test
    fun `swipe only returns to tap well below the threshold`() {
        assertEquals(Direction.E, resolve(90f, length = 35f, previous = Direction.E))
        assertEquals(Direction.CENTER, resolve(90f, length = 25f, previous = Direction.E))
    }
}
