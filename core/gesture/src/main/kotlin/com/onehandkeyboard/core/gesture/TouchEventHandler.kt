package com.onehandkeyboard.core.gesture

import android.view.MotionEvent
import com.onehandkeyboard.core.domain.model.Position
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manejador de eventos de toque para gestos del teclado.
 * 
 * Responsabilidades:
 * - Procesar eventos de MotionEvent (DOWN, MOVE, UP, CANCEL)
 * - Encontrar celdas basadas en posiciones de toque
 * - Gestionar el estado de seguimiento de gestos
 * - Coordinar con otros componentes para detección completa
 */
@Singleton
class TouchEventHandler @Inject constructor() {
    
    private var cellBounds: Array<CellBounds> = emptyArray()
    
    /**
     * Procesa un evento de toque y extrae información relevante
     */
    fun processTouchEvent(event: MotionEvent): TouchEventInfo {
        val position = Position(event.x, event.y)
        val cell = findCellAt(position)
        val timestamp = System.currentTimeMillis()
        
        return TouchEventInfo(
            action = event.action,
            position = position,
            cell = cell,
            timestamp = timestamp,
            eventTime = event.eventTime,
            downTime = event.downTime
        )
    }
    
    /**
     * Encuentra la celda en la posición especificada
     */
    fun findCellAt(position: Position): Int {
        for (bounds in cellBounds) {
            if (bounds.contains(position.x, position.y)) {
                return bounds.cellIndex
            }
        }
        return -1 // No encontrada
    }
    
    /**
     * Actualiza los límites de las celdas
     */
    fun updateCellBounds(bounds: Array<CellBounds>) {
        this.cellBounds = bounds
    }
    
    /**
     * Obtiene los límites de las celdas actuales
     */
    fun getCellBounds(): Array<CellBounds> {
        return cellBounds
    }
    
    /**
     * Verifica si una posición está dentro del área del teclado
     */
    fun isWithinKeyboardArea(position: Position): Boolean {
        return findCellAt(position) != -1
    }
    
    /**
     * Obtiene información de la celda en una posición específica
     */
    fun getCellInfo(position: Position): CellInfo? {
        val cellIndex = findCellAt(position)
        if (cellIndex == -1) return null
        
        val bounds = cellBounds.find { it.cellIndex == cellIndex } ?: return null
        return CellInfo(
            index = cellIndex,
            bounds = bounds,
            center = Position(bounds.center.first, bounds.center.second)
        )
    }
    
    /**
     * Calcula la distancia desde una posición al centro de una celda
     */
    fun getDistanceToCell(position: Position, cellIndex: Int): Float {
        val bounds = cellBounds.find { it.cellIndex == cellIndex } ?: return Float.MAX_VALUE
        
        val center = Position(bounds.center.first, bounds.center.second)
        return position.distanceTo(center)
    }
    
    /**
     * Obtiene la celda más cercana a una posición
     */
    fun getClosestCell(position: Position): Int {
        if (cellBounds.isEmpty()) return -1
        
        var closestCell = cellBounds.first().cellIndex
        var minDistance = getDistanceToCell(position, closestCell)
        
        for (bounds in cellBounds) {
            val distance = getDistanceToCell(position, bounds.cellIndex)
            if (distance < minDistance) {
                minDistance = distance
                closestCell = bounds.cellIndex
            }
        }
        
        return closestCell
    }
    
    /**
     * Verifica si el toque cambió de celda
     */
    fun didCellChange(previousCell: Int, currentCell: Int): Boolean {
        return previousCell != currentCell && previousCell != -1 && currentCell != -1
    }
    
    /**
     * Obtiene todas las celdas que intersectan con un área rectangular
     */
    fun getCellsInArea(topLeft: Position, bottomRight: Position): List<Int> {
        val cells = mutableListOf<Int>()
        
        for (bounds in cellBounds) {
            // Verificar intersección
            if (bounds.left <= bottomRight.x && 
                bounds.right >= topLeft.x && 
                bounds.top <= bottomRight.y && 
                bounds.bottom >= topLeft.y) {
                cells.add(bounds.cellIndex)
            }
        }
        
        return cells
    }
    
    /**
     * Información de evento de toque procesado
     */
    data class TouchEventInfo(
        val action: Int,
        val position: Position,
        val cell: Int,
        val timestamp: Long,
        val eventTime: Long,
        val downTime: Long
    )
    
    /**
     * Información detallada de una celda
     */
    data class CellInfo(
        val index: Int,
        val bounds: CellBounds,
        val center: Position
    )
}


