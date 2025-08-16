package com.onehandkeyboard.ui

import android.content.Context
import android.graphics.Color
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout

/**
 * Factory para crear botones de control del teclado.
 *
 * Responsabilidades:
 * - Crear botones de layout (cambio de idioma/modo)
 * - Crear botones de case (mayúsculas/minúsculas)
 * - Crear botón de backspace
 * - Gestionar estilos específicos de botones de control
 */
class ControlButtonFactory(
    private val context: Context,
    private val layoutCalculator: LayoutCalculator,
    private val styleProvider: ButtonStyleProvider,
) {
    /**
     * Crea el botón individual para cambiar layout (para columna lateral)
     */
    fun createLayoutButton(onClickListener: () -> Unit): Button {
        return Button(context).apply {
            text = "Layout"
            textSize = styleProvider.getControlTextSize()
            setTextColor(Color.parseColor("#4CAF50"))
            val cellHeight = layoutCalculator.calculateCellHeight()
            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    cellHeight, // Usar la misma altura que las celdas
                ).apply {
                    setMargins(0, 4, 0, 4)
                }
            background = styleProvider.createButtonBackground(false, false, true)

            setOnClickListener { onClickListener() }
        }
    }

    /**
     * Crea el botón individual para cambiar case (para columna lateral)
     */
    fun createCaseButton(onClickListener: () -> Unit): Button {
        return Button(context).apply {
            text = "Aa"
            textSize = styleProvider.getControlTextSize()
            setTextColor(Color.parseColor("#2196F3"))
            val cellHeight = layoutCalculator.calculateCellHeight()
            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    cellHeight, // Usar la misma altura que las celdas
                ).apply {
                    setMargins(0, 4, 0, 4)
                }
            background = styleProvider.createButtonBackground(false, false, true)

            setOnClickListener { onClickListener() }
        }
    }

    /**
     * Crea el botón de backspace mejorado
     */
    fun createBackspaceButton(onClickListener: () -> Unit): FrameLayout {
        return FrameLayout(context).apply {
            val cellHeight = layoutCalculator.calculateCellHeight()
            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    cellHeight, // Usar la misma altura que las celdas
                ).apply {
                    setMargins(8, 8, 8, 8)
                }

            background = styleProvider.createButtonBackground(false, true)

            // TextView para el símbolo de backspace
            val backspaceText =
                android.widget.TextView(context).apply {
                    text = "⌫"
                    textSize = styleProvider.getStandardTextSize()
                    setTextColor(styleProvider.getTextColor(isBackspace = true))
                    gravity = android.view.Gravity.CENTER
                    layoutParams =
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT,
                        )
                }

            addView(backspaceText)

            // Configurar click listener
            setOnClickListener { onClickListener() }

            // Sin feedback visual al presionar para apariencia plana
        }
    }

    /**
     * Crea un botón mejorado con diseño moderno (método legacy)
     */
    fun createLegacyButton(
        keyData: com.onehandkeyboard.core.domain.model.KeyData,
        onClickListener: (String) -> Unit,
    ): FrameLayout {
        return FrameLayout(context).apply {
            layoutParams =
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                ).apply {
                    setMargins(4, 4, 4, 4)
                }

            background = styleProvider.createButtonBackground(false)

            // TextView principal
            val mainText =
                android.widget.TextView(context).apply {
                    text = keyData.primaryChar
                    textSize = styleProvider.getStandardTextSize()
                    setTextColor(styleProvider.getTextColor())
                    gravity = android.view.Gravity.CENTER
                    layoutParams =
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT,
                        )
                }

            addView(mainText)

            // Configurar click listener
            setOnClickListener { onClickListener(keyData.primaryChar) }

            // Sin feedback visual al presionar para apariencia plana
        }
    }
}
