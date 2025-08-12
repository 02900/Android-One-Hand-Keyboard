package com.onehandkeyboard.core.gesture

import com.onehandkeyboard.core.domain.model.Position
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calculadora de velocidad para gestos del teclado.
 * 
 * Responsabilidades:
 * - Rastrear puntos de velocidad durante el movimiento
 * - Calcular la velocidad basada en puntos recientes
 * - Gestionar el historial de puntos de velocidad
 * - Proporcionar métricas de velocidad para clasificación de gestos
 */
@Singleton
class VelocityCalculator @Inject constructor() {
    
    private val velocityTracker = mutableListOf<VelocityPoint>()
    private val maxVelocityPoints = 10
    
    /**
     * Agrega un punto de velocidad al tracker
     */
    fun addVelocityPoint(position: Position, time: Long) {
        velocityTracker.add(VelocityPoint(position, time))
        
        // Mantener solo puntos recientes
        if (velocityTracker.size > maxVelocityPoints) {
            velocityTracker.removeAt(0)
        }
    }
    
    /**
     * Calcula la velocidad actual basada en los puntos recientes
     */
    fun calculateVelocity(): Float {
        if (velocityTracker.size < 2) return 0f
        
        val recent = velocityTracker.takeLast(5) // Usar últimos 5 puntos para velocidad
        if (recent.size < 2) return 0f
        
        val first = recent.first()
        val last = recent.last()
        
        val distance = first.position.distanceTo(last.position)
        val timeDelta = (last.time - first.time) / 1000f // Convertir a segundos
        
        return if (timeDelta > 0) distance / timeDelta else 0f
    }
    
    /**
     * Obtiene la velocidad promedio durante todo el gesto
     */
    fun getAverageVelocity(): Float {
        if (velocityTracker.size < 2) return 0f
        
        val first = velocityTracker.first()
        val last = velocityTracker.last()
        
        val distance = first.position.distanceTo(last.position)
        val timeDelta = (last.time - first.time) / 1000f
        
        return if (timeDelta > 0) distance / timeDelta else 0f
    }
    
    /**
     * Obtiene la velocidad máxima registrada
     */
    fun getMaxVelocity(): Float {
        if (velocityTracker.size < 2) return 0f
        
        var maxVelocity = 0f
        
        for (i in 1 until velocityTracker.size) {
            val prev = velocityTracker[i - 1]
            val curr = velocityTracker[i]
            
            val distance = prev.position.distanceTo(curr.position)
            val timeDelta = (curr.time - prev.time) / 1000f
            
            if (timeDelta > 0) {
                val velocity = distance / timeDelta
                if (velocity > maxVelocity) {
                    maxVelocity = velocity
                }
            }
        }
        
        return maxVelocity
    }
    
    /**
     * Obtiene el número de puntos de velocidad registrados
     */
    fun getPointCount(): Int {
        return velocityTracker.size
    }
    
    /**
     * Verifica si hay suficientes puntos para calcular velocidad
     */
    fun hasSufficientData(): Boolean {
        return velocityTracker.size >= 2
    }
    
    /**
     * Limpia todos los puntos de velocidad
     */
    fun clear() {
        velocityTracker.clear()
    }
    
    /**
     * Obtiene todos los puntos de velocidad (para debugging)
     */
    fun getAllPoints(): List<VelocityPoint> {
        return velocityTracker.toList()
    }
    
    /**
     * Punto de velocidad que contiene posición y tiempo
     */
    data class VelocityPoint(
        val position: Position,
        val time: Long
    )
}
