package com.onehandkeyboard.core.gesture

import com.onehandkeyboard.core.domain.model.GestureConfig
import com.onehandkeyboard.core.domain.model.GestureType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Clasificador de tipos de gestos del teclado.
 *
 * Responsabilidades:
 * - Determinar el tipo de gesto basado en métricas de movimiento
 * - Aplicar umbrales configurables para clasificación
 * - Distinguir entre TAP, LONG_PRESS, SWIPE, FLICK y DRAG
 * - Proporcionar lógica de clasificación consistente
 */
@Singleton
class GestureTypeClassifier
    @Inject
    constructor() {
        private var config = GestureConfig()

        /**
         * Determina el tipo de gesto basado en las métricas proporcionadas
         */
        fun classifyGesture(
            distance: Float,
            velocity: Float,
            duration: Long,
        ): GestureType {
            return when {
                // Tap: movimiento corto (sin considerar duración)
                distance <= config.maxTapDistance -> {
                    GestureType.TAP
                }

                // Flick: movimiento rápido
                velocity >= config.flickSpeedThreshold -> {
                    GestureType.FLICK
                }

                // Drag: movimiento con alta velocidad
                velocity > config.flickSpeedThreshold * 0.5f -> {
                    GestureType.DRAG
                }

                // Swipe: movimiento regular dentro del rango aceptable
                distance >= config.minSwipeDistance -> {
                    GestureType.SWIPE
                }

                // Por defecto, tap
                else -> GestureType.TAP
            }
        }

        /**
         * Verifica si un gesto califica como TAP
         */
        fun isTap(
            distance: Float,
            duration: Long,
        ): Boolean {
            return distance <= config.maxTapDistance
        }

        /**
         * Verifica si un gesto califica como SWIPE
         */
        fun isSwipe(
            distance: Float,
            velocity: Float,
        ): Boolean {
            return distance >= config.minSwipeDistance && velocity < config.flickSpeedThreshold
        }

        /**
         * Verifica si un gesto califica como FLICK
         */
        fun isFlick(velocity: Float): Boolean {
            return velocity >= config.flickSpeedThreshold
        }

        /**
         * Verifica si un gesto califica como DRAG
         */
        fun isDrag(
            startCell: Int,
            currentCell: Int,
            distance: Float,
        ): Boolean {
            return startCell != currentCell &&
                startCell != -1 &&
                currentCell != -1 &&
                distance >= config.minSwipeDistance
        }

        /**
         * Obtiene el umbral de distancia máxima para TAP
         */
        fun getMaxTapDistance(): Float {
            return config.maxTapDistance
        }


        /**
         * Obtiene el umbral de distancia mínima para SWIPE
         */
        fun getMinSwipeDistance(): Float {
            return config.minSwipeDistance
        }

        /**
         * Obtiene el umbral de velocidad para FLICK
         */
        fun getFlickSpeedThreshold(): Float {
            return config.flickSpeedThreshold
        }

        /**
         * Clasifica un gesto con información detallada para debugging
         */
        fun classifyWithDetails(
            distance: Float,
            duration: Long,
            velocity: Float,
            startCell: Int,
            currentCell: Int,
        ): GestureClassificationResult {
            val gestureType = classifyGesture(distance, velocity, duration)

            val reasons = mutableListOf<String>()

            when (gestureType) {
                GestureType.TAP -> {
                    reasons.add("Distance ${distance}px <= ${config.maxTapDistance}px")
                }
                GestureType.FLICK -> {
                    reasons.add("Velocity ${velocity}px/s >= ${config.flickSpeedThreshold}px/s")
                }
                GestureType.DRAG -> {
                    reasons.add("Cell changed from $startCell to $currentCell")
                    reasons.add("Distance ${distance}px >= ${config.minSwipeDistance}px")
                }
                GestureType.SWIPE -> {
                    reasons.add("Distance ${distance}px >= ${config.minSwipeDistance}px")
                    reasons.add("Velocity ${velocity}px/s < ${config.flickSpeedThreshold}px/s")
                }
            }

            return GestureClassificationResult(
                gestureType = gestureType,
                distance = distance,
                duration = duration,
                velocity = velocity,
                startCell = startCell,
                currentCell = currentCell,
                reasons = reasons,
            )
        }

        /**
         * Actualiza la configuración del clasificador
         */
        fun updateConfig(newConfig: GestureConfig) {
            this.config = newConfig
        }

        /**
         * Obtiene la configuración actual
         */
        fun getConfig(): GestureConfig {
            return config
        }

        /**
         * Resultado detallado de clasificación de gesto
         */
        data class GestureClassificationResult(
            val gestureType: GestureType,
            val distance: Float,
            val duration: Long,
            val velocity: Float,
            val startCell: Int,
            val currentCell: Int,
            val reasons: List<String>,
        )
    }
