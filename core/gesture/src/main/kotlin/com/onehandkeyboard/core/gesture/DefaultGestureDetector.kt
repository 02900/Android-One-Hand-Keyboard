package com.onehandkeyboard.core.gesture

import android.view.MotionEvent
import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.Gesture
import com.onehandkeyboard.core.domain.model.GestureConfig
import com.onehandkeyboard.core.domain.model.GestureType
import com.onehandkeyboard.core.domain.model.Position
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of GestureDetector
 * 
 * This implementation uses a state machine approach to track touch events
 * and detect gestures with configurable thresholds and dead zones.
 */
@Singleton
class DefaultGestureDetector @Inject constructor() : GestureDetector {
    
    private val _gestures = Channel<Gesture>(Channel.UNLIMITED)
    override val gestures: Flow<Gesture> = _gestures.receiveAsFlow()
    
    private var config = GestureConfig()
    private var currentState = GestureState.IDLE
    private var gestureStartTime = 0L
    private var startPosition = Position(0f, 0f)
    private var currentPosition = Position(0f, 0f)
    private var startCell = -1
    private var currentCell = -1
    private var cellBounds: Array<CellBounds> = emptyArray()
    
    // For velocity calculation
    private val velocityTracker = mutableListOf<VelocityPoint>()
    private val maxVelocityPoints = 10
    
    // For direction tracking with hysteresis
    private var lastDetectedDirection: Direction? = null
    private var directionLocked = false
    
    override fun onTouchEvent(event: MotionEvent, cellBounds: Array<CellBounds>): Boolean {
        return try {
            this.cellBounds = cellBounds
            
            println("🎯 DETECTOR: onTouchEvent called - action: ${event.action}, cellBounds.size: ${cellBounds.size}")
            
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    println("🎯 DETECTOR: About to call handleActionDown")
                    handleActionDown(event)
                    println("🎯 DETECTOR: handleActionDown completed")
                }
                MotionEvent.ACTION_MOVE -> {
                    println("🎯 DETECTOR: About to call handleActionMove")
                    handleActionMove(event)
                    println("🎯 DETECTOR: handleActionMove completed")
                }
                MotionEvent.ACTION_UP -> {
                    println("🎯 DETECTOR: About to call handleActionUp")
                    handleActionUp(event)
                    println("🎯 DETECTOR: handleActionUp completed")
                }
                MotionEvent.ACTION_CANCEL -> {
                    println("🎯 DETECTOR: About to call handleActionCancel")
                    handleActionCancel()
                    println("🎯 DETECTOR: handleActionCancel completed")
                }
                else -> {
                    println("🎯 DETECTOR: Unknown action: ${event.action}")
                }
            }
            
