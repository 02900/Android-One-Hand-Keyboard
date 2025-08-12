package com.onehandkeyboard

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.onehandkeyboard.config.ActionConfig
import com.onehandkeyboard.config.CellConfiguration
import com.onehandkeyboard.config.GestureAction
import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.GestureType
import com.onehandkeyboard.core.domain.model.KeyAction
import com.onehandkeyboard.core.domain.model.KeyData
import com.onehandkeyboard.core.gesture.CellBounds
import com.onehandkeyboard.core.gesture.GestureDetector
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main InputMethodService for OneHandKeyboard - A 3x3 gesture-based keyboard.
 *
 * This service provides a compact 9-key keyboard with 8-directional swipe gestures,
 * supporting multiple languages (Spanish, English) and layout modes (Letters, Symbols).
 * 
 * Architecture:
 * - Uses Clean Architecture principles with modular components
 * - Implements Android Views for stable IME compatibility
 * - Integrates Hilt dependency injection for configuration management
 * 
 * Key Components:
 * - KeyboardConfigManager: Configuration and state management (Hilt singleton)
 * - KeyboardUiFactory: UI creation and layout helpers (manual init)
 * - GestureIntegration: Gesture processing and bounds calculation (manual init)
 * - KeyboardActionProcessor: Action execution and text input (manual init)
 * 
 * Features:
 * - 3x3 grid with gesture hints and modern styling
 * - Multi-directional swipes (8 directions + tap + long press)
 * - Dynamic layout switching and case mode management
 * - Offline-first, privacy-focused design
 */
@AndroidEntryPoint
class OneHandKeyboardService : InputMethodService() {
    private var keyboardView: LinearLayout? = null
    
    // Gestor de configuración del teclado - inyectado por Hilt
    @Inject
    lateinit var configManager: KeyboardConfigManager
    
    // Factory para creación de UI (inicializado manualmente debido a dependencias de contexto)
    private lateinit var uiFactory: KeyboardUiFactory
    
    // Sistema de gestos avanzado - inyectado por Hilt
    @Inject
    lateinit var gestureDetector: GestureDetector
    
    // Integración de gestos
    private lateinit var gestureIntegration: GestureIntegration
    
    // Procesador de acciones del teclado
    private lateinit var actionProcessor: KeyboardActionProcessor

    // ============================================================================
    // REGION: LIFECYCLE & CORE SERVICE
    // ============================================================================

    override fun onCreateInputView(): View {
        Log.d("OneHandKeyboard", "onCreateInputView() called - GESTURE INTEGRATED VERSION")
        
        // Inicializar UI factory (requiere contexto del servicio)
        uiFactory = KeyboardUiFactory(this, configManager)
        
        // Inicializar procesador de acciones (no inyectable debido a callbacks)
        actionProcessor = KeyboardActionProcessor(
            service = this,
            configManager = configManager,
            onLayoutSwitch = { recreateKeyboard() },
            onCaseSwitch = { switchCaseMode() }
        )
        
        // Inicializar integración de gestos (no inyectable debido a callbacks)
        gestureIntegration = GestureIntegration(this, configManager, gestureDetector)
        gestureIntegration.initializeGestureSystem()
        
        try {
            keyboardView = createSimpleKeyboard()
            Log.d("OneHandKeyboard", "Simple keyboard created successfully")
            return keyboardView!!
        } catch (e: Exception) {
            Log.e("OneHandKeyboard", "Error creating keyboard", e)
            return TextView(this).apply {
                text = "Error creating keyboard: ${e.message}"
                setTextColor(Color.RED)
            }
        }
    }
    
    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        Log.d("OneHandKeyboard", "🔥 onStartInputView() called")
        
