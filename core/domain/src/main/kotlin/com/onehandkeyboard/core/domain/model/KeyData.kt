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
    val label: String, // Texto mostrado en el botón (ABC, DEF, etc.)
    val primaryChar: String, // Carácter principal (a, d, g, etc.)
    val accentChars: String, // Caracteres con acentos (áàâä, etc.)
)
