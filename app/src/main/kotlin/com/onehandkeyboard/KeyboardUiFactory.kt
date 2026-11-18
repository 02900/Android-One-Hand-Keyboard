package com.onehandkeyboard

import android.content.Context
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.onehandkeyboard.KeyboardLayoutManager
import com.onehandkeyboard.actions.ActionConfig
import com.onehandkeyboard.config.CellConfiguration
import com.onehandkeyboard.core.design.theme.ThemeManager
import com.onehandkeyboard.prefs.KeyboardPreferences
import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.KeyAction
import com.onehandkeyboard.core.domain.model.KeyData
import com.onehandkeyboard.ui.ButtonStyleProvider
import com.onehandkeyboard.ui.ControlButtonFactory
import com.onehandkeyboard.ui.DirectionalLabelFactory
import com.onehandkeyboard.ui.GestureHintProvider
import com.onehandkeyboard.ui.LayoutCalculator

/**
 * Factory class for creating keyboard UI components using Android Views.
 *
 * This class coordinates UI creation by delegating to specialized components:
 * - LayoutCalculator: Handles sizing and positioning calculations
 * - ButtonStyleProvider: Manages visual styles and themes
 * - GestureHintProvider: Generates gesture hints and symbols
 * - ControlButtonFactory: Creates control buttons (layout, case, backspace)
 * - DirectionalLabelFactory: Creates positioned directional labels
 *
 * Refactored for better separation of concerns and modularity.
 * Uses manual initialization due to Context dependency requirements.
 */
