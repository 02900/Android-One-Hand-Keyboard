package com.onehandkeyboard.core.gesture

import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.GestureConfig
import com.onehandkeyboard.core.domain.model.Position
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * Detector de direcciones para gestos del teclado.
 * 
 * Responsabilidades:
 * - Detectar direcciones de gestos con dead zones configurables
 * - Aplicar histeresis para prevenir oscilación entre direcciones
 * - Normalizar ángulos y calcular diferencias angulares
 * - Gestionar el estado de bloqueo de dirección
 */
@Singleton
class DirectionDetector @Inject constructor() {
    
    private var config = GestureConfig()
    
    // Para rastreo de dirección con histeresis
    private var lastDetectedDirection: Direction? = null
    private var directionLocked = false
    
    /**
     * Detecta la dirección con dead zones y histeresis para prevenir oscilación
     */
    fun detectDirectionWithDeadZones(startPos: Position, endPos: Position): Direction {
        val deltaX = endPos.x - startPos.x
        val deltaY = endPos.y - startPos.y
        
        // Si el movimiento es muy pequeño, considerarlo como CENTER
        val distance = sqrt(deltaX * deltaX + deltaY * deltaY)
        if (distance < config.minSwipeDistance) {
            return Direction.CENTER
        }
        
        // Calcular ángulo en grados (0° = Este, 90° = Norte, etc.)
        val angleRadians = atan2(-deltaY, deltaX) // -deltaY porque Y crece hacia abajo en Android
        val angleDegrees = Math.toDegrees(angleRadians.toDouble()).toFloat()
        val normalizedAngle = normalizeAngle(angleDegrees)
        
        // Aplicar histeresis si tenemos una dirección previamente bloqueada
        if (directionLocked && lastDetectedDirection != null && lastDetectedDirection != Direction.CENTER) {
            val lastDirection = lastDetectedDirection!!
            val lastAngle = lastDirection.angleDegrees
            
            // Calcular diferencia angular considerando wrap-around
            val angleDiff = abs(normalizedAngle - lastAngle).let { diff ->
                if (diff > 180f) 360f - diff else diff
            }
            
            // Si está dentro del umbral de histeresis, mantener la misma dirección
            if (angleDiff <= config.directionHysteresis) {
                return lastDirection
            }
        }
        
        // Encontrar la mejor dirección coincidente considerando dead zones
        val candidateDirection = Direction.fromAngle(normalizedAngle)
        
        // Verificar si estamos en una dead zone (entre dos direcciones)
        val directionAngle = candidateDirection.angleDegrees
        val angleDiff = abs(normalizedAngle - directionAngle).let { diff ->
            if (diff > 180f) 360f - diff else diff
        }
        
        // Si estamos en la dead zone, mantener dirección previa o retornar CENTER
        if (angleDiff > config.directionDeadZone) {
            return if (directionLocked && lastDetectedDirection != null) {
                lastDetectedDirection!!
            } else {
                Direction.CENTER
            }
        }
        
        // Actualizar estado de rastreo
        lastDetectedDirection = candidateDirection
        directionLocked = true
        
        return candidateDirection
    }
    
    /**
     * Normaliza un ángulo al rango [0, 360)
     */
    fun normalizeAngle(angle: Float): Float {
        var normalized = angle % 360f
        if (normalized < 0f) {
            normalized += 360f
        }
        return normalized
    }
    
    /**
     * Calcula la diferencia angular más corta entre dos ángulos
     */
    fun calculateAngularDifference(angle1: Float, angle2: Float): Float {
        val diff = abs(angle1 - angle2)
        return if (diff > 180f) 360f - diff else diff
    }
    
    /**
     * Verifica si un ángulo está dentro de una dead zone
     */
    fun isInDeadZone(angle: Float, targetDirection: Direction): Boolean {
        val targetAngle = targetDirection.angleDegrees
        val angleDiff = calculateAngularDifference(angle, targetAngle)
        return angleDiff > config.directionDeadZone
    }
    
    /**
     * Obtiene la dirección más cercana a un ángulo dado
     */
    fun getClosestDirection(angle: Float): Direction {
        val normalizedAngle = normalizeAngle(angle)
        return Direction.fromAngle(normalizedAngle)
    }
    
    /**
     * Verifica si el movimiento es suficiente para ser considerado un gesto direccional
     */
    fun isSufficientMovement(startPos: Position, endPos: Position): Boolean {
        val distance = startPos.distanceTo(endPos)
        return distance >= config.minSwipeDistance
    }
    
    /**
     * Calcula la distancia entre dos posiciones
     */
    fun calculateDistance(startPos: Position, endPos: Position): Float {
        return startPos.distanceTo(endPos)
    }
    
    /**
     * Obtiene la última dirección detectada
     */
    fun getLastDetectedDirection(): Direction? {
        return lastDetectedDirection
    }
    
    /**
     * Verifica si la dirección está bloqueada (histeresis activa)
     */
    fun isDirectionLocked(): Boolean {
        return directionLocked
    }
    
    /**
     * Actualiza la configuración del detector
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
     * Reinicia el estado del detector
     */
    fun reset() {
        lastDetectedDirection = null
        directionLocked = false
    }
    
    /**
     * Fuerza el bloqueo de una dirección específica
     */
    fun lockDirection(direction: Direction) {
        lastDetectedDirection = direction
        directionLocked = true
    }
    
    /**
     * Desbloquea la dirección actual
     */
    fun unlockDirection() {
        directionLocked = false
    }
}
