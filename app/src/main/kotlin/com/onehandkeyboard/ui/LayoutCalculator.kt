package com.onehandkeyboard.ui

import android.content.Context

/**
 * Calculadora de layouts y dimensiones para el teclado.
 *
 * Responsabilidades:
 * - Calcular tamaños de celdas con aspect ratio 1:1
 * - Determinar dimensiones basadas en el tamaño de pantalla
 * - Gestionar márgenes y espaciado del grid
 */
class LayoutCalculator(private val context: Context) {
    /**
     * Calcula el tamaño de celda cuadrada basado en el ancho disponible para el grid
     */
    fun calculateSquareCellSize(): Int {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val gridWidth = (screenWidth * 0.75).toInt() // 75% del ancho para el grid (25% para controles)
        val cellSize = (gridWidth - 60) / 3 // 60px para márgenes totales entre 3 celdas
        return cellSize
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
