package com.onehandkeyboard.ui

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import com.onehandkeyboard.config.CellConfiguration
import com.onehandkeyboard.core.domain.model.Direction
import com.onehandkeyboard.core.domain.model.GestureType

/**
 * Factory para crear labels direccionales en las celdas del teclado.
 * 
 * Responsabilidades:
 * - Crear TextViews posicionados para cada dirección
 * - Gestionar la combinación de TAP y LONG_PRESS en el centro
 * - Posicionar labels absolutamente dentro de FrameLayouts
 * - Aplicar estilos específicos según la dirección
 */
class DirectionalLabelFactory(
    private val context: Context,
    private val styleProvider: ButtonStyleProvider,
    private val hintProvider: GestureHintProvider
) {
    
    /**
     * Crea labels posicionados absolutamente para cada dirección en un FrameLayout
     */
    fun createDirectionalLabelsAbsolute(container: FrameLayout, cellConfig: CellConfiguration?, cellIndex: Int) {
        if (cellConfig == null) {
            Log.d("OneHandKeyboard", "⚠️ No configuration found for cell $cellIndex")
            return
        }
        
        // Crear labels para todas las direcciones no-CENTER
        val nonCenterDirections = listOf(
            Direction.N, Direction.NE, Direction.E, Direction.SE,
            Direction.S, Direction.SW, Direction.W, Direction.NW
        )
        
        for (direction in nonCenterDirections) {
            val label = hintProvider.getDirectionalLabel(cellConfig, direction)
            if (label.isNotEmpty()) {
                val textView = createDirectionalTextView(label, direction, GestureType.SWIPE)
                container.addView(textView)
            }
        }
        
        // Crear label combinado para el centro (TAP + LONG_PRESS)
        val centerLabel = hintProvider.getCenterLabel(cellConfig)
        if (centerLabel.isNotEmpty()) {
            val centerTextView = createCenterCombinedTextView(cellConfig)
            container.addView(centerTextView)
        }
    }
    
    /**
     * Crea un TextView posicionado para una dirección específica
     */
    fun createDirectionalTextView(text: String, direction: Direction, gestureType: GestureType): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = styleProvider.getDirectionalLabelTextSize()
            setTextColor(styleProvider.getDirectionalLabelColor())
            gravity = Gravity.CENTER
            
            // Posicionamiento absoluto según la dirección
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
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
     * Maneja la combinación de TAP y LONG_PRESS para el centro
     */
    fun createCenterCombinedTextView(cellConfig: CellConfiguration): TextView {
        val centerLabel = hintProvider.getCenterLabel(cellConfig)
        
        return TextView(context).apply {
            text = centerLabel
            textSize = styleProvider.getCenterTextSize()
            setTextColor(styleProvider.getCenterTextColor())
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
     * Crea el texto con labels direccionales para una celda (método legacy)
     */
    fun createDirectionalLabels(cellConfig: CellConfiguration?, cellIndex: Int): String {
        if (cellConfig == null) return ""
        
        val labels = mutableListOf<String>()
        
        // Agregar labels para cada dirección configurada
        for (gestureAction in cellConfig.gestureActions) {
            val directionSymbol = hintProvider.getDirectionSymbol(gestureAction.direction)
            val label = hintProvider.getDirectionalLabel(cellConfig, gestureAction.direction)
            if (label.isNotEmpty()) {
                labels.add("$directionSymbol$label")
            }
        }
        
        return labels.joinToString(" ")
    }
    
    /**
     * Crea la vista del título con información de estado
     */
    fun createTitleView(): TextView {
        return TextView(context).apply {
            text = "OneHand Keyboard - 3x3 Grid"
            textSize = 14f
            setTextColor(android.graphics.Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
        }
    }
}
