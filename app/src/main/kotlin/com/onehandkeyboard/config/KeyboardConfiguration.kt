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
) {
    companion object {
        /**
         * Obtiene la configuración para un idioma y modo específicos
         */
        fun getConfiguration(
            language: Language,
            mode: LayoutMode,
        ): KeyboardConfiguration {
            return KeyboardConfigurationFactory.getConfiguration(language, mode)
        }

        /**
         * Configuración por defecto para layout español (mantener compatibilidad)
         */
        fun createSpanishDefault(): KeyboardConfiguration {
            return KeyboardConfigurationFactory.createSpanishDefault()
        }

        /**
         * Configuración personalizable vacía
         */
        fun createEmpty(): KeyboardConfiguration {
            return KeyboardConfigurationFactory.createEmpty()
        }
    }
}
