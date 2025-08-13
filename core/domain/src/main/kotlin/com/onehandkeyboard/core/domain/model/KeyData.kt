package com.onehandkeyboard.core.domain.model

/**
 * Representa los datos de una tecla del teclado
 *
 * Contiene toda la información necesaria para mostrar y procesar una tecla:
 * - label: El texto mostrado en el botón
 * - primaryChar: El carácter principal que se inserta con tap
 * - accentChars: Los caracteres con acentos disponibles para esta tecla
 */
data class KeyData(
    // Texto mostrado en el botón (ABC, DEF, etc.)
    val label: String,
    // Carácter principal (a, d, g, etc.)
    val primaryChar: String,
    // Caracteres con acentos (áàâä, etc.)
    val accentChars: String,
)
