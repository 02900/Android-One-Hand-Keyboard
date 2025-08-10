package com.onehandkeyboard.core.domain.model

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Represents the 8 cardinal and intercardinal directions plus center (tap)
 * 
 * The directions are arranged in a circle starting from North (0°) and going clockwise:
 * - N (North): 0° / 360°
 * - NE (Northeast): 45°
 * - E (East): 90°
 * - SE (Southeast): 135°
 * - S (South): 180°
 * - SW (Southwest): 225°
 * - W (West): 270°
 * - NW (Northwest): 315°
 * - CENTER: No movement (tap)
 */
enum class Direction(
    val angleDegrees: Float,
    val displayName: String
) {
    CENTER(0f, "Center"),
    N(0f, "North"),
    NE(45f, "Northeast"),
    E(90f, "East"),
    SE(135f, "Southeast"),
    S(180f, "South"),
    SW(225f, "Southwest"),
    W(270f, "West"),
    NW(315f, "Northwest");

    companion object {
        /**
         * Determines the direction based on a vector from start to end point
         * 
         * @param deltaX Horizontal displacement (positive = right)
         * @param deltaY Vertical displacement (positive = down, following Android coordinate system)
         * @param minDistance Minimum distance required to register as a swipe (otherwise CENTER)
         * @return The closest Direction enum value
         */
        fun fromVector(
            deltaX: Float,
            deltaY: Float,
            minDistance: Float = 0f
        ): Direction {
            val distance = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)
            
            // If movement is too small, consider it a tap
            if (distance < minDistance) {
                return CENTER
            }
            
            // Calculate angle in radians, then convert to degrees
            // atan2 returns angle from -π to π, we need 0 to 2π
            var angleRadians = atan2(deltaY, deltaX)
            if (angleRadians < 0) {
                angleRadians += 2 * PI.toFloat()
            }
            
            val angleDegrees = Math.toDegrees(angleRadians.toDouble()).toFloat()
            
            // Map angle to direction with 45° sectors
            // Add 22.5° offset so that 0° ± 22.5° maps to East (right)
            val adjustedAngle = (angleDegrees + 22.5f) % 360f
            
            return when {
                adjustedAngle < 45f -> E      // 0° ± 22.5° (right)
                adjustedAngle < 90f -> SE     // 45° ± 22.5°
                adjustedAngle < 135f -> S     // 90° ± 22.5° (down)
                adjustedAngle < 180f -> SW    // 135° ± 22.5°
                adjustedAngle < 225f -> W     // 180° ± 22.5° (left)
                adjustedAngle < 270f -> NW    // 225° ± 22.5°
                adjustedAngle < 315f -> N     // 270° ± 22.5° (up)
                adjustedAngle < 360f -> NE    // 315° ± 22.5°
                else -> E // Fallback
            }
        }
        
        /**
         * Determines the direction based on an angle in degrees
         * 
         * @param angleDegrees Angle in degrees (0° = East/right, 90° = South/down, etc.)
         * @return The closest Direction enum value
         */
        fun fromAngle(angleDegrees: Float): Direction {
            // Normalize angle to 0-360 range
            val normalizedAngle = ((angleDegrees % 360f) + 360f) % 360f
            
            // Map angle to direction with 45° sectors
            // Add 22.5° offset so that each direction covers ±22.5° around its center
            val adjustedAngle = (normalizedAngle + 22.5f) % 360f
            
            return when {
                adjustedAngle < 45f -> E      // 0° ± 22.5° (right)
                adjustedAngle < 90f -> SE     // 45° ± 22.5°
                adjustedAngle < 135f -> S     // 90° ± 22.5° (down)
                adjustedAngle < 180f -> SW    // 135° ± 22.5°
                adjustedAngle < 225f -> W     // 180° ± 22.5° (left)
                adjustedAngle < 270f -> NW    // 225° ± 22.5°
                adjustedAngle < 315f -> N     // 270° ± 22.5° (up)
                adjustedAngle < 360f -> NE    // 315° ± 22.5°
                else -> E // Fallback
            }
        }
        
        /**
         * Get all swipe directions (excluding CENTER)
         */
        fun swipeDirections(): List<Direction> = values().filter { it != CENTER }
        
        /**
         * Get the opposite direction
         */
        fun Direction.opposite(): Direction = when (this) {
            CENTER -> CENTER
            N -> S
            NE -> SW
            E -> W
            SE -> NW
            S -> N
            SW -> NE
            W -> E
            NW -> SE
        }
        
        /**
         * Get unit vector for this direction
         */
        fun Direction.toUnitVector(): Pair<Float, Float> = when (this) {
            CENTER -> 0f to 0f
            N -> 0f to -1f
            NE -> 0.707f to -0.707f
            E -> 1f to 0f
            SE -> 0.707f to 0.707f
            S -> 0f to 1f
            SW -> -0.707f to 0.707f
            W -> -1f to 0f
            NW -> -0.707f to -0.707f
        }
    }
}
