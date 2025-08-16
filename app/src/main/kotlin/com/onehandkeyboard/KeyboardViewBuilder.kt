package com.onehandkeyboard

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import com.onehandkeyboard.actions.ActionConfig
import com.onehandkeyboard.config.CellConfiguration
import com.onehandkeyboard.core.design.theme.ThemeManager
import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.GestureType
import com.onehandkeyboard.core.domain.model.KeyAction
import com.onehandkeyboard.core.domain.model.KeyboardOutput
import com.onehandkeyboard.gestures.GestureAction
import com.onehandkeyboard.ui.ButtonStyleProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Builds and creates all keyboard UI components and views.
 *
 * This component handles:
 * - Main keyboard layout construction
 * - Individual button/cell creation with gesture hints
 * - Directional label positioning and styling
 * - Control buttons (Layout, Case mode)
 * - Button backgrounds and visual styling
 * - Title view with status information
 *
 * Extracted from OneHandKeyboardService.kt as part of SRP refactoring.
 */
class KeyboardViewBuilder(
    private val context: Context,
    private val layoutManager: KeyboardLayoutManager,
    private val uiFactory: KeyboardUiFactory,
    private val gestureIntegration: GestureIntegration,
    private val actionProcessor: KeyboardActionProcessor,
    private val themeManager: ThemeManager,
    private val styleProvider: ButtonStyleProvider,
) {
    
    /**
     * Gets current theme tokens synchronously
     */
    private fun getCurrentTheme() = runBlocking {
        themeManager.currentTheme.first()
    }
    /**
     * Creates the complete keyboard layout with 3x3 grid and controls
     */
    fun createMainKeyboard(): LinearLayout {
        Log.d("KeyboardViewBuilder", "Creating main keyboard layout")

        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
            setPadding(0, 0, 0, 0)
            
            // Usar backgroundRoot del tema para el fondo general del teclado IME
            val theme = getCurrentTheme()
            setBackgroundColor(theme.colors.backgroundRoot.toArgb())

            // Add main content (grid + controls)
            addView(createMainContent())

            // Add spacebar row below main content
            addView(createSpacebarRow())
        }
    }

    /**
     * Creates the main content area with 3x3 grid and side controls
     */
    private fun createMainContent(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )

            // Add 3x3 grid (takes most space)
            addView(create3x3Grid())

            // Add side controls column
            addView(createSideControlsColumn())
        }
    }

    /**
     * Creates the 3x3 gesture grid
     */
    private fun create3x3Grid(): LinearLayout {
        val keyData = layoutManager.getCurrentKeyData()

        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams =
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    3f, // Takes 3/4 of horizontal space
                )
            
            // No background on container - will be applied to individual cells

            // Create 3 rows
            for (row in 0..2) {
                addView(
                    LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams =
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                            )

                        // Create 3 columns in each row
                        for (col in 0..2) {
                            val cellIndex = row * 3 + col
                            // Use uiFactory method that has OnTouchListener configured
                            addView(
                                uiFactory.createGestureButton(keyData[row][col], cellIndex) { event, index ->
                                    Log.d("OneHandKeyboard", "🎯 Touch event received on cell $index: ${event.action}")
                                    // Process gesture using the integrated gesture system
                                    gestureIntegration.processGestureDirectly(event, index) { keyboardOutput ->
                                        Log.d(
                                            "OneHandKeyboard",
                                            "🎯 Gesture result ready for processing: $keyboardOutput",
                                        )
                                        // Process the gesture result using KeyboardActionProcessor
                                        keyboardOutput?.let { output ->
                                            Log.d(
                                                "OneHandKeyboard",
                                                "🎯 Processing output with ActionProcessor: $output",
                                            )
                                            actionProcessor.processKeyboardOutput(output)
                                        }
                                    }
                                    true
                                },
                            )
                        }
                    },
                )
            }
        }
    }

    /**
     * Creates the side controls column (Layout, Case mode buttons, Backspace)
     */
    private fun createSideControlsColumn(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams =
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1f, // Takes 1/4 of horizontal space
                )
            setPadding(0, 0, 0, 0) // Sin padding entre grid y columna de controles
            
            // No background on container - will be applied to individual buttons

            // Layout button
            addView(createLayoutButton())

            // Case mode button (only visible in LETTERS mode)
            if (layoutManager.isLettersMode()) {
                addView(createCaseButton())
            }

            // Backspace button
            addView(createBackspaceButton())
        }
    }

    /**
     * Calculates square cell size based on available width
     */
    private fun calculateSquareCellSize(): Int {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val padding = 16 * displayMetrics.density.toInt() // 8dp padding on each side
        val availableWidth = screenWidth - padding
        return (availableWidth * 0.75f / 3).toInt() // 75% for grid, divided by 3 columns
    }

    /**
     * Creates directional labels positioned absolutely within a FrameLayout
     */
    private fun createDirectionalLabelsAbsolute(
        container: FrameLayout,
        cellConfig: CellConfiguration?,
        cellIndex: Int,
    ) {
        cellConfig?.let { config ->
            // Create labels for each configured direction
            config.actions.forEach { (gestureAction, actionConfig) ->
                val direction = gestureAction.direction
                val gestureType = gestureAction.gestureType

                // Skip CENTER directions for now (handled separately)
                if (direction != Direction.CENTER) {
                    val label = getActionLabel(actionConfig)
                    if (label.isNotEmpty()) {
                        container.addView(createDirectionalTextView(label, direction, gestureType))
                    }
                }
            }

            // Handle CENTER (TAP and LONG_PRESS combined)
            val centerLabel = getCenterLabel(config)
            if (centerLabel.isNotEmpty()) {
                container.addView(createCenterTextView(centerLabel))
            }
        }
    }

    /**
     * Creates a TextView positioned for a specific direction using theme tokens
     */
    private fun createDirectionalTextView(
        text: String,
        direction: Direction,
        gestureType: GestureType,
    ): TextView {
        val theme = getCurrentTheme()
        
        return TextView(context).apply {
            this.text = text
            textSize = 10f
            setTextColor(theme.colors.contentPrimary.toArgb())
            gravity = Gravity.CENTER

            layoutParams =
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    // Position based on direction
                    when (direction) {
                        Direction.N -> {
                            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                            setMargins(0, 4, 0, 0)
                        }
                        Direction.NE -> {
                            gravity = Gravity.TOP or Gravity.END
                            setMargins(0, 4, 4, 0)
                        }
                        Direction.E -> {
                            gravity = Gravity.CENTER_VERTICAL or Gravity.END
                            setMargins(0, 0, 4, 0)
                        }
                        Direction.SE -> {
                            gravity = Gravity.BOTTOM or Gravity.END
                            setMargins(0, 0, 4, 4)
                        }
                        Direction.S -> {
                            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                            setMargins(0, 0, 0, 4)
                        }
                        Direction.SW -> {
                            gravity = Gravity.BOTTOM or Gravity.START
                            setMargins(4, 0, 0, 4)
                        }
                        Direction.W -> {
                            gravity = Gravity.CENTER_VERTICAL or Gravity.START
                            setMargins(4, 0, 0, 0)
                        }
                        Direction.NW -> {
                            gravity = Gravity.TOP or Gravity.START
                            setMargins(4, 4, 0, 0)
                        }
                        Direction.CENTER -> {
                            gravity = Gravity.CENTER
                        }
                    }
                }
        }
    }

    /**
     * Creates center text view for TAP/LONG_PRESS combination using theme tokens
     */
    private fun createCenterTextView(text: String): TextView {
        val theme = getCurrentTheme()
        
        return TextView(context).apply {
            this.text = text
            textSize = 14f
            setTextColor(theme.colors.contentPrimary.toArgb())
            gravity = Gravity.CENTER

            layoutParams =
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER,
                )
        }
    }

    /**
     * Gets the center label for TAP gesture
     */
    private fun getCenterLabel(cellConfig: CellConfiguration): String {
        val tapAction = cellConfig.actions[GestureAction(GestureType.TAP, Direction.CENTER)]
        return tapAction?.let { getActionLabel(it) } ?: ""
    }

    /**
     * Gets display label for an action configuration
     */
    private fun getActionLabel(actionConfig: ActionConfig): String {
        return when (actionConfig) {
            is ActionConfig.Character -> actionConfig.char.toString()
            is ActionConfig.Text -> actionConfig.text
            is ActionConfig.Action -> getActionSymbol(actionConfig.keyAction)
        }
    }

    /**
     * Gets symbol representation for keyboard actions
     */
    private fun getActionSymbol(action: KeyAction): String {
        return when (action) {
            is KeyAction.Backspace -> "⌫"
            is KeyAction.Space -> "␣"
            is KeyAction.Enter -> "↵"
            is KeyAction.ToggleCase -> "Aa"
            else -> "⚡"
        }
    }

    /**
     * Creates layout switch button
     */
    fun createLayoutButton(): Button {
        return Button(context).apply {
            text = layoutManager.getCurrentLayoutModeText()
            textSize = 12f
            setTextColor(styleProvider.getTextColor(isControl = true))
            val cellHeight = uiFactory.calculateCellHeight()
            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    cellHeight, // Usar la misma altura que las celdas
                ).apply {
                    setMargins(0, 0, 0, 0) // Sin márgenes entre botones
                }
            background = styleProvider.createButtonBackground(false, isControl = true)
        }
    }

    /**
     * Creates case mode switch button using theme tokens
     */
    fun createCaseButton(): Button {
        val theme = getCurrentTheme()
        
        return Button(context).apply {
            text = layoutManager.getCurrentCaseModeText()
            textSize = 12f
            setTextColor(styleProvider.getTextColor(isControl = true))
            val cellHeight = uiFactory.calculateCellHeight()
            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    cellHeight, // Usar la misma altura que las celdas
                ).apply {
                    setMargins(0, 0, 0, 0) // Sin márgenes entre botones
                }
            background = styleProvider.createButtonBackground(false, isControl = true)
        }
    }

    /**
     * Creates button background with rounded corners using theme tokens
     */
    private fun createButtonBackground(
        pressed: Boolean,
        isBackspace: Boolean = false,
        isControl: Boolean = false,
    ): GradientDrawable {
        val theme = getCurrentTheme()
        
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = theme.borderRadius.md.value

            val backgroundColor = when {
                pressed -> theme.colors.keyBackgroundPressed
                isBackspace -> theme.colors.actionKeyBackground
                isControl -> theme.colors.actionKeyBackground
                else -> theme.colors.keyBackground
            }
            
            setColor(backgroundColor.toArgb())
            setStroke(2, theme.colors.keyBorder.toArgb())
        }
    }

    /**
     * Creates backspace button using theme tokens
     */
    fun createBackspaceButton(): Button {
        val theme = getCurrentTheme()
        
        return Button(context).apply {
            text = "⌫"
            textSize = 16f
            setTextColor(styleProvider.getTextColor(isBackspace = true))
            val cellHeight = uiFactory.calculateCellHeight()
            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    cellHeight, // Usar la misma altura que las celdas
                ).apply {
                    setMargins(0, 0, 0, 0) // Sin márgenes entre botones
                }
            background = styleProvider.createButtonBackground(false, isBackspace = true)
            
            setOnClickListener {
                actionProcessor.processKeyboardOutput(KeyboardOutput.Action(KeyAction.Backspace))
            }
        }
    }

    /**
     * Creates the spacebar row at the bottom
     */
    private fun createSpacebarRow(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
            setPadding(0, 0, 0, 0) // Sin padding entre contenido principal y spacebar
            
            // No background on container - will be applied to individual buttons

            // Create full-width spacebar button
            addView(createFullWidthSpacebar())
        }
    }

    /**
     * Creates a full-width spacebar button using theme tokens
     */
    private fun createFullWidthSpacebar(): Button {
        val theme = getCurrentTheme()
        val cellHeight = uiFactory.calculateCellHeight() // Usar la misma altura que las celdas
        
        return Button(context).apply {
            text = "Espacio"
            layoutParams = LinearLayout.LayoutParams(
                0, // width = 0 to use weight
                cellHeight // Usar la misma altura que las celdas
            ).apply {
                weight = 1.0f // Take all available horizontal space
                setMargins(0, 0, 0, 0) // Sin márgenes en el spacebar
            }
            
            textSize = 16f
            setTextColor(styleProvider.getTextColor(isControl = true))
            // Usar backgroundSecondary para la barra espaciadora (isControl = true)
            background = styleProvider.createButtonBackground(false, isControl = true)
            
            setOnClickListener {
                actionProcessor.processKeyboardOutput(KeyboardOutput.Action(KeyAction.Space))
            }
        }
    }
}
