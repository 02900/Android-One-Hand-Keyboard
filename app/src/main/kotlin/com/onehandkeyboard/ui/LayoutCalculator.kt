package com.onehandkeyboard.ui

import android.content.Context
import com.onehandkeyboard.prefs.KeyboardPreferences

/**
 * Calculadora de layouts y dimensiones para el teclado.
 *
 * Responsabilidades:
 * - Calcular dimensiones de celdas con altura configurable
 * - Determinar dimensiones basadas en el tamaño de pantalla
 * - Gestionar márgenes y espaciado del grid
 */
class LayoutCalculator(
    private val context: Context,
    private val prefs: KeyboardPreferences? = null,
) {
    // Altura configurable de celda en dp, persistida en KeyboardPreferences
    private var cellHeightDp = prefs?.cellHeightDp ?: KeyboardPreferences.DEFAULT_CELL_HEIGHT_DP
        set(value) {
            field = value
            prefs?.cellHeightDp = value
        }

    // Height while the user is dragging the resize handle (not persisted yet)
    private var liveHeightDp: Float? = null

    // Límites para la altura de celda
    private val minCellHeightDp = 40f
    private val maxCellHeightDp = 80f

    /**
     * Calcula la altura de celda en pixels basada en la configuración
     */
    fun calculateCellHeight(): Int {
        val displayMetrics = context.resources.displayMetrics
        return (getCurrentCellHeightDp() * displayMetrics.density).toInt()
    }

    /**
     * Aumenta la altura de celda en 1dp
     * @return true si se pudo aumentar, false si ya está en el máximo
     */
    fun increaseCellHeight(): Boolean {
        return if (cellHeightDp < maxCellHeightDp) {
            cellHeightDp += 1f
            true
        } else {
            false
        }
    }

    /**
     * Disminuye la altura de celda en 1dp
     * @return true si se pudo disminuir, false si ya está en el mínimo
     */
    fun decreaseCellHeight(): Boolean {
        return if (cellHeightDp > minCellHeightDp) {
            cellHeightDp -= 1f
            true
        } else {
            false
        }
    }

    val minHeightDp: Float get() = minCellHeightDp
    val maxHeightDp: Float get() = maxCellHeightDp

    /**
     * Sets the cell height (clamped). During a live resize drag pass `persist = false` and
     * persist once at the end to avoid a disk write per frame.
     */
    fun setCellHeightDp(
        value: Float,
        persist: Boolean = true,
    ) {
        val clamped = value.coerceIn(minCellHeightDp, maxCellHeightDp)
        if (persist) {
            cellHeightDp = clamped
        } else {
            liveHeightDp = clamped
        }
    }

    /** Persists the height reached during a live resize. */
    fun commitLiveHeight() {
        liveHeightDp?.let { cellHeightDp = it }
        liveHeightDp = null
    }

    /**
     * Obtiene la altura actual de celda en dp
     */
    fun getCurrentCellHeightDp(): Float = liveHeightDp ?: cellHeightDp

    /**
     * Resetea la altura de celda al valor por defecto (60dp)
     * @return true si se cambió la altura, false si ya estaba en el valor por defecto
     */
    fun resetCellHeight(): Boolean {
        val defaultHeight = KeyboardPreferences.DEFAULT_CELL_HEIGHT_DP
        return if (cellHeightDp != defaultHeight) {
            cellHeightDp = defaultHeight
            true
        } else {
            false
        }
    }
}
