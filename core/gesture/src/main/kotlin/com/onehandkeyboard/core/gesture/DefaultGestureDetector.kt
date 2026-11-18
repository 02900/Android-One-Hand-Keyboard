package com.onehandkeyboard.core.gesture

import android.view.MotionEvent
import com.onehandkeyboard.core.domain.model.Gesture
import com.onehandkeyboard.core.domain.model.GestureConfig
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of GestureDetector
 *
 * This implementation coordinates specialized components to detect gestures
 * from touch events using a state machine approach.
 */
@Singleton
class DefaultGestureDetector
    @Inject
    constructor() : GestureDetector {
        private val _gestures = Channel<Gesture>(Channel.UNLIMITED)
        override val gestures: Flow<Gesture> = _gestures.receiveAsFlow()

        // Specialized components for gesture detection
        private val velocityCalculator = VelocityCalculator()
        private val directionDetector = DirectionDetector()
        private val gestureTypeClassifier = GestureTypeClassifier()
        private val touchEventHandler = TouchEventHandler()

        private var config = GestureConfig()
        private var currentState = GestureState.IDLE
        private var gestureStartTime = 0L

        override fun onTouchEvent(
            event: MotionEvent,
            cellBounds: Array<CellBounds>,
        ): Boolean {
            return try {

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        handleActionDown(event, cellBounds)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        handleActionMove(event, cellBounds)
                    }
                    MotionEvent.ACTION_UP -> {
                        handleActionUp(event, cellBounds)
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        handleActionCancel()
                    }
                    else -> {
                    }
                }

                true
            } catch (e: Exception) {
                false
            }
        }

        private fun handleActionDown(
            event: MotionEvent,
            cellBounds: Array<CellBounds>,
        ) {
            touchEventHandler.updateCellBounds(cellBounds)
            val touchInfo = touchEventHandler.processTouchEvent(event)


            if (touchInfo.cell == -1) {
                // Touch outside keyboard area
                currentState = GestureState.IDLE
                return
            }

            currentState = GestureState.TRACKING
            gestureStartTime = touchInfo.timestamp

            // Initialize components with touch data
            velocityCalculator.clear()
            velocityCalculator.addVelocityPoint(touchInfo.position, touchInfo.timestamp)
            directionDetector.reset()

        }

        private fun handleActionMove(
            event: MotionEvent,
            cellBounds: Array<CellBounds>,
        ) {
            if (currentState != GestureState.TRACKING) return

            touchEventHandler.updateCellBounds(cellBounds)
            val touchInfo = touchEventHandler.processTouchEvent(event)

            // Add velocity point
            velocityCalculator.addVelocityPoint(touchInfo.position, touchInfo.timestamp)

            // Check if we should transition to long press
            val duration = touchInfo.timestamp - gestureStartTime
            // For distance calculation, we need to track the start position
            // This will be handled by the gesture type classifier

        }

        private fun handleActionUp(
            event: MotionEvent,
            cellBounds: Array<CellBounds>,
        ) {

            if (currentState == GestureState.IDLE) {
                return
            }

            touchEventHandler.updateCellBounds(cellBounds)
            val touchInfo = touchEventHandler.processTouchEvent(event)
            val duration = touchInfo.timestamp - gestureStartTime
            val velocity = velocityCalculator.calculateVelocity()

            // Get start position from velocity tracker (first point)
            val allPoints = velocityCalculator.getAllPoints()
            val startPosition = if (allPoints.isNotEmpty()) allPoints.first().position else touchInfo.position
            val distance = startPosition.distanceTo(touchInfo.position)


            // Get start cell from first touch
            val startCell =
                if (allPoints.isNotEmpty()) {
                    touchEventHandler.findCellAt(allPoints.first().position)
                } else {
                    touchInfo.cell
                }

            // Determine gesture type and direction using specialized components
            val gestureType =
                gestureTypeClassifier.classifyGesture(
                    distance = distance,
                    velocity = velocity,
                    duration = duration,
                )

            val direction =
                if (gestureType == com.onehandkeyboard.core.domain.model.GestureType.TAP) {
                    com.onehandkeyboard.core.domain.model.Direction.CENTER
                } else {
                    directionDetector.detectDirectionWithDeadZones(startPosition, touchInfo.position)
                }


            val gesture =
                Gesture(
                    startCell = startCell,
                    endCell = touchInfo.cell,
                    direction = direction,
                    durationMs = duration,
                    speed = velocity,
                    distance = distance,
                    startPosition = startPosition,
                    endPosition = touchInfo.position,
                    type = gestureType,
                )

            // Emit the gesture
            val result = _gestures.trySend(gesture)

            // Reset state
            currentState = GestureState.IDLE
            directionDetector.reset()
        }

        private fun handleActionCancel() {
            currentState = GestureState.IDLE
            velocityCalculator.clear()
            directionDetector.reset()
        }

        override fun updateConfig(config: GestureConfig) {
            this.config = config
            // Update config in all components that support it
            directionDetector.updateConfig(config)
            gestureTypeClassifier.updateConfig(config)
        }

        override fun reset() {
            currentState = GestureState.IDLE
            velocityCalculator.clear()
            directionDetector.reset()
            _gestures.close()
        }

        private enum class GestureState {
            IDLE,
            TRACKING,
        }
    }
