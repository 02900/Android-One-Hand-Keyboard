package com.onehandkeyboard.core.domain.model

import com.onehandkeyboard.core.domain.model.Direction.Companion.opposite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Direction is pure data; swipe resolution is tested in core:gesture (SwipeResolverTest).
 */
class DirectionTest {
    @Test
    fun `opposite directions are correct`() {
        assertEquals(Direction.S, Direction.N.opposite())
        assertEquals(Direction.N, Direction.S.opposite())
        assertEquals(Direction.W, Direction.E.opposite())
        assertEquals(Direction.E, Direction.W.opposite())
        assertEquals(Direction.SW, Direction.NE.opposite())
        assertEquals(Direction.NE, Direction.SW.opposite())
        assertEquals(Direction.NW, Direction.SE.opposite())
        assertEquals(Direction.SE, Direction.NW.opposite())
        assertEquals(Direction.CENTER, Direction.CENTER.opposite())
    }

    @Test
    fun `swipeDirections excludes CENTER`() {
        val swipeDirections = Direction.swipeDirections()
        assertEquals(8, swipeDirections.size)
        assertFalse(swipeDirections.contains(Direction.CENTER))
    }

    @Test
    fun `compass angles go clockwise from north in 45 degree steps`() {
        Direction.swipeDirections().forEachIndexed { index, direction ->
            assertEquals(index * 45f, direction.angleDegrees)
        }
    }
}
