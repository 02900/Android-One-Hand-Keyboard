package com.onehandkeyboard.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.compose.ui.graphics.toArgb
import com.onehandkeyboard.core.design.theme.ThemeManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

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
    private fun getCurrentTheme() = runBlocking {
        themeManager.currentTheme.first()
    }
    
    /**
     * Crea el fondo del botón con bordes redondeados usando tokens del tema
     */
    fun createButtonBackground(
        pressed: Boolean,
        isBackspace: Boolean = false,
        isControl: Boolean = false,
        isGridCell: Boolean = false,
    ): GradientDrawable {
        val theme = getCurrentTheme()
        
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 12f

            when {
                isBackspace -> {
                    // Usar colores específicos para backspace (rojo)
                    colors =
                        if (pressed) {
                            intArrayOf(Color.parseColor("#D32F2F"), Color.parseColor("#B71C1C"))
                        } else {
                            intArrayOf(Color.parseColor("#F44336"), Color.parseColor("#D32F2F"))
                        }
                }
                isControl -> {
                    // Botones de control usan backgroundSecondary
                    val baseColor = theme.colors.backgroundSecondary.toArgb()
                    colors =
                        if (pressed) {
                            intArrayOf(adjustBrightness(baseColor, 0.8f), adjustBrightness(baseColor, 0.6f))
                        } else {
                            intArrayOf(baseColor, adjustBrightness(baseColor, 0.9f))
                        }
                }
                isGridCell -> {
                    // Celdas del grid usan backgroundPrimary
                    val baseColor = theme.colors.backgroundPrimary.toArgb()
                    colors =
                        if (pressed) {
                            intArrayOf(adjustBrightness(baseColor, 0.8f), adjustBrightness(baseColor, 0.6f))
                        } else {
                            intArrayOf(baseColor, adjustBrightness(baseColor, 0.9f))
                        }
                }
                else -> {
                    // Fallback a backgroundPrimary para compatibilidad
                    val baseColor = theme.colors.backgroundPrimary.toArgb()
                    colors =
                        if (pressed) {
                            intArrayOf(adjustBrightness(baseColor, 0.8f), adjustBrightness(baseColor, 0.6f))
                        } else {
                            intArrayOf(baseColor, adjustBrightness(baseColor, 0.9f))
                        }
                }
            }

            gradientType = GradientDrawable.LINEAR_GRADIENT
            orientation = GradientDrawable.Orientation.TOP_BOTTOM

            // Borde sutil usando color gris por defecto
            setStroke(1, Color.parseColor("#757575"))
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
     * Obtiene el color de texto para diferentes tipos de botones usando tokens del tema
     */
    fun getTextColor(
        isBackspace: Boolean = false,
        isControl: Boolean = false,
        isGridCell: Boolean = false,
    ): Int {
        val theme = getCurrentTheme()
        return when {
            isBackspace -> Color.WHITE // Mantener blanco para backspace rojo
            isControl -> theme.colors.contentSecondary.toArgb() // Texto sobre backgroundSecondary
            isGridCell -> theme.colors.contentPrimary.toArgb() // Texto sobre backgroundPrimary
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
}
