package com.onehandkeyboard.core.domain.model

import com.onehandkeyboard.core.domain.model.Direction.Companion.toUnitVector
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.sqrt

/**
 * Unit tests for Direction enum and its companion functions
 */
class DirectionTest {

    @Test
    fun `fromVector detects cardinal directions correctly`() {
        // Test cardinal directions (N, E, S, W)
        assertEquals(Direction.E, Direction.fromVector(100f, 0f, 10f))    // Right
        assertEquals(Direction.W, Direction.fromVector(-100f, 0f, 10f))   // Left
        assertEquals(Direction.N, Direction.fromVector(0f, -100f, 10f))   // Up
        assertEquals(Direction.S, Direction.fromVector(0f, 100f, 10f))    // Down
    }

    @Test
    fun `fromVector detects intercardinal directions correctly`() {
        // Test diagonal directions (NE, SE, SW, NW)
        assertEquals(Direction.NE, Direction.fromVector(100f, -100f, 10f))  // Up-Right
        assertEquals(Direction.SE, Direction.fromVector(100f, 100f, 10f))   // Down-Right
        assertEquals(Direction.SW, Direction.fromVector(-100f, 100f, 10f))  // Down-Left
        assertEquals(Direction.NW, Direction.fromVector(-100f, -100f, 10f)) // Up-Left
    }

    @Test
    fun `fromVector returns CENTER for small movements`() {
        // Test that small movements are considered taps
        assertEquals(Direction.CENTER, Direction.fromVector(5f, 5f, 10f))
        assertEquals(Direction.CENTER, Direction.fromVector(0f, 0f, 10f))
        assertEquals(Direction.CENTER, Direction.fromVector(8f, 0f, 10f))
    }

    @Test
    fun `fromVector handles edge cases near direction boundaries`() {
        // Test vectors near the boundaries between directions
        val distance = 100f
        
        // Just inside E sector (22.5° from horizontal)
        val angle22 = Math.toRadians(22.0).toFloat()
        assertEquals(Direction.E, Direction.fromVector(
            distance * kotlin.math.cos(angle22), 
            distance * kotlin.math.sin(angle22), 
            10f
        ))
        
        // Just inside SE sector (67.5° from horizontal)
        val angle67 = Math.toRadians(67.0).toFloat()
        assertEquals(Direction.SE, Direction.fromVector(
            distance * kotlin.math.cos(angle67), 
            distance * kotlin.math.sin(angle67), 
            10f
        ))
    }

    @Test
    fun `fromVector with zero minDistance always returns direction`() {
        // Even tiny movements should return a direction when minDistance is 0
        assertEquals(Direction.E, Direction.fromVector(1f, 0f, 0f))
        assertEquals(Direction.N, Direction.fromVector(0f, -1f, 0f))
    }

