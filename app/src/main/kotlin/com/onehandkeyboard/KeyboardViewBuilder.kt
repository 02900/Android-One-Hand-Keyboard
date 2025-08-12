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
import com.onehandkeyboard.actions.ActionConfig
import com.onehandkeyboard.config.CellConfiguration
import com.onehandkeyboard.gestures.GestureAction
import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.GestureType
import com.onehandkeyboard.core.domain.model.KeyAction
import com.onehandkeyboard.core.domain.model.KeyData

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
    private val uiFactory: KeyboardUiFactory
) {
    
    /**
     * Creates the complete keyboard layout with 3x3 grid and controls
     */
    fun createMainKeyboard(): LinearLayout {
        Log.d("KeyboardViewBuilder", "Creating main keyboard layout")
        
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(8, 8, 8, 8)
            
            // Add title view
            addView(createTitleView())
            
            // Add main content (grid + controls)
            addView(createMainContent())
        }
    }
    
    /**
     * Creates the main content area with 3x3 grid and side controls
     */
    private fun createMainContent(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
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
        val cellSize = calculateSquareCellSize()
        
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                3f // Takes 3/4 of horizontal space
            )
            
            // Create 3 rows
            for (row in 0..2) {
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    
                    // Create 3 columns in each row
                    for (col in 0..2) {
                        val cellIndex = row * 3 + col
                        addView(createGestureButton(keyData[row][col], cellIndex, cellSize))
                    }
                })
            }
        }
    }
    
    /**
     * Creates the side controls column (Layout, Case mode buttons)
     */
    private fun createSideControlsColumn(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f // Takes 1/4 of horizontal space
            )
            setPadding(8, 0, 0, 0)
            
            // Layout button
            addView(createLayoutButton())
            
            // Case mode button (only visible in LETTERS mode)
            if (layoutManager.isLettersMode()) {
                addView(createCaseButton())
            }
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
     * Creates a gesture button with directional labels
     */
    fun createGestureButton(keyData: KeyData, cellIndex: Int, cellSize: Int): FrameLayout {
        return FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(cellSize, cellSize).apply {
                setMargins(2, 2, 2, 2)
            }
            background = createButtonBackground(false)
            
            // Add directional labels
            val cellConfig = layoutManager.getCellConfiguration(cellIndex)
            
            createDirectionalLabelsAbsolute(this, cellConfig, cellIndex)
        }
    }
    
    /**
     * Creates directional labels positioned absolutely within a FrameLayout
     */
    private fun createDirectionalLabelsAbsolute(
        container: FrameLayout, 
        cellConfig: CellConfiguration?, 
        cellIndex: Int
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
     * Creates a TextView positioned for a specific direction
     */
    private fun createDirectionalTextView(
        text: String, 
        direction: Direction, 
        gestureType: GestureType
    ): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 10f
            setTextColor(Color.parseColor("#666666"))
            gravity = Gravity.CENTER
            
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
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
     * Creates center text view for TAP/LONG_PRESS combination
     */
    private fun createCenterTextView(text: String): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 14f
            setTextColor(Color.parseColor("#333333"))
            gravity = Gravity.CENTER
            
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        }
    }
    
    /**
     * Gets the center label combining TAP and LONG_PRESS
     */
    private fun getCenterLabel(cellConfig: CellConfiguration): String {
        val tapAction = cellConfig.actions[GestureAction(GestureType.TAP, Direction.CENTER)]
        val longPressAction = cellConfig.actions[GestureAction(GestureType.LONG_PRESS, Direction.CENTER)]
        
        val tapLabel = tapAction?.let { getActionLabel(it) } ?: ""
        val longPressLabel = longPressAction?.let { getActionLabel(it) } ?: ""
        
        return when {
            tapLabel.isNotEmpty() && longPressLabel.isNotEmpty() -> "$tapLabel ($longPressLabel)"
            tapLabel.isNotEmpty() -> tapLabel
            longPressLabel.isNotEmpty() -> "($longPressLabel)"
            else -> ""
        }
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
     * Creates the title view with status information
     */
    fun createTitleView(): TextView {
        return TextView(context).apply {
            text = "OneHand Keyboard - ${layoutManager.getCurrentLanguageText()} " +
                   "${layoutManager.getCurrentLayoutModeText()} ${layoutManager.getCurrentCaseModeText()}"
            textSize = 14f
            setTextColor(Color.parseColor("#333333"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 8)
            }
        }
    }
    
    /**
     * Creates layout switch button
     */
    fun createLayoutButton(): Button {
        return Button(context).apply {
            text = layoutManager.getCurrentLayoutModeText()
            textSize = 12f
            setTextColor(Color.parseColor("#4CAF50"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 0, 4)
            }
            background = createButtonBackground(false, false, true)
        }
    }
    
    /**
     * Creates case mode switch button
     */
    fun createCaseButton(): Button {
        return Button(context).apply {
            text = layoutManager.getCurrentCaseModeText()
            textSize = 12f
            setTextColor(Color.parseColor("#2196F3"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 0, 4)
            }
            background = createButtonBackground(false, false, true)
        }
    }
    
    /**
     * Creates button background with rounded corners
     */
    private fun createButtonBackground(pressed: Boolean, isBackspace: Boolean = false, isControl: Boolean = false): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = if (isControl) 8f else 12f
            
            if (pressed) {
                setColor(Color.parseColor("#4A4A4A"))
            } else if (isBackspace) {
                setColor(Color.parseColor("#2D2D2D"))
            } else if (isControl) {
                setColor(Color.parseColor("#2A2A2A"))
            } else {
                setColor(Color.parseColor("#3A3A3A"))
            }
            
            setStroke(2, Color.parseColor("#555555"))
        }
    }
}
