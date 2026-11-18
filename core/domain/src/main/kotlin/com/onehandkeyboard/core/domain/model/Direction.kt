package com.onehandkeyboard.core.domain.model

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
 *
 * Swipe resolution (vector to direction) lives in `core:gesture` (SwipeResolver).
 */
enum class Direction(
    val angleDegrees: Float,
    val displayName: String,
) {
    CENTER(0f, "Center"),
    N(0f, "North"),
    NE(45f, "Northeast"),
    E(90f, "East"),
    SE(135f, "Southeast"),
    S(180f, "South"),
    SW(225f, "Southwest"),
    W(270f, "West"),
    NW(315f, "Northwest"),
    ;

    companion object {
        /** All swipe directions (excluding CENTER). */
        fun swipeDirections(): List<Direction> = values().filter { it != CENTER }

        /** The opposite direction. */
        fun Direction.opposite(): Direction =
            when (this) {
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
    }
}
