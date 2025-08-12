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
import com.onehandkeyboard.core.domain.model.*
import com.onehandkeyboard.core.gesture.GestureDetector
import com.onehandkeyboard.core.gesture.CellBounds
import com.onehandkeyboard.config.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Data class for keyboard key information
 */
data class KeyData(
    val label: String,        // Texto mostrado en el botón (ABC, DEF, etc.)
    val primaryChar: String,  // Carácter principal (a, d, g, etc.)
    val accentChars: String   // Caracteres con acentos (áàâä, etc.)
)

/**
 * Enum for keyboard layouts
 */
enum class KeyboardLayout {
    SPANISH_LETTERS,
    ENGLISH_LETTERS, 
    NUMBERS_SYMBOLS
}

/**
 * Enum for case modes
 */
enum class CaseMode {
    LOWERCASE,
    UPPERCASE,
    CAPITALIZE
}

/**
 * Main InputMethodService for OneHandKeyboard
 *
 * Enhanced version with improved UI using Android Views
 */
@AndroidEntryPoint
class OneHandKeyboardService : InputMethodService() {
    private var keyboardView: LinearLayout? = null
    
    // Estado del teclado
    private var currentLayout = KeyboardLayout.SPANISH_LETTERS
    private var currentCaseMode = CaseMode.LOWERCASE
    private var isCapitalizeNext = false // Para modo capitalize
    
    // Sistema de gestos avanzado - inyectado por Hilt
    @Inject
    lateinit var gestureDetector: GestureDetector
    private var currentKeyboardLayout: com.onehandkeyboard.core.domain.model.KeyboardLayout? = null
    private var cellBounds: Array<CellBounds> = emptyArray()
    private var gestureJob: Job? = null
    private val gestureScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Sistema de configuración externa
    private var keyboardConfiguration: KeyboardConfiguration = KeyboardConfiguration.createSpanishDefault()
    
    // Estado para procesamiento directo de gestos
    private var gestureStartTime = 0L
    private var gestureStartX = 0f
    private var gestureStartY = 0f
    private var isLongPressDetected = false
    private var gestureAlreadyEmitted = false // Prevenir repetición de gestos
    private val longPressThreshold = 500L // 500ms
    private val swipeThreshold = 50f // pixels

    override fun onCreateInputView(): View {
        Log.d("OneHandKeyboard", "onCreateInputView() called - GESTURE INTEGRATED VERSION")
        
        // Inicializar sistema de gestos
        initializeGestureSystem()
        
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
                calculateCellBounds()
            }
            
            // Intento 2: Post con delay para asegurar que la vista esté renderizada
            view.postDelayed({
                Log.d("OneHandKeyboard", "🔥 Delayed post runnable executing - recalculating bounds")
                calculateCellBounds()
            }, 100) // 100ms delay
            
