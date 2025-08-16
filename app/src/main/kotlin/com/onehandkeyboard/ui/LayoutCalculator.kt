package com.onehandkeyboard.ui

import android.content.Context

/**
 * Calculadora de layouts y dimensiones para el teclado.
 *
 * Responsabilidades:
 * - Calcular dimensiones de celdas con altura configurable
 * - Determinar dimensiones basadas en el tamaño de pantalla
 * - Gestionar márgenes y espaciado del grid
 */
class LayoutCalculator(private val context: Context) {
    // Altura configurable de celda en dp
    private val cellHeightDp = 60f
    
    /**
     * Calcula la altura de celda en pixels basada en la configuración
     */
    fun calculateCellHeight(): Int {
        val displayMetrics = context.resources.displayMetrics
        return (cellHeightDp * displayMetrics.density).toInt()
    }
    
    /**
     * Calcula el ancho de celda basado en el ancho disponible para el grid
     */
    fun calculateCellWidth(): Int {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val gridWidth = (screenWidth * 0.75).toInt() // 75% del ancho para el grid (25% para controles)
        val cellWidth = (gridWidth - 60) / 3 // 60px para márgenes totales entre 3 celdas
        return cellWidth
    }
    
    /**
     * @deprecated Use calculateCellWidth() and calculateCellHeight() instead
     */
    @Deprecated("Use calculateCellWidth() and calculateCellHeight() for flexible sizing")
    fun calculateSquareCellSize(): Int {
        return calculateCellWidth()
    }

    /**
     * Calcula el ancho disponible para el grid (75% de la pantalla)
     */
    fun calculateGridWidth(): Int {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        return (screenWidth * 0.75).toInt()
    }

    /**
     * Calcula el ancho disponible para los controles laterales (25% de la pantalla)
     */
    fun calculateControlsWidth(): Int {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        return (screenWidth * 0.25).toInt()
    }

    /**
     * Obtiene los márgenes estándar para las celdas
     */
    fun getStandardCellMargins(): Int = 6

    /**
     * Obtiene el margen total entre celdas para cálculos
     */
    fun getTotalCellMargins(): Int = 60
}