        // Configurar bounds de las celdas cuando la vista esté lista - múltiples intentos
        keyboardView?.let { view ->
            // Intento 1: Post inmediato
            view.post {
                Log.d("OneHandKeyboard", "🔥 Post runnable executing - about to calculate bounds")
                gestureIntegration.calculateCellBounds(keyboardView)
            }
            
            // Intento 2: Post con delay para asegurar que la vista esté renderizada
            view.postDelayed({
                Log.d("OneHandKeyboard", "🔥 Delayed post runnable executing - recalculating bounds")
                gestureIntegration.calculateCellBounds(keyboardView)
            }, 100) // 100ms delay
            
            // Intento 3: Post con más delay como backup
            view.postDelayed({
                Log.d("OneHandKeyboard", "🔥 Final delayed post runnable executing - last attempt")
                gestureIntegration.calculateCellBounds(keyboardView)
            }, 500) // 500ms delay
        } ?: Log.w("OneHandKeyboard", "🔥 keyboardView is null in onStartInputView!")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        gestureIntegration.cleanup()
        gestureDetector.reset()
    }

    // ============================================================================
    // REGION: UI FACTORY & LAYOUT CREATION
    // ============================================================================

    private fun createSimpleKeyboard(): LinearLayout {
        Log.d("OneHandKeyboard", "Creating enhanced 3x3 keyboard - ${configManager.getKeyboardTitle()}")
        
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 12)
            setBackgroundColor(Color.parseColor("#1E1E1E")) // Fondo oscuro moderno
            // Eliminado minimumHeight para permitir altura dinámica
            
            // Título con información de estado
            addView(uiFactory.createTitleView())
            
            // Crear fila principal que contendrá grid + controles laterales
            val mainRow = LinearLayout(this@OneHandKeyboardService).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
                setPadding(8, 8, 8, 8)
            }
            
            // Crear grid 3x3 dinámico
            val keyData = getCurrentKeyData()
            
            // Crear container para el grid (ocupa la mayor parte del espacio)
            val gridContainer = LinearLayout(this@OneHandKeyboardService).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    3f // Peso 3 para el grid
                )
                setPadding(0, 0, 8, 0) // Padding derecho para separar de controles
            }
            
            for (row in 0..2) {
                val rowLayout = LinearLayout(this@OneHandKeyboardService).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                    )
                    setPadding(0, 4, 0, 4)
                }
                
                for (col in 0..2) {
                    val keyIndex = row * 3 + col
                    val key = keyData[row][col]
                    
                    if (keyIndex == 8) {
                        // Botón backspace especial en la última posición
                        rowLayout.addView(uiFactory.createBackspaceButton {
                            currentInputConnection?.deleteSurroundingText(1, 0)
                        })
                    } else {
                        // Botón normal con gestos
                        rowLayout.addView(uiFactory.createGestureButton(key, keyIndex) { event, cellIndex ->
                            gestureIntegration.processGestureEvent(event, cellIndex) { output ->
                                output?.let { actionProcessor.processKeyboardOutput(it) }
                            }
                            true // Consumir el evento
                        })
                    }
                }
                
                gridContainer.addView(rowLayout)
            }
            
            // Agregar grid al mainRow
            mainRow.addView(gridContainer)
            
            // Crear columna de controles laterales
            val controlsColumn = LinearLayout(this@OneHandKeyboardService).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1f // Peso 1 para los controles
                )
                setPadding(8, 0, 0, 0) // Padding izquierdo para separar del grid
                gravity = Gravity.CENTER_VERTICAL // Centrar verticalmente los controles
            }
            
            // Crear controles individuales para la columna lateral
            val layoutButton = uiFactory.createLayoutButton { switchLayout() }
            val caseButton = uiFactory.createCaseButton { switchCaseMode() }
            
            controlsColumn.addView(layoutButton)
            controlsColumn.addView(caseButton)
            
            // Agregar columna de controles al mainRow
            mainRow.addView(controlsColumn)
            
            // Agregar la fila principal al layout principal
            addView(mainRow)
        }
    }
    
    /**
     * Calcula el tamaño de celda cuadrada basado en el ancho disponible para el grid
     */
    private fun calculateSquareCellSize(): Int {
        val screenWidth = resources.displayMetrics.widthPixels
        val controlsWidth = screenWidth / 4 // Los controles ocupan 1/4 del ancho (peso 1 de 4 total)
        val gridAvailableWidth = screenWidth - controlsWidth // Ancho disponible para el grid
        val totalMargins = 60 // 6px * 2 (left+right) * 3 columns + padding extra
        return (gridAvailableWidth - totalMargins) / 3
    }

    /**
     * Crea una celda con labels posicionados absolutamente usando configuración externa
     */
    private fun createGestureButton(keyData: KeyData, cellIndex: Int): FrameLayout {
        return FrameLayout(this).apply {
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
                
                // Procesar gesto usando GestureIntegration
                gestureIntegration.processGestureEvent(event, cellIndex) { output ->
                    output?.let { actionProcessor.processKeyboardOutput(it) }
                }
                
                // Consumir el evento para evitar propagación
                true
            }
        }
    }
    
    /**
     * Crea labels posicionados absolutamente para cada dirección en un FrameLayout
     */
    private fun createDirectionalLabelsAbsolute(container: FrameLayout, cellConfig: CellConfiguration?, cellIndex: Int) {
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
    private fun createDirectionalTextView(text: String, direction: Direction, gestureType: GestureType): TextView {
        return TextView(this).apply {
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
                    Direction.NW -> {
                        gravity = Gravity.TOP or Gravity.START
                        setMargins(4, 4, 0, 0)
                    }
                    Direction.N -> {
                        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                        setMargins(0, 4, 0, 0)
                    }
                    Direction.NE -> {
                        gravity = Gravity.TOP or Gravity.END
                        setMargins(0, 4, 4, 0)
                    }
                    Direction.W -> {
                        gravity = Gravity.CENTER_VERTICAL or Gravity.START
                        setMargins(4, 0, 0, 0)
                    }
                    Direction.CENTER -> {
                        gravity = Gravity.CENTER
                        setMargins(0, 0, 0, 0)
                    }
                    Direction.E -> {
                        gravity = Gravity.CENTER_VERTICAL or Gravity.END
                        setMargins(0, 0, 4, 0)
                    }
                    Direction.SW -> {
                        gravity = Gravity.BOTTOM or Gravity.START
                        setMargins(4, 0, 0, 4)
                    }
                    Direction.S -> {
                        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                        setMargins(0, 0, 0, 4)
                    }
                    Direction.SE -> {
                        gravity = Gravity.BOTTOM or Gravity.END
                        setMargins(0, 0, 4, 4)
                    }
                }
            }
        }
    }
    
    /**
     * Maneja la combinación de TAP y LONG_PRESS para el centro
     */
    private fun createCenterCombinedTextView(cellConfig: CellConfiguration): TextView? {
        val tapAction = cellConfig.getActionFor(GestureType.TAP, Direction.CENTER)
        val longPressAction = cellConfig.getActionFor(GestureType.LONG_PRESS, Direction.CENTER)
        
        val tapLabel = when (tapAction) {
            is ActionConfig.Character -> configManager.applyCase(tapAction.char.toString())
            is ActionConfig.Text -> configManager.applyCase(tapAction.text.take(2))
            is ActionConfig.Action -> getActionSymbol(tapAction.keyAction)
            null -> ""
        }
        
        val longPressLabel = when (longPressAction) {
            is ActionConfig.Character -> configManager.applyCase(longPressAction.char.toString())
            is ActionConfig.Text -> configManager.applyCase(longPressAction.text.take(2))
            is ActionConfig.Action -> getActionSymbol(longPressAction.keyAction)
            null -> ""
        }
        
        val combinedText = when {
            tapLabel.isNotEmpty() && longPressLabel.isNotEmpty() -> "$tapLabel($longPressLabel)"
            tapLabel.isNotEmpty() -> tapLabel
            longPressLabel.isNotEmpty() -> "($longPressLabel)"
            else -> return null
        }
        
        return TextView(this).apply {
            text = combinedText
            textSize = 14f // Más grande para el centro
            setTextColor(Color.parseColor("#FFFFFF"))
            gravity = Gravity.CENTER
            
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
        }
    }

    /**
     * Crea el texto con labels direccionales para una celda
     */
    private fun createDirectionalLabels(cellConfig: CellConfiguration?, cellIndex: Int): String {
        if (cellConfig == null) {
            return "Cell $cellIndex"
        }
        
        val lines = mutableListOf<String>()
        
        // Línea superior: NW, N, NE
        val topLine = buildString {
            val nw = getDirectionalLabel(cellConfig, Direction.NW)
            val n = getDirectionalLabel(cellConfig, Direction.N)
            val ne = getDirectionalLabel(cellConfig, Direction.NE)
            
            append(nw.padEnd(4))
            append(n.padEnd(4))
            append(ne)
        }.trim()
        if (topLine.isNotBlank()) lines.add(topLine)
        
        // Línea media: W, CENTER, E
        val middleLine = buildString {
            val w = getDirectionalLabel(cellConfig, Direction.W)
            val center = getCenterLabel(cellConfig)
            val e = getDirectionalLabel(cellConfig, Direction.E)
            
            append(w.padEnd(4))
            append(center.padEnd(4))
            append(e)
        }.trim()
        if (middleLine.isNotBlank()) lines.add(middleLine)
        
        // Línea inferior: SW, S, SE
        val bottomLine = buildString {
            val sw = getDirectionalLabel(cellConfig, Direction.SW)
            val s = getDirectionalLabel(cellConfig, Direction.S)
            val se = getDirectionalLabel(cellConfig, Direction.SE)
            
            append(sw.padEnd(4))
            append(s.padEnd(4))
            append(se)
        }.trim()
        if (bottomLine.isNotBlank()) lines.add(bottomLine)
        
        return lines.joinToString("\n").ifEmpty { "Cell $cellIndex" }
    }
    
    /**
     * Obtiene el label para una dirección específica
     */
    private fun getDirectionalLabel(cellConfig: CellConfiguration, direction: Direction): String {
        val action = cellConfig.getActionFor(GestureType.SWIPE, direction)
        return when (action) {
            is ActionConfig.Character -> configManager.applyCase(action.char.toString())
            is ActionConfig.Text -> configManager.applyCase(action.text.take(2)) // Máximo 2 caracteres
            is ActionConfig.Action -> getActionSymbol(action.keyAction)
            null -> ""
        }
    }
    
    /**
     * Obtiene el label para el centro (combinando TAP y LONG_PRESS)
     */
    private fun getCenterLabel(cellConfig: CellConfiguration): String {
        val tapAction = cellConfig.getActionFor(GestureType.TAP, Direction.CENTER)
        val longPressAction = cellConfig.getActionFor(GestureType.LONG_PRESS, Direction.CENTER)
        
        val tapLabel = when (tapAction) {
            is ActionConfig.Character -> configManager.applyCase(tapAction.char.toString())
            is ActionConfig.Text -> configManager.applyCase(tapAction.text.take(2))
            is ActionConfig.Action -> getActionSymbol(tapAction.keyAction)
            null -> ""
        }
        
        val longPressLabel = when (longPressAction) {
            is ActionConfig.Character -> configManager.applyCase(longPressAction.char.toString())
            is ActionConfig.Text -> configManager.applyCase(longPressAction.text.take(2))
            is ActionConfig.Action -> getActionSymbol(longPressAction.keyAction)
            null -> ""
        }
        
        return when {
            tapLabel.isNotEmpty() && longPressLabel.isNotEmpty() -> "$tapLabel($longPressLabel)"
            tapLabel.isNotEmpty() -> tapLabel
            longPressLabel.isNotEmpty() -> "($longPressLabel)"
            else -> ""
        }
    }

    /**
     * Obtiene hints visuales de los gestos configurados para una celda
     */
    private fun getConfigurationHints(cellIndex: Int): String {
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
        
        return if (hints.isNotEmpty()) "\n${hints.take(3).joinToString(" ")}" else ""
    }
    
    /**
     * Obtiene el símbolo visual para una dirección
     */
    private fun getDirectionSymbol(direction: Direction): String {
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
    private fun getActionSymbol(action: KeyAction): String {
        return when (action) {
            is KeyAction.Backspace -> "⌫"
            is KeyAction.Space -> "␣"
            is KeyAction.Enter -> "↵"
            is KeyAction.ToggleCase -> "Aa"
            else -> "⚡"
        }
    }
    
    // createEnhancedButton function removed - replaced by KeyboardUiFactory
    
    // applyCase function moved to KeyboardConfigManager
    
    /**
     * Crea el botón de backspace mejorado
     */
    private fun createBackspaceButton(): Button {
        return Button(this).apply {
            text = "⌫"
            textSize = 20f
            setTextColor(Color.parseColor("#FF6B6B"))
            gravity = Gravity.CENTER
            
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            ).apply {
                setMargins(6, 6, 6, 6)
            }
            
            background = createButtonBackground(false, true)
            
            setOnClickListener {
                Log.d("OneHandKeyboard", "Backspace clicked")
                currentInputConnection?.deleteSurroundingText(1, 0)
                
                // Feedback visual
                background = createButtonBackground(true, true)
                postDelayed({
                    background = createButtonBackground(false, true)
                }, 100)
            }
        }
    }
    
    /**
     * Crea el fondo del botón con bordes redondeados
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
    
    /**
     * Crea la vista del título con información de estado
     */
    private fun createTitleView(): TextView {
        return TextView(this).apply {
            text = configManager.getKeyboardTitle()
            textSize = 14f
            setTextColor(Color.parseColor("#E0E0E0"))
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 12)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
    }
    
    // ============================================================================
    // REGION: CONFIGURATION & STATE MANAGEMENT
    // ============================================================================
    
    /**
     * Cambia entre modos LETTERS y SYMBOLS para el idioma actual
     */
    private fun switchLayout() {
        if (configManager.switchLayout()) {
            recreateKeyboard()
        }
    }
    
    /**
     * Cambia entre modos de mayúsculas
     */
    private fun switchCaseMode() {
        if (configManager.switchCaseMode()) {
            recreateKeyboard()
        }
    }
    
    /**
     * Recrea el teclado con la configuración actual
     */
    private fun recreateKeyboard() {
        try {
            // Asegurar que UI factory esté inicializada
            if (!::uiFactory.isInitialized) {
                uiFactory = KeyboardUiFactory(this, configManager)
            }
            
            keyboardView = createSimpleKeyboard()
            setInputView(keyboardView)
            Log.d("OneHandKeyboard", "Keyboard recreated - ${configManager.getKeyboardTitle()}")
        } catch (e: Exception) {
            Log.e("OneHandKeyboard", "Error recreating keyboard", e)
        }
    }

    /**
     * Crea el botón individual para cambiar layout (para columna lateral)
     */
    private fun createLayoutButton(): Button {
        return Button(this).apply {
            text = "Layout"
            textSize = 12f
            setTextColor(Color.parseColor("#4CAF50"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 0, 4)
            }
            background = createButtonBackground(false, false, true)
            
            setOnClickListener {
                switchLayout()
            }
        }
    }
    
    /**
     * Crea el botón individual para cambiar case (para columna lateral)
     */
    private fun createCaseButton(): Button {
        return Button(this).apply {
            text = "Aa"
            textSize = 12f
            setTextColor(Color.parseColor("#2196F3"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 0, 4)
            }
            background = createButtonBackground(false, false, true)
            
            setOnClickListener {
                switchCaseMode()
            }
        }
    }

    /**
     * Crea los botones de control para cambiar layout y case
     */
    private fun createControlButtons(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 0, 0, 8)
            
            // Botón para cambiar layout
            addView(Button(this@OneHandKeyboardService).apply {
                text = "Layout"
                textSize = 12f
                setTextColor(Color.parseColor("#4CAF50"))
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    setMargins(4, 0, 4, 0)
                }
                background = createButtonBackground(false, false, true)
                
                setOnClickListener {
                    switchLayout()
                }
            })
            
            // Botón para cambiar case
            addView(Button(this@OneHandKeyboardService).apply {
                text = "Aa"
                textSize = 12f
                setTextColor(Color.parseColor("#2196F3"))
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    setMargins(4, 0, 4, 0)
                }
                background = createButtonBackground(false, false, true)
                
                setOnClickListener {
                    switchCaseMode()
                }
            })
        }
    }
    
    /**
     * Obtiene los datos de las teclas según el idioma y modo actual
     */
    private fun getCurrentKeyData(): Array<Array<KeyData>> {
        return configManager.getCurrentKeyData()
    }


    // ============================================================================
    // REGION: UTILITY FUNCTIONS
    // ============================================================================
    
    /**
     * Muestra opciones de acentos (implementación simple por ahora)
     */
    private fun showAccentOptions(accentChars: String) {
        // Por ahora, simplemente envía el primer carácter con acento
        if (accentChars.isNotEmpty()) {
            val firstAccent = accentChars.first().toString()
            Log.d("OneHandKeyboard", "Sending accent: $firstAccent")
            currentInputConnection?.commitText(firstAccent, 1)
        }
    }
    
    // createUltraSimpleView function removed - no longer used

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        Log.d("OneHandKeyboard", "onFinishInputView() called - keyboard hiding")
        // Clean up any ongoing operations
    }
    
    override fun onComputeInsets(outInsets: InputMethodService.Insets) {
        super.onComputeInsets(outInsets)
        Log.d("OneHandKeyboard", "onComputeInsets() called")
        
        // Forzar que el teclado ocupe espacio visible
        outInsets.contentTopInsets = 0
        outInsets.visibleTopInsets = 0
        outInsets.touchableInsets = InputMethodService.Insets.TOUCHABLE_INSETS_VISIBLE
        
        Log.d("OneHandKeyboard", "Insets configured - keyboard should be visible")
    }
    
    override fun onEvaluateInputViewShown(): Boolean {
        Log.d("OneHandKeyboard", "onEvaluateInputViewShown() called")
        // Forzar que el teclado siempre se muestre
        return true
    }
}


