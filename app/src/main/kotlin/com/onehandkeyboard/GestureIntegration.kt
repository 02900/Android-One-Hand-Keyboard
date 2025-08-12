package com.onehandkeyboard

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import android.widget.LinearLayout
import com.onehandkeyboard.config.*
import com.onehandkeyboard.core.domain.model.*
import com.onehandkeyboard.core.gesture.CellBounds
import com.onehandkeyboard.core.gesture.GestureDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Handles gesture integration and processing for the OneHandKeyboard.
 * 
 * This class centralizes all gesture-related logic including:
 * - Gesture system initialization with DefaultGestureDetector
 * - Cell bounds calculation for 3x3 grid layout
 * - Domain keyboard layout creation from configuration
 * - Gesture processing and event handling via coroutines
 * - Direction detection and mapping to keyboard actions
 * - Integration between UI touch events and gesture detection
 * 
 * Extracted from OneHandKeyboardService for better separation of concerns.
 * Uses manual initialization due to Context and callback dependencies.
 */
class GestureIntegration(
    private val context: Context,
    private val configManager: KeyboardConfigManager,
    private val gestureDetector: GestureDetector
) {
    
    // Estado para procesamiento directo de gestos
    private var gestureStartTime = 0L
    private var gestureStartX = 0f
    private var gestureStartY = 0f
    private var isLongPressDetected = false
    private var gestureAlreadyEmitted = false // Prevenir repetición de gestos
    private val longPressThreshold = 500L // 500ms
    private val swipeThreshold = 50f // pixels
    
    // Sistema de gestos avanzado
    private var currentKeyboardLayout: com.onehandkeyboard.core.domain.model.KeyboardLayout? = null
    private var cellBounds: Array<CellBounds> = emptyArray()
    private var gestureJob: Job? = null
    private val gestureScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // ============================================================================
    // REGION: GESTURE SYSTEM INITIALIZATION
    // ============================================================================
    
    /**
     * Inicializa el sistema de gestos avanzado
     */
    fun initializeGestureSystem() {
        Log.d("OneHandKeyboard", "🔥 Initializing advanced gesture system...")
        
        // Crear layout del dominio
        currentKeyboardLayout = createDomainKeyboardLayout()
        
        // Configurar el detector de gestos con configuración por defecto
        val config = GestureConfig()
        gestureDetector.updateConfig(config)
        Log.d("OneHandKeyboard", "🔥 Gesture detector configured with: $config")
        
        // Iniciar recolección de gestos
        gestureJob?.cancel()
        gestureJob = gestureScope.launch {
            try {
                gestureDetector.gestures.collect { gesture ->
                    Log.d("OneHandKeyboard", "🔥 GESTURE DETECTED: $gesture")
                    // TODO: Implement gesture handling when needed
                }
            } catch (e: Exception) {
                Log.e("OneHandKeyboard", "🔥 Error in gesture collection", e)
            }
        }
        
        Log.d("OneHandKeyboard", "🔥 Advanced gesture system initialized successfully")
    }
    
    // ============================================================================
    // REGION: CELL BOUNDS CALCULATION
    // ============================================================================
    
    /**
     * Calcula los bounds de las celdas para el detector de gestos
     */
    fun calculateCellBounds(keyboardView: LinearLayout?) {
        if (keyboardView == null) {
            Log.w("OneHandKeyboard", "🔥 Cannot calculate bounds - keyboardView is null")
            return
        }
        
        Log.d("OneHandKeyboard", "🔥 Starting cell bounds calculation...")
        
        // Buscar el gridContainer (segundo hijo del LinearLayout principal)
        val mainRow = keyboardView.getChildAt(1) as? LinearLayout
        if (mainRow == null) {
            Log.e("OneHandKeyboard", "🔥 Cannot find mainRow")
            return
        }
        
        val gridContainer = mainRow.getChildAt(0) as? LinearLayout
        if (gridContainer == null) {
            Log.e("OneHandKeyboard", "🔥 Cannot find gridContainer")
            return
        }
        
        val bounds = mutableListOf<CellBounds>()
        
        // Iterar por las 3 filas
        for (row in 0..2) {
            val rowLayout = gridContainer.getChildAt(row) as? LinearLayout
            if (rowLayout == null) {
                Log.w("OneHandKeyboard", "🔥 Row $row is null")
                continue
            }
            
            // Iterar por las 3 columnas
            for (col in 0..2) {
                val cellIndex = row * 3 + col
                val cellView = rowLayout.getChildAt(col)
                
                if (cellView != null) {
                    val location = IntArray(2)
                    cellView.getLocationOnScreen(location)
                    
                    val cellBounds = CellBounds(
                        cellIndex = cellIndex,
                        left = location[0].toFloat(),
                        top = location[1].toFloat(),
                        right = (location[0] + cellView.width).toFloat(),
                        bottom = (location[1] + cellView.height).toFloat()
                    )
                    
                    bounds.add(cellBounds)
                    Log.d("OneHandKeyboard", "🔥 Cell $cellIndex bounds: $cellBounds")
                } else {
                    Log.w("OneHandKeyboard", "🔥 Cell view at [$row][$col] is null")
                }
            }
        }
        
        cellBounds = bounds.toTypedArray()
        
        Log.d("OneHandKeyboard", "🔥 Cell bounds calculation completed. Total bounds: ${cellBounds.size}")
    }
    
    // ============================================================================
    // REGION: DOMAIN KEYBOARD LAYOUT CREATION
    // ============================================================================
    
    /**
     * Crea el layout del dominio según el idioma y modo actual usando configuración externa
     */
    fun createDomainKeyboardLayout(): com.onehandkeyboard.core.domain.model.KeyboardLayout {
        // TODO: Use external configuration instead of hardcoded layouts
        // For now, return a simple layout to avoid compilation errors
        val cells = (0..8).map { index ->
            Cell.create(
                index = index,
                label = "Cell$index",
                primaryChars = listOf('a'),
                secondaryChars = emptyMap()
            )
        }
        
        return com.onehandkeyboard.core.domain.model.KeyboardLayout(
            id = "${configManager.currentLanguage.name.lowercase()}_${configManager.currentMode.name.lowercase()}",
            name = "${configManager.currentLanguage.name} ${configManager.currentMode.name}",
            description = "Current active layout: ${configManager.currentLanguage.name} ${configManager.currentMode.name}",
            cells = cells,
            type = when (configManager.currentMode) {
                LayoutMode.LETTERS -> LayoutType.LETTERS
                LayoutMode.SYMBOLS -> LayoutType.NUMBERS
            },
            locale = when (configManager.currentLanguage) {
                Language.SPANISH -> "es-MX"
                Language.ENGLISH -> "en-US"
            }
        )
    }
    
    // TODO: Remove hardcoded cell creation functions - use external configuration instead
    
    // ============================================================================
    // REGION: GESTURE PROCESSING
    // ============================================================================
    
    /**
     * Procesa eventos de gesto en tiempo real
     */
    fun processGestureEvent(event: MotionEvent, cellIndex: Int, onGestureProcessed: (KeyboardOutput?) -> Unit) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                gestureStartTime = System.currentTimeMillis()
                gestureStartX = event.x
                gestureStartY = event.y
                isLongPressDetected = false
                gestureAlreadyEmitted = false
                Log.d("OneHandKeyboard", "🎯 ACTION_DOWN on cell $cellIndex at (${event.x}, ${event.y})")
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (gestureAlreadyEmitted) return
                
                val currentTime = System.currentTimeMillis()
                val deltaTime = currentTime - gestureStartTime
                val deltaX = event.x - gestureStartX
                val deltaY = event.y - gestureStartY
                val distance = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)
                
                // Detectar long press
                if (!isLongPressDetected && deltaTime >= longPressThreshold && distance < swipeThreshold) {
                    isLongPressDetected = true
                    gestureAlreadyEmitted = true
                    Log.d("OneHandKeyboard", "🎯 LONG_PRESS detected on cell $cellIndex")
                    handleGestureAction(cellIndex, GestureType.LONG_PRESS, Direction.CENTER, onGestureProcessed)
                }
                
                // Detectar swipe
                if (!gestureAlreadyEmitted && distance >= swipeThreshold) {
                    val direction = getSwipeDirection(deltaX, deltaY)
                    gestureAlreadyEmitted = true
                    Log.d("OneHandKeyboard", "🎯 SWIPE detected on cell $cellIndex, direction: $direction")
                    handleGestureAction(cellIndex, GestureType.SWIPE, direction, onGestureProcessed)
                }
            }
            
            MotionEvent.ACTION_UP -> {
                if (gestureAlreadyEmitted) return
                
                val currentTime = System.currentTimeMillis()
                val deltaTime = currentTime - gestureStartTime
                val deltaX = event.x - gestureStartX
                val deltaY = event.y - gestureStartY
                val distance = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)
                
                // Detectar tap (si no hubo long press ni swipe)
                if (!isLongPressDetected && distance < swipeThreshold) {
                    Log.d("OneHandKeyboard", "🎯 TAP detected on cell $cellIndex")
                    handleGestureAction(cellIndex, GestureType.TAP, Direction.CENTER, onGestureProcessed)
                }
            }
        }
    }
    
    /**
     * Determina la dirección del swipe basado en deltaX y deltaY
     */
    private fun getSwipeDirection(deltaX: Float, deltaY: Float): Direction {
        val absX = kotlin.math.abs(deltaX)
        val absY = kotlin.math.abs(deltaY)
        
        return when {
            absX > absY * 2 -> if (deltaX > 0) Direction.E else Direction.W
            absY > absX * 2 -> if (deltaY > 0) Direction.S else Direction.N
            deltaX > 0 && deltaY < 0 -> Direction.NE
            deltaX > 0 && deltaY > 0 -> Direction.SE
            deltaX < 0 && deltaY > 0 -> Direction.SW
            deltaX < 0 && deltaY < 0 -> Direction.NW
            else -> Direction.CENTER
        }
    }
    
    // ============================================================================
    // REGION: GESTURE HANDLING
    // ============================================================================
    
    /**
     * Maneja una acción de gesto detectada usando configuración externa
     */
    fun handleGestureAction(
        cellIndex: Int, 
        gestureType: GestureType, 
        direction: Direction,
        onGestureProcessed: (KeyboardOutput?) -> Unit
    ) {
        Log.d("OneHandKeyboard", "🎯 Processing direct gesture: $gestureType + $direction on cell $cellIndex")
        
        // Validar combinación de gesto y dirección
        val isValidCombination = when (gestureType) {
            GestureType.TAP, GestureType.LONG_PRESS -> direction == Direction.CENTER
            GestureType.SWIPE -> direction != Direction.CENTER
            GestureType.DRAG, GestureType.FLICK -> direction != Direction.CENTER
        }
        
        if (!isValidCombination) {
            Log.e("OneHandKeyboard", "🎯 INVALID GESTURE: $gestureType cannot be performed in direction $direction")
            onGestureProcessed(null)
            return
        }
        
        // Buscar la configuración de la celda
        val cellConfig = configManager.keyboardConfiguration.cells.find { it.cellIndex == cellIndex }
        if (cellConfig == null) {
            Log.w("OneHandKeyboard", "🎯 No configuration found for cell $cellIndex")
            onGestureProcessed(null)
            return
        }
        
        // Buscar la acción configurada para este gesto
        val actionConfig = cellConfig.getActionFor(gestureType, direction)
        if (actionConfig == null) {
            Log.d("OneHandKeyboard", "🎯 No action configured for $gestureType + $direction on cell $cellIndex")
            onGestureProcessed(null)
            return
        }
        
        Log.d("OneHandKeyboard", "🎯 Found action config: $actionConfig")
        
        // Convertir a KeyboardOutput
        val output = when (actionConfig) {
            is ActionConfig.Character -> {
                val char = configManager.applyCase(actionConfig.char.toString())
                Log.d("OneHandKeyboard", "🎯 SENDING CHARACTER: '$char' (original: '${actionConfig.char}')")
                KeyboardOutput.Character(char)
            }
            is ActionConfig.Action -> {
                Log.d("OneHandKeyboard", "🎯 EXECUTING ACTION: ${actionConfig.keyAction}")
                KeyboardOutput.Action(actionConfig.keyAction)
            }
            is ActionConfig.Text -> {
                val text = configManager.applyCase(actionConfig.text)
                Log.d("OneHandKeyboard", "🎯 SENDING TEXT: '$text'")
                KeyboardOutput.Character(text)
            }
        }
        
        onGestureProcessed(output)
    }
    
    // TODO: Implement handleGesture function when needed
    
    // ============================================================================
    // REGION: CLEANUP
    // ============================================================================
    
    /**
     * Limpia recursos del sistema de gestos
     */
    fun cleanup() {
        gestureJob?.cancel()
        gestureJob = null
        Log.d("OneHandKeyboard", "🔥 Gesture system cleaned up")
    }
}

// KeyboardOutput moved to KeyboardActionProcessor.kt
