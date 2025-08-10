package com.onehandkeyboard

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

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
class OneHandKeyboardService : InputMethodService() {
    private var keyboardView: LinearLayout? = null
    
    // Estado del teclado
    private var currentLayout = KeyboardLayout.SPANISH_LETTERS
    private var currentCaseMode = CaseMode.LOWERCASE
    private var isCapitalizeNext = false // Para modo capitalize

    override fun onCreateInputView(): View {
        Log.d("OneHandKeyboard", "onCreateInputView() called - SIMPLE VERSION")
        
        try {
            keyboardView = createSimpleKeyboard()
            Log.d("OneHandKeyboard", "Simple keyboard created successfully")
            return keyboardView!!
        } catch (e: Exception) {
            Log.e("OneHandKeyboard", "Error creating keyboard", e)
            // Ultra-fallback
            return createUltraSimpleView()
        }
    }
    
    private fun createSimpleKeyboard(): LinearLayout {
        Log.d("OneHandKeyboard", "Creating enhanced 3x3 keyboard - Layout: $currentLayout, Case: $currentCaseMode")
        
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 12)
            setBackgroundColor(Color.parseColor("#1E1E1E")) // Fondo oscuro moderno
            minimumHeight = 400
            
            // Título con información de estado
            addView(createTitleView())
            
            // Botones de control (layout y case)
            addView(createControlButtons())
            
            // Crear grid 3x3 dinámico
            val keyData = getCurrentKeyData()
            
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
                    val keyInfo = keyData[row][col]
                    val button = createEnhancedButton(keyInfo)
                    rowLayout.addView(button)
                }
                
                addView(rowLayout)
            }
            
            // Botón de backspace mejorado
            addView(createBackspaceButton())
        }
    }
    
    /**
     * Crea un botón mejorado con diseño moderno
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
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(6, 12, 6, 6)
                height = 120
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
            keyboardView = createSimpleKeyboard()
            setInputView(keyboardView)
            Log.d("OneHandKeyboard", "Keyboard recreated successfully")
        } catch (e: Exception) {
            Log.e("OneHandKeyboard", "Error recreating keyboard", e)
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

    override fun onDestroy() {
        keyboardView = null
        super.onDestroy()
    }

    override fun onStartInputView(
        info: android.view.inputmethod.EditorInfo?,
        restarting: Boolean,
    ) {
        super.onStartInputView(info, restarting)
        Log.d("OneHandKeyboard", "onStartInputView() called - keyboard should be visible now")
        Log.d("OneHandKeyboard", "InputView size: ${keyboardView?.width}x${keyboardView?.height}")
        // Configure keyboard based on input type and IME options
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