            println("🎯 DETECTOR: onTouchEvent returning true")
            true
        } catch (e: Exception) {
            println("🎯 DETECTOR: EXCEPTION in onTouchEvent: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    private fun handleActionDown(event: MotionEvent) {
        val position = Position(event.x, event.y)
        val cell = findCellAt(position)
        
        println("🎯 DETECTOR: handleActionDown - position: $position, cell: $cell")
        
        if (cell == -1) {
            // Touch outside keyboard area
            currentState = GestureState.IDLE
            println("🎯 DETECTOR: Touch outside keyboard area, setting IDLE")
            return
        }
        
        currentState = GestureState.TRACKING
        gestureStartTime = System.currentTimeMillis()
        startPosition = position
        currentPosition = position
        startCell = cell
        currentCell = cell
        
        // Initialize velocity tracking
        velocityTracker.clear()
        addVelocityPoint(position, gestureStartTime)
        
        println("🎯 DETECTOR: State set to TRACKING, startCell: $startCell")
    }
    
    private fun handleActionMove(event: MotionEvent) {
        if (currentState != GestureState.TRACKING) return
        
        val position = Position(event.x, event.y)
        val cell = findCellAt(position)
        
        currentPosition = position
        currentCell = if (cell != -1) cell else currentCell
        
        // Add velocity point
        addVelocityPoint(position, System.currentTimeMillis())
        
        // Check if we should transition to long press
        val duration = System.currentTimeMillis() - gestureStartTime
        val distance = startPosition.distanceTo(currentPosition)
        
        if (duration >= config.longPressThreshold && distance <= config.maxTapDistance) {
            currentState = GestureState.LONG_PRESS
        }
    }
    
    private fun handleActionUp(event: MotionEvent) {
        println("🎯 DETECTOR: handleActionUp called, currentState: $currentState")
        
        if (currentState == GestureState.IDLE) {
            println("🎯 DETECTOR: State is IDLE, returning early")
            return
        }
        
        val endPosition = Position(event.x, event.y)
        val endTime = System.currentTimeMillis()
        val duration = endTime - gestureStartTime
        val distance = startPosition.distanceTo(endPosition)
        val velocity = calculateVelocity()
        
        println("🎯 DETECTOR: duration: $duration, distance: $distance, velocity: $velocity")
        
        // Determine gesture type and direction
        val gestureType = determineGestureType(distance, duration, velocity)
        val direction = if (gestureType == GestureType.TAP || gestureType == GestureType.LONG_PRESS) {
            Direction.CENTER
        } else {
            detectDirectionWithDeadZones(startPosition, endPosition)
        }
        
        println("🎯 DETECTOR: gestureType: $gestureType, direction: $direction")
        
        val gesture = Gesture(
            startCell = startCell,
            endCell = currentCell,
            direction = direction,
            durationMs = duration,
            speed = velocity,
            distance = distance,
            startPosition = startPosition,
            endPosition = endPosition,
            type = gestureType
        )
        
        // Emit the gesture
        val result = _gestures.trySend(gesture)
        println("🎯 GESTURE DETECTOR: Emitting gesture $gesture, result: $result")
        
        // Reset state
        currentState = GestureState.IDLE
        lastDetectedDirection = null
        directionLocked = false
    }
    
    private fun handleActionCancel() {
        currentState = GestureState.IDLE
        velocityTracker.clear()
        lastDetectedDirection = null
        directionLocked = false
    }
    
    private fun findCellAt(position: Position): Int {
        return cellBounds.find { it.contains(position.x, position.y) }?.cellIndex ?: -1
    }
    
    /**
     * Detects direction with dead zones and hysteresis to prevent oscillation
     */
    private fun detectDirectionWithDeadZones(startPos: Position, endPos: Position): Direction {
        val (deltaX, deltaY) = startPos.vectorTo(endPos)
        val distance = startPos.distanceTo(endPos)
        
        // If movement is too small, consider it center/tap
        if (distance < config.minSwipeDistance) {
            return Direction.CENTER
        }
        
        // Calculate angle in degrees (0° = right, 90° = up, etc.)
        val angleRad = kotlin.math.atan2(-deltaY, deltaX) // Negative deltaY for screen coordinates
        val angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()
        val normalizedAngle = if (angleDeg < 0) angleDeg + 360f else angleDeg
        
        // Apply hysteresis if we have a previously locked direction
        if (directionLocked && lastDetectedDirection != null && lastDetectedDirection != Direction.CENTER) {
            val lastDirection = lastDetectedDirection!!
            val lastAngle = lastDirection.angleDegrees
            
            // Calculate angular difference considering wrap-around
            val angleDiff = kotlin.math.abs(normalizedAngle - lastAngle).let { diff ->
                if (diff > 180f) 360f - diff else diff
            }
            
            // If within hysteresis threshold, keep the same direction
            if (angleDiff <= config.directionHysteresis) {
                return lastDirection
            }
        }
        
        // Find the best matching direction considering dead zones
        val candidateDirection = Direction.fromAngle(normalizedAngle)
        
        // Check if we're in a dead zone (between two directions)
        val directionAngle = candidateDirection.angleDegrees
        val angleDiff = kotlin.math.abs(normalizedAngle - directionAngle).let { diff ->
            if (diff > 180f) 360f - diff else diff
        }
        
        // If we're in the dead zone, either keep previous direction or return CENTER
        if (angleDiff > config.directionDeadZone) {
            return if (directionLocked && lastDetectedDirection != null) {
                lastDetectedDirection!!
            } else {
                Direction.CENTER
            }
        }
        
        // Update tracking state
        lastDetectedDirection = candidateDirection
        directionLocked = true
        
        return candidateDirection
    }
    
    private fun addVelocityPoint(position: Position, time: Long) {
        velocityTracker.add(VelocityPoint(position, time))
        
        // Keep only recent points
        if (velocityTracker.size > maxVelocityPoints) {
            velocityTracker.removeAt(0)
        }
    }
    
    private fun calculateVelocity(): Float {
        if (velocityTracker.size < 2) return 0f
        
        val recent = velocityTracker.takeLast(5) // Use last 5 points for velocity
        if (recent.size < 2) return 0f
        
        val first = recent.first()
        val last = recent.last()
        
        val distance = first.position.distanceTo(last.position)
        val timeDelta = (last.time - first.time) / 1000f // Convert to seconds
        
        return if (timeDelta > 0) distance / timeDelta else 0f
    }
    
    private fun determineGestureType(distance: Float, duration: Long, velocity: Float): GestureType {
        return when {
            // Long press: held down for a while without much movement
            duration >= config.longPressThreshold && distance <= config.maxTapDistance -> {
                GestureType.LONG_PRESS
            }
            
            // Tap: quick and short movement
            distance <= config.maxTapDistance -> {
                GestureType.TAP
            }
            
            // Flick: fast movement
            velocity >= config.flickSpeedThreshold -> {
                GestureType.FLICK
            }
            
            // Drag: movement across cells
            startCell != currentCell -> {
                GestureType.DRAG
            }
            
            // Swipe: regular movement within acceptable range
            distance >= config.minSwipeDistance -> {
                GestureType.SWIPE
            }
            
            // Default to tap
            else -> GestureType.TAP
        }
    }
    
    override fun updateConfig(config: GestureConfig) {
        this.config = config
    }
    
    override fun reset() {
        currentState = GestureState.IDLE
        velocityTracker.clear()
        lastDetectedDirection = null
        directionLocked = false
        _gestures.close()
    }
    
    private enum class GestureState {
        IDLE,
        TRACKING,
        LONG_PRESS
    }
    
    private data class VelocityPoint(
        val position: Position,
        val time: Long
    )
}
