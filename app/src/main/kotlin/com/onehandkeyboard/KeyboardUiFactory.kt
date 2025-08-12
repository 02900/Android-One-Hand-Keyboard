package com.onehandkeyboard

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.onehandkeyboard.core.domain.model.*
import com.onehandkeyboard.config.*

/**
 * Factory class for creating keyboard UI components
 * 
 * This class centralizes all UI creation logic including:
 * - Button creation and styling
 * - Layout calculations and positioning
 * - Visual helpers and backgrounds
 * - Directional labels and text views
 */
class KeyboardUiFactory(
    private val context: Context,
    private val configManager: KeyboardConfigManager
) {
    
    // ============================================================================
    // REGION: LAYOUT CALCULATIONS
    // ============================================================================
    
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
    
    // ============================================================================
    // REGION: BUTTON CREATION
    // ============================================================================
    
    /**
     * Crea una celda con labels posicionados absolutamente usando configuración externa
     */
    fun createGestureButton(
        keyData: KeyData, 
        cellIndex: Int,
        onTouchListener: (MotionEvent, Int) -> Boolean
    ): FrameLayout {
        return FrameLayout(context).apply {
            // Obtener configuración de la celda
            val cellConfig = configManager.keyboardConfiguration.cells.find { it.cellIndex == cellIndex }
            
            // Layout parameters con aspect ratio 1:1
            val squareSize = calculateSquareCellSize()
            layoutParams = LinearLayout.LayoutParams(
                0,
                squareSize, // Altura fija para aspect ratio 1:1
                1f
            ).apply {
                setMargins(6, 6, 6, 6)
            }
            
            // Fondo con bordes redondeados
            background = createButtonBackground(false)
            
            // Crear labels posicionados para cada dirección
            createDirectionalLabelsAbsolute(this, cellConfig, cellIndex)
            
            // Configurar touch listener para gestos
            setOnTouchListener { _, event ->
                Log.d("OneHandKeyboard", "🎯 Touch event on cell $cellIndex: ${event.action}")
                
                // Procesar gesto usando el callback proporcionado
                val handled = onTouchListener(event, cellIndex)
                Log.d("OneHandKeyboard", "🎯 Gesture processed: $handled")
                
                // Consumir el evento para evitar propagación
                handled
            }
        }
    }
    
    /**
     * Crea el botón de backspace mejorado
     */
    fun createBackspaceButton(onClickListener: () -> Unit): FrameLayout {
        return FrameLayout(context).apply {
            // Layout parameters con aspect ratio 1:1
            val squareSize = calculateSquareCellSize()
            layoutParams = LinearLayout.LayoutParams(
                0,
                squareSize,
                1f
            ).apply {
                setMargins(6, 6, 6, 6)
            }
            
            // Fondo especial para backspace
            background = createButtonBackground(false, isBackspace = true)
            
            // TextView centrado con símbolo de backspace
            val textView = TextView(context).apply {
                text = "⌫"
                textSize = 20f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            
            addView(textView)
            
            // Configurar click listener
            setOnClickListener { onClickListener() }
        }
    }
    
    /**
     * Crea un botón mejorado con diseño moderno (método legacy)
     */
    fun createEnhancedButton(keyData: KeyData, onClickListener: (String) -> Unit): FrameLayout {
        return FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            ).apply {
                setMargins(4, 4, 4, 4)
            }
            
            // Fondo con bordes redondeados
            background = createButtonBackground(false)
            
            // TextView principal
            val mainTextView = TextView(context).apply {
                text = keyData.primaryChar.uppercase()
                textSize = 18f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            
            addView(mainTextView)
            
            // Configurar click listeners
            setOnClickListener {
                val outputChar = configManager.applyCase(keyData.primaryChar)
                Log.d("OneHandKeyboard", "Key clicked: $outputChar")
                onClickListener(outputChar)
                
                // Manejar modo capitalize
                configManager.onCharacterWritten()
                
                // Feedback visual
                background = createButtonBackground(true)
                postDelayed({
                    background = createButtonBackground(false)
                }, 100)
            }
        }
    }
    
    // ============================================================================
    // REGION: VISUAL HELPERS & BACKGROUNDS
    // ============================================================================
    
    /**
     * Crea el fondo del botón con bordes redondeados
     */
    fun createButtonBackground(pressed: Boolean, isBackspace: Boolean = false, isControl: Boolean = false): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 12f
            
            when {
                isBackspace -> {
                    setColor(if (pressed) Color.parseColor("#D32F2F") else Color.parseColor("#F44336"))
                }
                isControl -> {
                    setColor(if (pressed) Color.parseColor("#1976D2") else Color.parseColor("#2196F3"))
                }
                else -> {
                    setColor(if (pressed) Color.parseColor("#424242") else Color.parseColor("#616161"))
                }
            }
            
            // Borde sutil
            setStroke(2, Color.parseColor("#FFFFFF33"))
        }
    }
    
    /**
     * Crea la vista del título con información de estado
     */
    fun createTitleView(): TextView {
        return TextView(context).apply {
            text = configManager.getKeyboardTitle()
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(16, 8, 16, 8)
            setBackgroundColor(Color.parseColor("#37474F"))
        }
    }
    
    // ============================================================================
    // REGION: DIRECTIONAL LABELS & TEXT VIEWS
    // ============================================================================
    
    /**
     * Crea labels posicionados absolutamente para cada dirección en un FrameLayout
     */
    fun createDirectionalLabelsAbsolute(container: FrameLayout, cellConfig: CellConfiguration?, cellIndex: Int) {
        if (cellConfig == null) return
        
        // Manejar el centro de manera especial (combinar TAP y LONG_PRESS)
        val centerTextView = createCenterCombinedTextView(cellConfig)
        if (centerTextView != null) {
            container.addView(centerTextView)
        }
        
        // Crear TextView para direcciones no-centro
        cellConfig.actions.forEach { (gestureAction, actionConfig) ->
            if (gestureAction.direction != Direction.CENTER) {
                val label = when (actionConfig) {
                    is ActionConfig.Character -> configManager.applyCase(actionConfig.char.toString())
                    is ActionConfig.Text -> configManager.applyCase(actionConfig.text.take(2))
                    is ActionConfig.Action -> getActionSymbol(actionConfig.keyAction)
                }
                
                if (label.isNotEmpty()) {
                    val textView = createDirectionalTextView(label, gestureAction.direction, gestureAction.gestureType)
                    container.addView(textView)
                }
            }
        }
    }
    
    /**
     * Crea un TextView posicionado para una dirección específica
     */
    fun createDirectionalTextView(text: String, direction: Direction, gestureType: GestureType): TextView {
        return TextView(context).apply {
            this.text = if (gestureType == GestureType.LONG_PRESS) "($text)" else text
            textSize = 12f
            setTextColor(Color.parseColor("#FFFFFF"))
            gravity = Gravity.CENTER
            
            // Configurar posición absoluta según la dirección
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                when (direction) {
                    Direction.N -> {
                        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                        topMargin = 8
                    }
                    Direction.NE -> {
                        gravity = Gravity.TOP or Gravity.END
                        topMargin = 8
                        rightMargin = 8
                    }
                    Direction.E -> {
                        gravity = Gravity.CENTER_VERTICAL or Gravity.END
                        rightMargin = 8
                    }
                    Direction.SE -> {
                        gravity = Gravity.BOTTOM or Gravity.END
                        bottomMargin = 8
                        rightMargin = 8
                    }
                    Direction.S -> {
                        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                        bottomMargin = 8
                    }
                    Direction.SW -> {
                        gravity = Gravity.BOTTOM or Gravity.START
                        bottomMargin = 8
                        leftMargin = 8
                    }
                    Direction.W -> {
                        gravity = Gravity.CENTER_VERTICAL or Gravity.START
                        leftMargin = 8
                    }
                    Direction.NW -> {
                        gravity = Gravity.TOP or Gravity.START
                        topMargin = 8
                        leftMargin = 8
                    }
                    Direction.CENTER -> {
                        gravity = Gravity.CENTER
                    }
                }
            }
        }
    }
    
    /**
     * Maneja la combinación de TAP y LONG_PRESS para el centro
     */
    fun createCenterCombinedTextView(cellConfig: CellConfiguration): TextView? {
        val tapAction = cellConfig.getActionFor(GestureType.TAP, Direction.CENTER)
        val longPressAction = cellConfig.getActionFor(GestureType.LONG_PRESS, Direction.CENTER)
        
        val tapLabel = tapAction?.let { action ->
            when (action) {
                is ActionConfig.Character -> configManager.applyCase(action.char.toString())
                is ActionConfig.Text -> configManager.applyCase(action.text.take(2))
                is ActionConfig.Action -> getActionSymbol(action.keyAction)
            }
        } ?: ""
        
        val longPressLabel = longPressAction?.let { action ->
            when (action) {
                is ActionConfig.Character -> configManager.applyCase(action.char.toString())
                is ActionConfig.Text -> configManager.applyCase(action.text.take(2))
                is ActionConfig.Action -> getActionSymbol(action.keyAction)
            }
        } ?: ""
        
        // Combinar labels
        val combinedText = when {
            tapLabel.isNotEmpty() && longPressLabel.isNotEmpty() -> "$tapLabel ($longPressLabel)"
            tapLabel.isNotEmpty() -> tapLabel
            longPressLabel.isNotEmpty() -> "($longPressLabel)"
            else -> return null
        }
        
        return TextView(context).apply {
            text = combinedText
            textSize = 16f
            setTextColor(Color.parseColor("#FFFFFF"))
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
    }
    
    /**
     * Crea el texto con labels direccionales para una celda
     */
    fun createDirectionalLabels(cellConfig: CellConfiguration?, cellIndex: Int): String {
        if (cellConfig == null) return ""
        
        val labels = mutableListOf<String>()
        
        // Obtener label del centro (combinando TAP y LONG_PRESS)
        val centerLabel = getCenterLabel(cellConfig)
        if (centerLabel.isNotEmpty()) {
            labels.add(centerLabel)
        }
        
        // Obtener labels de otras direcciones
        val directions = listOf(Direction.N, Direction.NE, Direction.E, Direction.SE, 
                               Direction.S, Direction.SW, Direction.W, Direction.NW)
        
        directions.forEach { direction ->
            val label = getDirectionalLabel(cellConfig, direction)
            if (label.isNotEmpty()) {
                val dirSymbol = getDirectionSymbol(direction)
                labels.add("$dirSymbol$label")
            }
        }
        
        return labels.joinToString(" ")
    }
    
    /**
     * Obtiene el label para una dirección específica
     */
    fun getDirectionalLabel(cellConfig: CellConfiguration, direction: Direction): String {
        val swipeAction = cellConfig.getActionFor(GestureType.SWIPE, direction)
        return swipeAction?.let { action ->
            when (action) {
                is ActionConfig.Character -> configManager.applyCase(action.char.toString())
                is ActionConfig.Text -> configManager.applyCase(action.text.take(1))
                is ActionConfig.Action -> getActionSymbol(action.keyAction)
            }
        } ?: ""
    }
    
    /**
     * Obtiene el label para el centro (combinando TAP y LONG_PRESS)
     */
    fun getCenterLabel(cellConfig: CellConfiguration): String {
        val tapAction = cellConfig.getActionFor(GestureType.TAP, Direction.CENTER)
        val longPressAction = cellConfig.getActionFor(GestureType.LONG_PRESS, Direction.CENTER)
        
        val tapLabel = tapAction?.let { action ->
            when (action) {
                is ActionConfig.Character -> configManager.applyCase(action.char.toString())
                is ActionConfig.Text -> configManager.applyCase(action.text.take(1))
                is ActionConfig.Action -> getActionSymbol(action.keyAction)
            }
        } ?: ""
        
        val longPressLabel = longPressAction?.let { action ->
            when (action) {
                is ActionConfig.Character -> configManager.applyCase(action.char.toString())
                is ActionConfig.Text -> configManager.applyCase(action.text.take(1))
                is ActionConfig.Action -> getActionSymbol(action.keyAction)
            }
        } ?: ""
        
        return when {
            tapLabel.isNotEmpty() && longPressLabel.isNotEmpty() -> "$tapLabel($longPressLabel)"
            tapLabel.isNotEmpty() -> tapLabel
            longPressLabel.isNotEmpty() -> "($longPressLabel)"
            else -> ""
        }
    }
    
    // ============================================================================
    // REGION: CONFIGURATION HINTS & SYMBOLS
    // ============================================================================
    
    /**
     * Obtiene hints visuales de los gestos configurados para una celda
     */
    fun getConfigurationHints(cellIndex: Int): String {
        val cellConfig = configManager.keyboardConfiguration.cells.find { it.cellIndex == cellIndex } ?: return ""
        
        val hints = mutableListOf<String>()
        
        // Revisar todas las acciones configuradas (excluyendo tap center que ya se muestra)
        cellConfig.actions.forEach { (gestureAction, actionConfig) ->
            if (!(gestureAction.gestureType == GestureType.TAP && gestureAction.direction == Direction.CENTER)) {
                val directionSymbol = getDirectionSymbol(gestureAction.direction)
                val actionSymbol = when (actionConfig) {
                    is ActionConfig.Character -> actionConfig.char.toString()
                    is ActionConfig.Text -> actionConfig.text.take(1)
                    is ActionConfig.Action -> getActionSymbol(actionConfig.keyAction)
                }
                val gestureSymbol = when (gestureAction.gestureType) {
                    GestureType.SWIPE -> "→"
                    GestureType.LONG_PRESS -> "⏱"
                    else -> ""
                }
                hints.add("$directionSymbol$gestureSymbol$actionSymbol")
            }
        }
        
        return hints.joinToString(" ")
    }
    
    /**
     * Obtiene el símbolo visual para una dirección
     */
    fun getDirectionSymbol(direction: Direction): String {
        return when (direction) {
            Direction.N -> "↑"
            Direction.NE -> "↗"
            Direction.E -> "→"
            Direction.SE -> "↘"
            Direction.S -> "↓"
            Direction.SW -> "↙"
            Direction.W -> "←"
            Direction.NW -> "↖"
            Direction.CENTER -> "●"
        }
    }
    
    /**
     * Obtiene hints visuales de los gestos disponibles para una celda (método legacy)
     */
    fun getGestureHints(cellIndex: Int): String {
        val hints = mutableListOf<String>()
        
        // Ejemplo de hints básicos - esto se puede expandir
        when (cellIndex) {
            0 -> hints.addAll(listOf("↑á", "←à", "→â"))
            1 -> hints.addAll(listOf("↑é", "←è", "→ê"))
            2 -> hints.addAll(listOf("↑í", "←ì", "→î"))
            3 -> hints.addAll(listOf("↑ó", "←ò", "→ô"))
            4 -> hints.addAll(listOf("↑ú", "←ù", "→û"))
            5 -> hints.addAll(listOf("↑ñ", "←ç", "→ü"))
            6 -> hints.addAll(listOf("↑¿", "←¡", "→!"))
            7 -> hints.addAll(listOf("↑.", "←,", "→;"))
        }
        
        return hints.joinToString(" ")
    }
    
    /**
     * Obtiene el símbolo visual para una acción
     */
    fun getActionSymbol(action: KeyAction): String {
        return when (action) {
            is KeyAction.Backspace -> "⌫"
            is KeyAction.Space -> "␣"
            is KeyAction.Enter -> "↵"
            is KeyAction.ToggleCase -> "Aa"
            is KeyAction.SwitchToNumbers -> "123"
            is KeyAction.SwitchToLetters -> "ABC"
            is KeyAction.InsertText -> action.text.take(2)
            else -> "?"
        }
    }
    
    // ============================================================================
    // REGION: CONTROL BUTTONS
    // ============================================================================
    
    /**
     * Crea el botón individual para cambiar layout (para columna lateral)
     */
    fun createLayoutButton(onClickListener: () -> Unit): android.widget.Button {
        return android.widget.Button(context).apply {
            text = "Layout"
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 0, 4)
            }
            background = createButtonBackground(false, false, true)
            
            setOnClickListener { onClickListener() }
        }
    }
    
    /**
     * Crea el botón individual para cambiar case (para columna lateral)
     */
    fun createCaseButton(onClickListener: () -> Unit): android.widget.Button {
        return android.widget.Button(context).apply {
            text = "Aa"
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#2196F3"))
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 0, 4)
            }
            background = createButtonBackground(false, false, true)
            
            setOnClickListener { onClickListener() }
        }
    }
}
