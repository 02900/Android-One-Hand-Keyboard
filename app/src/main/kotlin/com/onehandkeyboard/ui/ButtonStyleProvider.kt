package com.onehandkeyboard.ui

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import androidx.compose.ui.graphics.toArgb
import com.onehandkeyboard.core.design.theme.ThemeManager
import kotlin.math.roundToInt

/**
 * Proveedor de estilos para botones del teclado.
 *
 * Responsabilidades:
 * - Crear fondos con gradientes y bordes redondeados usando tokens del tema
 * - Definir colores para diferentes tipos de botones
 * - Gestionar estados visuales (pressed, normal, control)
 */
class ButtonStyleProvider(
    private val context: Context,
    private val themeManager: ThemeManager,
) {
    /**
     * Gets current theme tokens synchronously
     */
    private fun getCurrentTheme() = themeManager.currentTheme.value
    
    /**
     * Crea el fondo de una celda o botón usando los tokens del tema.
     *
     * Las celdas del grid llevan un borde de 1dp solo a la derecha y abajo (excepto en la
     * última columna/fila), para que las líneas interiores no queden dobles.
     */
    fun createButtonBackground(
        pressed: Boolean = false,
        isBackspace: Boolean = false,
        isControl: Boolean = false,
        isGridCell: Boolean = false,
        cellIndex: Int = -1,
        row: Int = -1,
        column: Int = -1,
        totalRows: Int = 4,
        totalColumns: Int = 4,
    ): Drawable {
        val colors = getCurrentTheme().colors
        val fillColor =
            when {
                isBackspace || isControl -> colors.backgroundSecondary.toArgb()
                isGridCell && !isPrimaryCellGroup(cellIndex) -> colors.backgroundSecondary.toArgb()
                else -> colors.backgroundPrimary.toArgb()
            }
        val fill = ColorDrawable(fillColor)

        if (!isGridCell || row == -1 || column == -1) return fill

        val borderPx = (context.resources.displayMetrics.density * BORDER_DP).roundToInt().coerceAtLeast(1)
        val right = if (column < totalColumns - 1) borderPx else 0
        val bottom = if (row < totalRows - 1) borderPx else 0
        if (right == 0 && bottom == 0) return fill

        return LayerDrawable(arrayOf(ColorDrawable(colors.backgroundRoot.toArgb()), fill)).apply {
            setLayerInset(1, 0, 0, right, bottom)
        }
    }

    /**
     * Ajusta el brillo de un color
     */
    private fun adjustBrightness(color: Int, factor: Float): Int {
        val r = ((color shr 16) and 0xFF)
        val g = ((color shr 8) and 0xFF)
        val b = (color and 0xFF)
        
        val newR = (r * factor).toInt().coerceIn(0, 255)
        val newG = (g * factor).toInt().coerceIn(0, 255)
        val newB = (b * factor).toInt().coerceIn(0, 255)
        
        return (0xFF shl 24) or (newR shl 16) or (newG shl 8) or newB
    }

    /**
     * Determina si una celda pertenece al grupo primario (00-02, 10-12, 20-22)
     */
    private fun isPrimaryCellGroup(cellIndex: Int): Boolean {
        return when (cellIndex) {
            0, 1, 2,    // Row 0: 00, 01, 02
            4, 5, 6,    // Row 1: 10, 11, 12 (mapped as 4, 5, 6)
            8, 9, 10    // Row 2: 20, 21, 22 (mapped as 8, 9, 10)
            -> true
            else -> false // 03, 13, 23, 30, 31, 32, 33 are secondary
        }
    }

    /**
     * Obtiene el color de texto para diferentes tipos de botones usando tokens del tema
     */
    fun getTextColor(
        isBackspace: Boolean = false,
        isControl: Boolean = false,
        isGridCell: Boolean = false,
        cellIndex: Int = -1, // Índice de celda para aplicar colores diferenciados
    ): Int {
        val theme = getCurrentTheme()
        return when {
            isBackspace -> theme.colors.contentSecondary.toArgb() // Texto sobre backgroundSecondary
            isControl -> theme.colors.contentSecondary.toArgb() // Texto sobre backgroundSecondary
            isGridCell -> {
                // Aplicar colores de texto diferenciados según la posición de la celda
                if (isPrimaryCellGroup(cellIndex)) {
                    theme.colors.contentPrimary.toArgb() // Texto sobre backgroundPrimary
                } else {
                    theme.colors.contentSecondary.toArgb() // Texto sobre backgroundSecondary
                }
            }
            else -> theme.colors.contentPrimary.toArgb() // Fallback a contentPrimary
        }
    }

    /**
     * Obtiene el tamaño de texto estándar para botones
     */
    fun getStandardTextSize(): Float = 16f

    /**
     * Obtiene el tamaño de texto para botones de control
     */
    fun getControlTextSize(): Float = 12f

    /**
     * Obtiene el tamaño de texto para labels direccionales
     */
    fun getDirectionalLabelTextSize(): Float = 10f

    /**
     * Obtiene el color para labels direccionales usando tokens del tema
     */
    fun getDirectionalLabelColor(): Int {
        val theme = getCurrentTheme()
        return theme.colors.contentPrimary.toArgb() // Usar contentPrimary para labels direccionales
    }

    /**
     * Obtiene el color para el texto del centro (combinado) usando tokens del tema
     */
    fun getCenterTextColor(): Int {
        val theme = getCurrentTheme()
        return theme.colors.contentPrimary.toArgb() // Usar contentPrimary para texto del centro
    }

    /**
     * Obtiene el tamaño de texto para el centro
     */
    fun getCenterTextSize(): Float = 14f

    private companion object {
        const val BORDER_DP = 1f
    }
}
