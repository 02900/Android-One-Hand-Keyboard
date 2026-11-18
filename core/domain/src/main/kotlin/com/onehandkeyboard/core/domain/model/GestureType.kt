package com.onehandkeyboard.core.domain.model

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