            // Intento 3: Post con más delay como backup
            view.postDelayed({
                Log.d("OneHandKeyboard", "🔥 Final delayed post runnable executing - last attempt")
                calculateCellBounds()
            }, 500) // 500ms delay
        } ?: Log.w("OneHandKeyboard", "🔥 keyboardView is null in onStartInputView!")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        gestureScope.cancel()
        gestureDetector.reset()
    }

    private fun createSimpleKeyboard(): LinearLayout {
        Log.d("OneHandKeyboard", "Creating enhanced 3x3 keyboard - Layout: $currentLayout, Case: $currentCaseMode")
        
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 12)
            setBackgroundColor(Color.parseColor("#1E1E1E")) // Fondo oscuro moderno
            // Eliminado minimumHeight para permitir altura dinámica
            
            // Título con información de estado
            addView(createTitleView())
            
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
                        rowLayout.addView(createBackspaceButton())
                    } else {
                        // Botón normal con gestos
                        rowLayout.addView(createGestureButton(key, keyIndex))
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
            val layoutButton = createLayoutButton()
            val caseButton = createCaseButton()
            
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
            val cellConfig = keyboardConfiguration.cells.find { it.cellIndex == cellIndex }
            
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
                
                // Procesar gesto directamente
                val handled = processGestureDirectly(event, cellIndex)
                Log.d("OneHandKeyboard", "🎯 Gesture processed: $handled")
                
                // Consumir el evento para evitar propagación
                handled
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
                    is ActionConfig.Character -> applyCase(actionConfig.char.toString())
                    is ActionConfig.Text -> applyCase(actionConfig.text.take(2))
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
            is ActionConfig.Character -> applyCase(tapAction.char.toString())
            is ActionConfig.Text -> applyCase(tapAction.text.take(2))
            is ActionConfig.Action -> getActionSymbol(tapAction.keyAction)
            null -> ""
        }
        
        val longPressLabel = when (longPressAction) {
            is ActionConfig.Character -> applyCase(longPressAction.char.toString())
            is ActionConfig.Text -> applyCase(longPressAction.text.take(2))
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
            is ActionConfig.Character -> applyCase(action.char.toString())
            is ActionConfig.Text -> applyCase(action.text.take(2)) // Máximo 2 caracteres
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
            is ActionConfig.Character -> applyCase(tapAction.char.toString())
            is ActionConfig.Text -> applyCase(tapAction.text.take(2))
            is ActionConfig.Action -> getActionSymbol(tapAction.keyAction)
            null -> ""
        }
        
        val longPressLabel = when (longPressAction) {
            is ActionConfig.Character -> applyCase(longPressAction.char.toString())
            is ActionConfig.Text -> applyCase(longPressAction.text.take(2))
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
        val cellConfig = keyboardConfiguration.cells.find { it.cellIndex == cellIndex } ?: return ""
        
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
    
    /**
     * Obtiene hints visuales de los gestos disponibles para una celda (método legacy)
     */
    private fun getGestureHints(cellIndex: Int): String {
        val layout = currentKeyboardLayout ?: return ""
        val cell = layout.getCell(cellIndex) ?: return ""
        
        val hints = mutableListOf<String>()
        
        // Revisar direcciones con contenido
        Direction.values().forEach { direction ->
            if (direction != Direction.CENTER) {
                val char = cell.getCharacterForDirection(direction)
                val action = cell.getActionForDirection(direction)
                
                when {
                    char != null -> hints.add("${direction.name.first()}:$char")
                    action != null -> hints.add("${direction.name.first()}:${getActionSymbol(action)}")
                }
            }
        }
        
        return if (hints.isNotEmpty()) "\n${hints.take(2).joinToString(" ")}" else ""
    }
    
    /**
     * Obtiene el símbolo visual para una acción
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
     * Crea un botón mejorado con diseño moderno (método legacy)
     */
    private fun createEnhancedButton(keyData: KeyData): Button {
        return Button(this).apply {
            // Aplicar modo de mayúsculas al carácter
            val displayChar = applyCase(keyData.primaryChar)
            
            // Configuración del texto
            text = "${keyData.label}\n$displayChar"
            textSize = 14f
            setTextColor(Color.parseColor("#FFFFFF"))
            gravity = Gravity.CENTER
            
            // Layout parameters
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            ).apply {
                setMargins(6, 6, 6, 6)
            }
            
            // Fondo con bordes redondeados
            background = createButtonBackground(false)
            
            // Configurar click listeners
            setOnClickListener {
                val outputChar = applyCase(keyData.primaryChar)
                Log.d("OneHandKeyboard", "Key clicked: $outputChar")
                currentInputConnection?.commitText(outputChar, 1)
                
                // Manejar modo capitalize
                if (currentCaseMode == CaseMode.CAPITALIZE && isCapitalizeNext) {
                    isCapitalizeNext = false
                }
                
                // Feedback visual
                background = createButtonBackground(true)
                postDelayed({
                    background = createButtonBackground(false)
                }, 100)
            }
            
            // Long press para caracteres con acentos
            if (keyData.accentChars.isNotEmpty()) {
                setOnLongClickListener {
                    Log.d("OneHandKeyboard", "Long press: showing accents ${keyData.accentChars}")
                    showAccentOptions(keyData.accentChars)
                    true
                }
            }
        }
    }
    
    /**
     * Aplica el modo de mayúsculas al carácter
     */
    private fun applyCase(char: String): String {
        if (char == " ") return char // No cambiar espacios
        
        return when (currentCaseMode) {
            CaseMode.LOWERCASE -> char.lowercase()
            CaseMode.UPPERCASE -> char.uppercase()
            CaseMode.CAPITALIZE -> {
                if (isCapitalizeNext) {
                    char.uppercase()
                } else {
                    char.lowercase()
                }
            }
        }
    }
    
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
            text = "OneHand Keyboard - ${currentLayout.name.replace("_", " ")} (${currentCaseMode.name})"
            textSize = 14f
            setTextColor(Color.parseColor("#E0E0E0"))
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 12)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
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
     * Obtiene los datos de las teclas según el layout actual
     */
    private fun getCurrentKeyData(): Array<Array<KeyData>> {
        return when (currentLayout) {
            KeyboardLayout.SPANISH_LETTERS -> getSpanishLettersData()
            KeyboardLayout.ENGLISH_LETTERS -> getEnglishLettersData()
            KeyboardLayout.NUMBERS_SYMBOLS -> getNumbersSymbolsData()
        }
    }
    
    /**
     * Datos para layout español
     */
    private fun getSpanishLettersData(): Array<Array<KeyData>> {
        return arrayOf(
            arrayOf(
                KeyData("ABC", "a", "áàâä"),
                KeyData("DEF", "d", "éèêë"), 
                KeyData("GHI", "g", "íìîï")
            ),
            arrayOf(
                KeyData("JKL", "j", ""),
                KeyData("MNÑ", " ", "ñ"), // Espacio principal, ñ secundario
                KeyData("OPQ", "o", "óòôö")
            ),
            arrayOf(
                KeyData("RST", "r", ""),
                KeyData("UVW", "u", "úùûü"),
                KeyData("XYZ", "x", "")
            )
        )
    }
    
    /**
     * Datos para layout inglés
     */
    private fun getEnglishLettersData(): Array<Array<KeyData>> {
        return arrayOf(
            arrayOf(
                KeyData("ABC", "a", ""),
                KeyData("DEF", "d", ""), 
                KeyData("GHI", "g", "")
            ),
            arrayOf(
                KeyData("JKL", "j", ""),
                KeyData("MNO", " ", ""), // Espacio en el centro
                KeyData("PQR", "p", "")
            ),
            arrayOf(
                KeyData("STU", "s", ""),
                KeyData("VWX", "v", ""),
                KeyData("YZ", "y", "")
            )
        )
    }
    
    /**
     * Datos para layout números y símbolos
     */
    private fun getNumbersSymbolsData(): Array<Array<KeyData>> {
        return arrayOf(
            arrayOf(
                KeyData("1!?", "1", "!?"),
                KeyData("2@", "2", "@"), 
                KeyData("3#", "3", "#")
            ),
            arrayOf(
                KeyData("4$", "4", "$"),
                KeyData("5%", " ", ""), // Espacio en el centro
                KeyData("6^", "6", "^")
            ),
            arrayOf(
                KeyData("7&", "7", "&"),
                KeyData("8*", "8", "*"),
                KeyData("90", "9", "0")
            )
        )
    }
    
    /**
     * Cambia al siguiente layout
     */
    private fun switchLayout() {
        currentLayout = when (currentLayout) {
            KeyboardLayout.SPANISH_LETTERS -> KeyboardLayout.ENGLISH_LETTERS
            KeyboardLayout.ENGLISH_LETTERS -> KeyboardLayout.NUMBERS_SYMBOLS
            KeyboardLayout.NUMBERS_SYMBOLS -> KeyboardLayout.SPANISH_LETTERS
        }
        Log.d("OneHandKeyboard", "Switched to layout: $currentLayout")
        recreateKeyboard()
    }
    
    /**
     * Cambia al siguiente modo de mayúsculas
     */
    private fun switchCaseMode() {
        currentCaseMode = when (currentCaseMode) {
            CaseMode.LOWERCASE -> CaseMode.UPPERCASE
            CaseMode.UPPERCASE -> CaseMode.CAPITALIZE
            CaseMode.CAPITALIZE -> CaseMode.LOWERCASE
        }
        isCapitalizeNext = (currentCaseMode == CaseMode.CAPITALIZE)
        Log.d("OneHandKeyboard", "Switched to case mode: $currentCaseMode")
        recreateKeyboard()
    }
    
    /**
     * Recrea el teclado con el nuevo estado
     */
    private fun recreateKeyboard() {
        try {
            Log.d("OneHandKeyboard", "🔥 Starting recreateKeyboard()")
            
            // Actualizar el layout del dominio
            currentKeyboardLayout = createDomainKeyboardLayout()
            Log.d("OneHandKeyboard", "🔥 Domain layout updated")
            
            // Recrear la vista
            keyboardView = createSimpleKeyboard()
            setInputView(keyboardView)
            Log.d("OneHandKeyboard", "🔥 New keyboard view set")
            
            // Recalcular bounds después de que la vista esté lista
            keyboardView?.post {
                Log.d("OneHandKeyboard", "🔥 Post runnable in recreateKeyboard - calculating bounds")
                calculateCellBounds()
            } ?: Log.w("OneHandKeyboard", "🔥 keyboardView is null in recreateKeyboard!")
            
            Log.d("OneHandKeyboard", "🔥 Keyboard recreated successfully with gesture system")
        } catch (e: Exception) {
            Log.e("OneHandKeyboard", "🔥 Error recreating keyboard", e)
        }
    }
    
    /**
     * Inicializa el sistema de gestos avanzado
     */
    private fun initializeGestureSystem() {
        Log.d("OneHandKeyboard", "🔥 Initializing advanced gesture system")
        
        // Configurar el layout del dominio según el layout actual
        currentKeyboardLayout = createDomainKeyboardLayout()
        Log.d("OneHandKeyboard", "🔥 Domain layout created: ${currentKeyboardLayout?.name}")
        
        // Inicializar el detector de gestos con configuración por defecto
        val config = GestureConfig()
        gestureDetector.updateConfig(config)
        Log.d("OneHandKeyboard", "🔥 Gesture detector configured with: $config")
        
        // Configurar el listener de gestos
        gestureJob?.cancel()
        gestureJob = gestureScope.launch {
            Log.d("OneHandKeyboard", "🔥 Starting gesture listener coroutine")
            gestureDetector.gestures.collect { gesture ->
                Log.d("OneHandKeyboard", "🔥 Gesture received in coroutine: $gesture")
                handleGesture(gesture)
            }
        }
        Log.d("OneHandKeyboard", "🔥 Gesture system initialization complete")
    }
    
    /**
     * Calcula los bounds de las celdas para el detector de gestos
     */
    private fun calculateCellBounds() {
        Log.d("OneHandKeyboard", "🔥 Starting calculateCellBounds()")
        val keyboardView = this.keyboardView
        
        if (keyboardView == null) {
            Log.w("OneHandKeyboard", "🔥 keyboardView is null!")
            return
        }
        
        Log.d("OneHandKeyboard", "🔥 keyboardView children: ${keyboardView.childCount}")
        
        // Encontrar el grid layout (tercer hijo: título, controles, gridContainer)
        if (keyboardView.childCount < 3) {
            Log.w("OneHandKeyboard", "🔥 Not enough children in keyboardView: ${keyboardView.childCount}")
            return
        }
        
        val gridContainer = keyboardView.getChildAt(2) as? LinearLayout
        if (gridContainer == null) {
            Log.w("OneHandKeyboard", "🔥 Grid container is null or not LinearLayout")
            return
        }
        
        Log.d("OneHandKeyboard", "🔥 Grid container children: ${gridContainer.childCount}")
        val bounds = mutableListOf<CellBounds>()
        
        // Calcular bounds para cada celda del grid 3x3
        for (row in 0..2) {
            val rowLayout = gridContainer.getChildAt(row) as? LinearLayout
            if (rowLayout == null) {
                Log.w("OneHandKeyboard", "🔥 Row $row is null or not LinearLayout")
                continue
            }
            
            Log.d("OneHandKeyboard", "🔥 Row $row children: ${rowLayout.childCount}")
            
            for (col in 0..2) {
                val button = rowLayout.getChildAt(col) as? Button
                if (button == null) {
                    Log.w("OneHandKeyboard", "🔥 Button at [$row,$col] is null or not Button")
                    continue
                }
                
                val cellIndex = row * 3 + col
                
                // Obtener posición y tamaño del botón
                val location = IntArray(2)
                button.getLocationOnScreen(location)
                
                val cellBound = CellBounds(
                    cellIndex = cellIndex,
                    left = location[0].toFloat(),
                    top = location[1].toFloat(),
                    right = (location[0] + button.width).toFloat(),
                    bottom = (location[1] + button.height).toFloat()
                )
                
                Log.d("OneHandKeyboard", "🔥 Cell $cellIndex bounds: $cellBound")
                bounds.add(cellBound)
            }
        }
        
        cellBounds = bounds.toTypedArray()
        Log.d("OneHandKeyboard", "🔥 Calculated ${cellBounds.size} cell bounds successfully!")
    }
    
    /**
     * Crea el layout del dominio según el layout actual de la UI
     */
    private fun createDomainKeyboardLayout(): com.onehandkeyboard.core.domain.model.KeyboardLayout {
        val cells = when (currentLayout) {
            KeyboardLayout.SPANISH_LETTERS -> createSpanishCells()
            KeyboardLayout.ENGLISH_LETTERS -> createEnglishCells()
            KeyboardLayout.NUMBERS_SYMBOLS -> createNumbersCells()
        }
        
        return com.onehandkeyboard.core.domain.model.KeyboardLayout(
            id = "current_layout",
            name = currentLayout.name,
            description = "Current active layout",
            cells = cells,
            type = when (currentLayout) {
                KeyboardLayout.SPANISH_LETTERS, KeyboardLayout.ENGLISH_LETTERS -> LayoutType.LETTERS
                KeyboardLayout.NUMBERS_SYMBOLS -> LayoutType.NUMBERS
            },
            locale = if (currentLayout == KeyboardLayout.SPANISH_LETTERS) "es-MX" else "en-US"
        )
    }
    
    /**
     * Crea las celdas para el layout español con gestos
     */
    private fun createSpanishCells(): List<Cell> {
        return listOf(
            // Fila 0
            Cell.create(0, "ABC", listOf('a'), mapOf(Direction.N to 'á', Direction.W to 'à', Direction.E to 'â')),
            Cell.create(1, "DEF", listOf('d'), mapOf(Direction.N to 'é', Direction.W to 'è', Direction.E to 'ê')),
            Cell.create(2, "GHI", listOf('g'), mapOf(Direction.N to 'í', Direction.W to 'ì', Direction.E to 'î')),
            
            // Fila 1
            Cell.create(3, "JKL", listOf('j')),
            Cell.create(4, "MNÑ", listOf(' '), mapOf(Direction.N to 'ñ'), mapOf(Direction.S to KeyAction.Enter)),
            Cell.create(5, "OPQ", listOf('o'), mapOf(Direction.N to 'ó', Direction.W to 'ò', Direction.E to 'ô')),
            
            // Fila 2
            Cell.create(6, "RST", listOf('r')),
            Cell.create(7, "UVW", listOf('u'), mapOf(Direction.N to 'ú', Direction.W to 'ù', Direction.E to 'û')),
            Cell.create(8, "XYZ", listOf('x'), actions = mapOf(Direction.W to KeyAction.Backspace))
        )
    }
    
    /**
     * Crea las celdas para el layout inglés con gestos
     */
    private fun createEnglishCells(): List<Cell> {
        return listOf(
            // Fila 0
            Cell.create(0, "ABC", listOf('a')),
            Cell.create(1, "DEF", listOf('d')),
            Cell.create(2, "GHI", listOf('g')),
            
            // Fila 1
            Cell.create(3, "JKL", listOf('j')),
            Cell.create(4, "MNO", listOf(' '), actions = mapOf(Direction.S to KeyAction.Enter)),
            Cell.create(5, "PQR", listOf('p')),
            
            // Fila 2
            Cell.create(6, "STU", listOf('s')),
            Cell.create(7, "VWX", listOf('v')),
            Cell.create(8, "YZ", listOf('y'), actions = mapOf(Direction.W to KeyAction.Backspace))
        )
    }
    
    /**
     * Crea las celdas para el layout de números con gestos
     */
    private fun createNumbersCells(): List<Cell> {
        return listOf(
            // Fila 0
            Cell.create(0, "1!?", listOf('1'), mapOf(Direction.N to '!', Direction.S to '?')),
            Cell.create(1, "2@", listOf('2'), mapOf(Direction.N to '@')),
            Cell.create(2, "3#", listOf('3'), mapOf(Direction.N to '#')),
            
            // Fila 1
            Cell.create(3, "4$", listOf('4'), mapOf(Direction.N to '$')),
            Cell.create(4, "5%", listOf(' '), actions = mapOf(Direction.S to KeyAction.Enter)),
            Cell.create(5, "6^", listOf('6'), mapOf(Direction.N to '^')),
            
            // Fila 2
            Cell.create(6, "7&", listOf('7'), mapOf(Direction.N to '&')),
            Cell.create(7, "8*", listOf('8'), mapOf(Direction.N to '*')),
            Cell.create(8, "90", listOf('9'), mapOf(Direction.E to '0'), mapOf(Direction.W to KeyAction.Backspace))
        )
    }
    
    /**
     * Procesa gestos directamente sin detector externo
     */
    private fun processGestureDirectly(event: MotionEvent, cellIndex: Int): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                gestureStartTime = System.currentTimeMillis()
                gestureStartX = event.x
                gestureStartY = event.y
                isLongPressDetected = false
                gestureAlreadyEmitted = false // Reset para nuevo gesto
                Log.d("OneHandKeyboard", "🎯 DIRECT: Gesture started on cell $cellIndex")
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                // Solo procesar si no hemos emitido un gesto ya
                if (!gestureAlreadyEmitted) {
                    val deltaX = event.x - gestureStartX
                    val deltaY = event.y - gestureStartY
                    val distance = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)
                    
                    // Detectar swipe si se mueve más del threshold
                    if (distance > swipeThreshold) {
                        val direction = getSwipeDirection(deltaX, deltaY)
                        Log.d("OneHandKeyboard", "🎯 DIRECT: Swipe detected - direction: $direction, distance: $distance")
                        handleDirectGesture(cellIndex, GestureType.SWIPE, direction)
                        gestureAlreadyEmitted = true // Marcar como emitido
                        return true
                    }
                    
                    // Detectar long press durante el movimiento
                    val duration = System.currentTimeMillis() - gestureStartTime
                    if (duration >= longPressThreshold && !isLongPressDetected) {
                        Log.d("OneHandKeyboard", "🎯 DIRECT: Long press detected during move")
                        handleDirectGesture(cellIndex, GestureType.LONG_PRESS, Direction.CENTER)
                        isLongPressDetected = true
                        gestureAlreadyEmitted = true // Marcar como emitido
                        return true
                    }
                }
                return true
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val duration = System.currentTimeMillis() - gestureStartTime
                val deltaX = event.x - gestureStartX
                val deltaY = event.y - gestureStartY
                val distance = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)
                
                Log.d("OneHandKeyboard", "🎯 DIRECT: Gesture ended - duration: ${duration}ms, distance: $distance, alreadyEmitted: $gestureAlreadyEmitted")
                
                // Solo emitir si no hemos emitido nada aún
                if (!gestureAlreadyEmitted) {
                    when {
                        distance > swipeThreshold -> {
                            // Swipe tardío (no detectado en MOVE)
                            val direction = getSwipeDirection(deltaX, deltaY)
                            Log.d("OneHandKeyboard", "🎯 DIRECT: Late swipe detected - direction: $direction")
                            handleDirectGesture(cellIndex, GestureType.SWIPE, direction)
                        }
                        duration >= longPressThreshold -> {
                            Log.d("OneHandKeyboard", "🎯 DIRECT: Long press detected on release")
                            handleDirectGesture(cellIndex, GestureType.LONG_PRESS, Direction.CENTER)
                        }
                        else -> {
                            Log.d("OneHandKeyboard", "🎯 DIRECT: Tap detected")
                            handleDirectGesture(cellIndex, GestureType.TAP, Direction.CENTER)
                        }
                    }
                }
                
                // Reset estado para próximo gesto
                gestureAlreadyEmitted = false
                isLongPressDetected = false
                return true
            }
        }
        return false
    }
    
    /**
     * Determina la dirección del swipe basado en deltaX y deltaY
     */
    private fun getSwipeDirection(deltaX: Float, deltaY: Float): Direction {
        val angle = kotlin.math.atan2(-deltaY, deltaX) * 180 / kotlin.math.PI
        val normalizedAngle = if (angle < 0) angle + 360 else angle
        
        return when {
            normalizedAngle >= 337.5 || normalizedAngle < 22.5 -> Direction.E
            normalizedAngle >= 22.5 && normalizedAngle < 67.5 -> Direction.NE
            normalizedAngle >= 67.5 && normalizedAngle < 112.5 -> Direction.N
            normalizedAngle >= 112.5 && normalizedAngle < 157.5 -> Direction.NW
            normalizedAngle >= 157.5 && normalizedAngle < 202.5 -> Direction.W
            normalizedAngle >= 202.5 && normalizedAngle < 247.5 -> Direction.SW
            normalizedAngle >= 247.5 && normalizedAngle < 292.5 -> Direction.S
            else -> Direction.SE
        }
    }
    
    /**
     * Maneja un gesto detectado directamente usando configuración externa
     */
    private fun handleDirectGesture(cellIndex: Int, gestureType: GestureType, direction: Direction) {
        Log.d("OneHandKeyboard", "🎯 DIRECT GESTURE: $gestureType on cell $cellIndex, direction $direction")
        
        // Validar que el gesto sea válido antes de procesarlo
        if (!GestureAction.isValidGestureAction(gestureType, direction)) {
            Log.e("OneHandKeyboard", "🎯 INVALID GESTURE: $gestureType cannot be performed in direction $direction")
            return
        }
        
        // Buscar la configuración de la celda
        val cellConfig = keyboardConfiguration.cells.find { it.cellIndex == cellIndex }
        if (cellConfig == null) {
            Log.w("OneHandKeyboard", "🎯 No configuration found for cell $cellIndex")
            return
        }
        
        // Buscar la acción configurada para este gesto
        val actionConfig = cellConfig.getActionFor(gestureType, direction)
        if (actionConfig == null) {
            Log.d("OneHandKeyboard", "🎯 No action configured for $gestureType + $direction on cell $cellIndex")
            return
        }
        
        Log.d("OneHandKeyboard", "🎯 Found action config: $actionConfig")
        
        // Ejecutar la acción configurada
        when (actionConfig) {
            is ActionConfig.Character -> {
                val char = applyCase(actionConfig.char.toString())
                Log.d("OneHandKeyboard", "🎯 SENDING CHARACTER: '$char' (original: '${actionConfig.char}')")
                
                val inputConnection = currentInputConnection
                if (inputConnection == null) {
                    Log.e("OneHandKeyboard", "🎯 ERROR: currentInputConnection is NULL!")
                    return
                }
                
                val result = inputConnection.commitText(char, 1)
                Log.d("OneHandKeyboard", "🎯 commitText result: $result")
                
                // Manejar modo capitalize
                if (currentCaseMode == CaseMode.CAPITALIZE && isCapitalizeNext) {
                    isCapitalizeNext = false
                }
            }
            is ActionConfig.Action -> {
                Log.d("OneHandKeyboard", "🎯 EXECUTING ACTION: ${actionConfig.keyAction}")
                executeKeyAction(actionConfig.keyAction)
            }
            is ActionConfig.Text -> {
                val text = applyCase(actionConfig.text)
                Log.d("OneHandKeyboard", "🎯 SENDING TEXT: '$text'")
                
                val inputConnection = currentInputConnection
                if (inputConnection == null) {
                    Log.e("OneHandKeyboard", "🎯 ERROR: currentInputConnection is NULL!")
                    return
                }
                
                val result = inputConnection.commitText(text, 1)
                Log.d("OneHandKeyboard", "🎯 commitText result: $result")
            }
        }
    }

    /**
     * Maneja un gesto detectado por el sistema avanzado
     */
    private fun handleGesture(gesture: Gesture) {
        Log.d("OneHandKeyboard", "🎯 GESTURE HANDLER CALLED: ${gesture.type} on cell ${gesture.startCell}, direction ${gesture.direction}")
        
        val layout = currentKeyboardLayout
        if (layout == null) {
            Log.e("OneHandKeyboard", "🎯 ERROR: currentKeyboardLayout is NULL!")
            return
        }
        
        Log.d("OneHandKeyboard", "🎯 Processing gesture with layout: ${layout.name}")
        val output = layout.processGesture(gesture)
        Log.d("OneHandKeyboard", "🎯 Layout processGesture returned: $output")
        
        when (output) {
            is KeyboardOutput.Character -> {
                val char = applyCase(output.char.toString())
                Log.d("OneHandKeyboard", "🎯 SENDING CHARACTER: '$char' (original: '${output.char}')")
                
                val inputConnection = currentInputConnection
                if (inputConnection == null) {
                    Log.e("OneHandKeyboard", "🎯 ERROR: currentInputConnection is NULL!")
                    return
                }
                
                val result = inputConnection.commitText(char, 1)
                Log.d("OneHandKeyboard", "🎯 commitText result: $result")
                
                // Manejar modo capitalize
                if (currentCaseMode == com.onehandkeyboard.CaseMode.CAPITALIZE && isCapitalizeNext) {
                    isCapitalizeNext = false
                }
            }
            is KeyboardOutput.Action -> {
                Log.d("OneHandKeyboard", "🎯 EXECUTING ACTION: ${output.action}")
                executeKeyAction(output.action)
            }
            null -> {
                Log.w("OneHandKeyboard", "🎯 WARNING: No output for gesture - gesture not mapped")
            }
        }
    }
    
    /**
     * Ejecuta una acción de teclado
     */
    private fun executeKeyAction(action: KeyAction) {
        when (action) {
            is KeyAction.Backspace -> {
                currentInputConnection?.deleteSurroundingText(1, 0)
            }
            is KeyAction.Space -> {
                currentInputConnection?.commitText(" ", 1)
            }
            is KeyAction.Enter -> {
                currentInputConnection?.commitText("\n", 1)
            }
            is KeyAction.ToggleCase -> {
                switchCaseMode()
            }
            is KeyAction.SwitchToNumbers -> {
                currentLayout = KeyboardLayout.NUMBERS_SYMBOLS
                recreateKeyboard()
            }
            is KeyAction.SwitchToLetters -> {
                currentLayout = KeyboardLayout.SPANISH_LETTERS
                recreateKeyboard()
            }
            is KeyAction.InsertText -> {
                currentInputConnection?.commitText(action.text, 1)
            }
            else -> {
                Log.d("OneHandKeyboard", "Action not implemented: $action")
            }
        }
    }
    
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
    
    private fun createUltraSimpleView(): TextView {
        Log.d("OneHandKeyboard", "Creating ultra-simple fallback")
        return TextView(this).apply {
            text = "OneHand Keyboard\n¡FUNCIONA!"
            textSize = 24f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.BLUE)
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            minimumHeight = 300
        }
    }

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