    @Test
    fun `opposite directions are correct`() {
        with(Direction.Companion) {
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
    }

    @Test
    fun `toUnitVector should return correct unit vectors for all directions`() {
        assertEquals(Pair(0f, 0f), Direction.CENTER.toUnitVector())
        assertEquals(Pair(0f, -1f), Direction.N.toUnitVector())
        assertEquals(Pair(0.707f, -0.707f), Direction.NE.toUnitVector())
        assertEquals(Pair(1f, 0f), Direction.E.toUnitVector())
        assertEquals(Pair(0.707f, 0.707f), Direction.SE.toUnitVector())
        assertEquals(Pair(0f, 1f), Direction.S.toUnitVector())
        assertEquals(Pair(-0.707f, 0.707f), Direction.SW.toUnitVector())
        assertEquals(Pair(-1f, 0f), Direction.W.toUnitVector())
        assertEquals(Pair(-0.707f, -0.707f), Direction.NW.toUnitVector())
    }

    @Test
    fun `fromAngle should return correct directions for cardinal angles`() {
        assertEquals(Direction.E, Direction.fromAngle(0f))      // East
        assertEquals(Direction.S, Direction.fromAngle(90f))     // South
        assertEquals(Direction.W, Direction.fromAngle(180f))    // West
        assertEquals(Direction.N, Direction.fromAngle(270f))    // North
    }
    
    @Test
    fun `fromAngle should return correct directions for diagonal angles`() {
        assertEquals(Direction.SE, Direction.fromAngle(45f))    // Southeast
        assertEquals(Direction.SW, Direction.fromAngle(135f))   // Southwest
        assertEquals(Direction.NW, Direction.fromAngle(225f))   // Northwest
        assertEquals(Direction.NE, Direction.fromAngle(315f))   // Northeast
    }
    
    @Test
    fun `fromAngle should handle negative angles`() {
        assertEquals(Direction.N, Direction.fromAngle(-90f))    // -90° = 270°
        assertEquals(Direction.W, Direction.fromAngle(-180f))   // -180° = 180°
        assertEquals(Direction.NE, Direction.fromAngle(-45f))   // -45° = 315°
    }
    
    @Test
    fun `fromAngle should handle angles greater than 360`() {
        assertEquals(Direction.E, Direction.fromAngle(360f))    // 360° = 0°
        assertEquals(Direction.S, Direction.fromAngle(450f))    // 450° = 90°
        assertEquals(Direction.W, Direction.fromAngle(540f))    // 540° = 180°
    }
    
    @Test
    fun `fromAngle should handle boundary cases with 22_5 degree tolerance`() {
        // Test basic cases first
        assertEquals(Direction.E, Direction.fromAngle(0f))      // Center of East
        assertEquals(Direction.SE, Direction.fromAngle(45f))    // Center of Southeast
        assertEquals(Direction.S, Direction.fromAngle(90f))     // Center of South
        
        // Test boundary cases - let's be more conservative with the boundaries
        // Based on implementation: adjustedAngle = (normalizedAngle + 22.5f) % 360f
        // E: adjustedAngle < 45f  => normalizedAngle < 22.5f
        // SE: 45f <= adjustedAngle < 90f => 22.5f <= normalizedAngle < 67.5f
        assertEquals(Direction.E, Direction.fromAngle(22.4f))   // Should be East
        assertEquals(Direction.SE, Direction.fromAngle(22.5f))  // Should be Southeast
        assertEquals(Direction.SE, Direction.fromAngle(67.4f))  // Should be Southeast
        assertEquals(Direction.S, Direction.fromAngle(67.5f))   // Should be South
        
        // Test negative angles - let's test simpler cases first
        assertEquals(Direction.E, Direction.fromAngle(-10f))    // Should wrap to East (350°)
        assertEquals(Direction.NE, Direction.fromAngle(-45f))   // Should wrap to Northeast (315°)
    }

    @Test
    fun `swipeDirections excludes CENTER`() {
        val swipeDirections = Direction.swipeDirections()
        assertEquals(8, swipeDirections.size)
        assertEquals(false, swipeDirections.contains(Direction.CENTER))
        
        // Should contain all 8 directional swipes
        assertEquals(true, swipeDirections.contains(Direction.N))
        assertEquals(true, swipeDirections.contains(Direction.NE))
        assertEquals(true, swipeDirections.contains(Direction.E))
        assertEquals(true, swipeDirections.contains(Direction.SE))
        assertEquals(true, swipeDirections.contains(Direction.S))
        assertEquals(true, swipeDirections.contains(Direction.SW))
        assertEquals(true, swipeDirections.contains(Direction.W))
        assertEquals(true, swipeDirections.contains(Direction.NW))
    }

    @Test
    fun `direction angles are correct`() {
        assertEquals(0f, Direction.N.angleDegrees)
        assertEquals(45f, Direction.NE.angleDegrees)
        assertEquals(90f, Direction.E.angleDegrees)
        assertEquals(135f, Direction.SE.angleDegrees)
        assertEquals(180f, Direction.S.angleDegrees)
        assertEquals(225f, Direction.SW.angleDegrees)
        assertEquals(270f, Direction.W.angleDegrees)
        assertEquals(315f, Direction.NW.angleDegrees)
        assertEquals(0f, Direction.CENTER.angleDegrees)
    }

    @Test
    fun `fromVector handles precise angle calculations`() {
        // Test specific angles to ensure correct sector assignment
        val distance = 100f
        
        // Test 0° (East)
        assertEquals(Direction.E, Direction.fromVector(distance, 0f, 10f))
        
        // Test 45° (Southeast) 
        val sqrt2 = sqrt(2f) / 2f
        assertEquals(Direction.SE, Direction.fromVector(
            distance * sqrt2, 
            distance * sqrt2, 
            10f
        ))
        
        // Test 90° (South)
        assertEquals(Direction.S, Direction.fromVector(0f, distance, 10f))
        
        // Test 135° (Southwest)
        assertEquals(Direction.SW, Direction.fromVector(
            -distance * sqrt2, 
            distance * sqrt2, 
            10f
        ))
        
        // Test 180° (West)
        assertEquals(Direction.W, Direction.fromVector(-distance, 0f, 10f))
        
        // Test 225° (Northwest)
        assertEquals(Direction.NW, Direction.fromVector(
            -distance * sqrt2, 
            -distance * sqrt2, 
            10f
        ))
        
        // Test 270° (North)
        assertEquals(Direction.N, Direction.fromVector(0f, -distance, 10f))
        
        // Test 315° (Northeast)
        assertEquals(Direction.NE, Direction.fromVector(
            distance * sqrt2, 
            -distance * sqrt2, 
            10f
        ))
    }
}
