package com.onehandkeyboard

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import android.widget.GridLayout
import android.widget.LinearLayout
import androidx.compose.ui.graphics.toArgb
import com.onehandkeyboard.core.design.theme.ThemeManager
import com.onehandkeyboard.core.domain.model.KeyData
import com.onehandkeyboard.core.domain.model.KeyboardOutput
import com.onehandkeyboard.core.domain.model.KeyAction
import com.onehandkeyboard.ui.ButtonStyleProvider
import com.onehandkeyboard.config.CellConfiguration
import com.onehandkeyboard.config.KeyboardConfigurationFactory
import com.onehandkeyboard.enums.Language
import com.onehandkeyboard.enums.LayoutMode

/**
 * Unified 4x4 grid keyboard builder
 * 
 * Grid layout:
 * 00 01 02 03
 * 10 11 12 13  
 * 20 21 22 23
 * 30 31 32 33
 * 
 * Cell mapping:
 * - 00-02, 10-12, 20-22: Main 3x3 grid with tap/swipe gestures
 * - 03, 13, 23, 33: Action buttons (layout, case, backspace, etc.)
 * - 30-32: Spacebar (letters mode) or 30-31=0, 32=space (numbers mode)
 */
class UnifiedGridKeyboardBuilder(
    private val context: Context,
    private val themeManager: ThemeManager,
    private val styleProvider: ButtonStyleProvider,
    private val layoutManager: KeyboardLayoutManager,
    private val gestureIntegration: GestureIntegration,
    private val actionProcessor: KeyboardActionProcessor,
    private val uiFactory: KeyboardUiFactory,
    private val configFactory: KeyboardConfigurationFactory
) {
    
    /**
     * Gets current theme tokens synchronously
     */
    private fun getCurrentTheme() = themeManager.currentTheme.value

    /**
     * Creates the unified 4x4 grid keyboard layout
     */
    fun createKeyboardLayout(): LinearLayout {
        Log.d("UnifiedGridKeyboardBuilder", "Creating unified 4x4 grid keyboard layout")

        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            setPadding(0, 0, 0, 0)
            
            // Set background using theme
            val theme = getCurrentTheme()
            setBackgroundColor(theme.colors.backgroundRoot.toArgb())

            // Add the unified 4x4 grid
            addView(createUnifiedGrid())
        }
    }

    /**
     * Creates the unified 4x4 grid with all keyboard elements
     */
    private fun createUnifiedGrid(): GridLayout {
        return GridLayout(context).apply {
            columnCount = 4
            rowCount = 4
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            
            // Get current configuration
            val config = configFactory.getConfiguration(
                layoutManager.currentLanguage,
                layoutManager.currentMode
            )
            
            // Create cells based on configuration
            config.cells.forEach { cellConfig ->
                addView(createConfiguredCell(cellConfig))
            }
        }
    }

    /**
     * Creates a cell based on configuration
     */
    private fun createConfiguredCell(cellConfig: CellConfiguration): android.view.View {
        val row = cellConfig.row
        val col = cellConfig.col
        
        return uiFactory.createGestureButton(
            KeyData("", "", ""), // Will be populated by gesture integration
            cellConfig.cellIndex
        ) { event, index ->
            gestureIntegration.processGestureEvent(event, index) { keyboardOutput ->
                keyboardOutput?.let { actionProcessor.processKeyboardOutput(it) }
            }
            true
        }.apply {
            // Set grid layout parameters with spans
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = uiFactory.calculateCellHeight()
                columnSpec = GridLayout.spec(col, cellConfig.colSpan, 1f)
                rowSpec = GridLayout.spec(row, cellConfig.rowSpan)
                setMargins(0, 0, 0, 0)
            }
        }
    }

}
