package com.onehandkeyboard.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable

/**
 * Proveedor de estilos para botones del teclado.
 *
 * Responsabilidades:
 * - Crear fondos con gradientes y bordes redondeados
 * - Definir colores para diferentes tipos de botones
 * - Gestionar estados visuales (pressed, normal, control)
 */
class ButtonStyleProvider {
    /**
     * Crea el fondo del botón con bordes redondeados
     */
    fun createButtonBackground(
        pressed: Boolean,
        isBackspace: Boolean = false,
        isControl: Boolean = false,
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 12f

            when {
                isBackspace -> {
                    colors =
                        if (pressed) {
                            intArrayOf(Color.parseColor("#D32F2F"), Color.parseColor("#B71C1C"))
                        } else {
                            intArrayOf(Color.parseColor("#F44336"), Color.parseColor("#D32F2F"))
                        }
                }
                isControl -> {
                    colors =
                        if (pressed) {
                            intArrayOf(Color.parseColor("#1976D2"), Color.parseColor("#0D47A1"))
                        } else {
                            intArrayOf(Color.parseColor("#2196F3"), Color.parseColor("#1976D2"))
                        }
                }
                else -> {
                    colors =
                        if (pressed) {
                            intArrayOf(Color.parseColor("#424242"), Color.parseColor("#212121"))
                        } else {
                            intArrayOf(Color.parseColor("#616161"), Color.parseColor("#424242"))
                        }
                }
            }

            gradientType = GradientDrawable.LINEAR_GRADIENT
            orientation = GradientDrawable.Orientation.TOP_BOTTOM

            // Borde sutil
            setStroke(1, Color.parseColor("#757575"))
        }
    }

    /**
     * Obtiene el color de texto para diferentes tipos de botones
     */
    fun getTextColor(
        isBackspace: Boolean = false,
        isControl: Boolean = false,
    ): Int {
        return when {
            isBackspace -> Color.WHITE
            isControl -> Color.WHITE
            else -> Color.WHITE
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
     * Obtiene el color para labels direccionales
     */
    fun getDirectionalLabelColor(): Int = Color.parseColor("#E0E0E0")

    /**
     * Obtiene el color para el texto del centro (combinado)
     */
    fun getCenterTextColor(): Int = Color.WHITE

    /**
     * Obtiene el tamaño de texto para el centro
     */
    fun getCenterTextSize(): Float = 14f
}
