package com.onehandkeyboard.config

import com.onehandkeyboard.enums.Language
import com.onehandkeyboard.enums.LayoutMode

/**
 * Configuración externa y flexible para las acciones de cada celda del teclado
 */
data class KeyboardConfiguration(
    val name: String,
    val description: String,
    val language: Language,
    val mode: LayoutMode,
    val cells: List<CellConfiguration>,
)
