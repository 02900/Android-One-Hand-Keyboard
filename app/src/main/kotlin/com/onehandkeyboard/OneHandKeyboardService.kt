package com.onehandkeyboard

import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Main InputMethodService for OneHandKeyboard
 *
 * Ultra-simple version without Hilt/Compose to avoid crashes
 */
class OneHandKeyboardService : InputMethodService() {
    private var keyboardView: LinearLayout? = null

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
        Log.d("OneHandKeyboard", "Creating simple 3x3 keyboard")
        
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.RED) // Rojo brillante para visibilidad
            minimumHeight = 400
            
            // Título
            addView(TextView(this@OneHandKeyboardService).apply {
                text = "OneHand Keyboard - FUNCIONANDO!"
                textSize = 20f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 20)
            })
            
            // Crear grid 3x3 simple
            val letters = arrayOf(
                arrayOf("ABC", "DEF", "GHI"),
                arrayOf("JKL", "MNÑ", "OPQ"),
                arrayOf("RST", "UVW", "XYZ")
            )
            
            val outputs = arrayOf(
                arrayOf("a", "d", "g"),
                arrayOf("j", " ", "o"), // Espacio en el centro
                arrayOf("r", "u", "x")
            )
            
            for (row in 0..2) {
                val rowLayout = LinearLayout(this@OneHandKeyboardService).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                    )
                }
                
                for (col in 0..2) {
                    val button = Button(this@OneHandKeyboardService).apply {
                        text = letters[row][col]
                        textSize = 16f
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            1f
                        ).apply {
                            setMargins(4, 4, 4, 4)
                        }
                        
                        val output = outputs[row][col]
                        setOnClickListener {
                            Log.d("OneHandKeyboard", "Button clicked: $output")
                            currentInputConnection?.commitText(output, 1)
                        }
                    }
                    rowLayout.addView(button)
                }
                
                addView(rowLayout)
            }
            
            // Botón de backspace
            addView(Button(this@OneHandKeyboardService).apply {
                text = "⌫ BORRAR"
                textSize = 16f
                setOnClickListener {
                    Log.d("OneHandKeyboard", "Backspace clicked")
                    currentInputConnection?.deleteSurroundingText(1, 0)
                }
            })
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