class KeyboardUiFactory(
    private val context: Context,
    private val layoutManager: KeyboardLayoutManager,
    private val themeManager: ThemeManager,
    prefs: KeyboardPreferences? = null,
) {
    // Delegated components for specialized responsibilities
    val layoutCalculator = LayoutCalculator(context, prefs)
    private val _styleProvider = ButtonStyleProvider(context, themeManager)
    val hintProvider = GestureHintProvider(layoutManager)
    private val controlButtonFactory = ControlButtonFactory(context, layoutCalculator, _styleProvider)
    private val labelFactory = DirectionalLabelFactory(context, _styleProvider, hintProvider)
    
    // Public accessor for styleProvider
    val styleProvider: ButtonStyleProvider get() = _styleProvider

    // ============================================================================
    // REGION: LAYOUT CALCULATIONS (Delegated)
    // ============================================================================

    /**
     * Calcula la altura de celda configurable
     */
    fun calculateCellHeight(): Int {
        return layoutCalculator.calculateCellHeight()
    }
    
    /**
     * @deprecated Use calculateCellHeight() instead
     */
    @Deprecated("Use calculateCellHeight() for flexible sizing")
    fun calculateSquareCellSize(): Int {
        return layoutCalculator.calculateSquareCellSize()
    }

    // ============================================================================
    // REGION: BUTTON CREATION
    // ============================================================================

    /**
     * Crea una celda con labels posicionados absolutamente usando configuración externa
     */
    fun createGestureButton(
        keyData: KeyData,
        cellIndex: Int,
        onTouchListener: (MotionEvent, Int) -> Boolean,
    ): FrameLayout {
        return FrameLayout(context).apply {
            // Obtener configuración de la celda
            val cellConfig = layoutManager.keyboardConfiguration.cells.find { it.cellIndex == cellIndex }

            // Layout parameters con altura configurable y ancho flexible - sin márgenes
            val cellHeight = layoutCalculator.calculateCellHeight()
            layoutParams =
                LinearLayout.LayoutParams(
                    0,
                    cellHeight, // Altura configurable
                    1f, // Peso 1 para usar todo el ancho disponible
                ).apply {
                    setMargins(0, 0, 0, 0) // Sin márgenes entre celdas
                }

            // Fondo con bordes redondeados usando backgroundPrimary para celdas del grid
            // Calcular fila y columna desde cellIndex para bordes selectivos
            val row = cellIndex / 4
            val column = cellIndex % 4
            background = _styleProvider.createButtonBackground(
                pressed = false, 
                isGridCell = true, 
                cellIndex = cellIndex,
                row = row,
                column = column,
                totalRows = 4,
                totalColumns = 4
            )

            // Crear labels posicionados para cada dirección
            labelFactory.createDirectionalLabelsAbsolute(this, cellConfig, cellIndex)

            // Configurar touch listener para gestos
            setOnTouchListener { _, event -> onTouchListener(event, cellIndex) }
        }
    }

    /**
     * Crea el botón de backspace mejorado
     */
    fun createBackspaceButton(onClickListener: () -> Unit): FrameLayout {
        return controlButtonFactory.createBackspaceButton(onClickListener)
    }

    /**
     * Crea un botón mejorado con diseño moderno (método legacy)
     */
    fun createLegacyButton(
        keyData: KeyData,
        onClickListener: (String) -> Unit,
    ): FrameLayout {
        return controlButtonFactory.createLegacyButton(keyData, onClickListener)
    }

    // ============================================================================
    // REGION: VISUAL HELPERS & BACKGROUNDS
    // ============================================================================

    // Removed: createButtonBackground - now handled by ButtonStyleProvider

    /**
     * Crea la vista del título con información de estado
     */
    fun createTitleView(): android.widget.TextView {
        return labelFactory.createTitleView()
    }

    // ============================================================================
    // REGION: DIRECTIONAL LABELS & TEXT VIEWS
    // ============================================================================

    // Removed: createDirectionalLabelsAbsolute - now handled by DirectionalLabelFactory

    // Removed: createDirectionalTextView - now handled by DirectionalLabelFactory

    // Removed: createCenterCombinedTextView - now handled by DirectionalLabelFactory

    /**
     * Crea el texto con labels direccionales para una celda
     */
    fun createDirectionalLabels(
        cellConfig: CellConfiguration?,
        cellIndex: Int,
    ): String {
        return labelFactory.createDirectionalLabels(cellConfig, cellIndex)
    }

    /**
     * Obtiene el label para una dirección específica
     */
    fun getDirectionalLabel(
        cellConfig: CellConfiguration,
        direction: Direction,
    ): String {
        return hintProvider.getDirectionalLabel(cellConfig, direction)
    }

    /**
     * Obtiene el label para el centro (combinando TAP y LONG_PRESS)
     */
    fun getCenterLabel(cellConfig: CellConfiguration): String {
        return hintProvider.getCenterLabel(cellConfig)
    }

    // ============================================================================
    // REGION: CONFIGURATION HINTS & SYMBOLS
    // ============================================================================

    /**
     * Obtiene hints visuales de los gestos configurados para una celda
     */
    fun getConfigurationHints(cellIndex: Int): String {
        return hintProvider.getConfigurationHints(cellIndex)
    }

    /**
     * Obtiene el símbolo visual para una dirección
     */
    fun getDirectionSymbol(direction: Direction): String {
        return hintProvider.getDirectionSymbol(direction)
    }

    /**
     * Obtiene hints visuales de los gestos disponibles para una celda (método legacy)
     */
    fun getLegacyGestureHints(cellIndex: Int): String {
        return hintProvider.getLegacyGestureHints(cellIndex)
    }

    /**
     * Obtiene el símbolo visual para una acción
     */
    fun getActionSymbol(action: KeyAction): String {
        return hintProvider.getActionSymbol(action)
    }

    // ============================================================================
    // REGION: CONTROL BUTTONS
    // ============================================================================

    /**
     * Crea el botón individual para cambiar layout (para columna lateral)
     */
    fun createLayoutButton(onClickListener: () -> Unit): android.widget.Button {
        return controlButtonFactory.createLayoutButton(onClickListener)
    }

    /**
     * Crea el botón individual para cambiar case (para columna lateral)
     */
    fun createCaseButton(onClickListener: () -> Unit): android.widget.Button {
        return controlButtonFactory.createCaseButton(onClickListener)
    }
}
