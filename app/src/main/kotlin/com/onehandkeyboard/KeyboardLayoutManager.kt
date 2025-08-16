package com.onehandkeyboard

import android.util.Log
import com.onehandkeyboard.config.KeyboardConfiguration
import com.onehandkeyboard.config.KeyboardConfigurationFactory
import com.onehandkeyboard.core.domain.model.CaseMode
import com.onehandkeyboard.core.domain.model.KeyData
import com.onehandkeyboard.enums.Language
import com.onehandkeyboard.enums.LayoutMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages keyboard layout configuration, switching, and state management.
 *
 * This component handles:
 * - Language switching (Spanish, English)
 * - Layout mode switching (LETTERS ↔ SYMBOLS)
 * - Case mode cycling (lowercase → UPPERCASE → Capitalize)
 * - Key data retrieval for current configuration
 * - Layout state coordination
 *
 * Consolidated from KeyboardConfigManager to eliminate redundant abstraction layer.
 */
@Singleton
class KeyboardLayoutManager
    @Inject
    constructor(
        private val configFactory: KeyboardConfigurationFactory
    ) {
    
    // Estado del teclado
    var currentLanguage = Language.SPANISH
        private set
    var currentMode = LayoutMode.LETTERS
        private set
    var currentCaseMode = CaseMode.LOWERCASE
        private set
    var isCapitalizeNext = false
        private set

    // Sistema de configuración externa
    var keyboardConfiguration: KeyboardConfiguration =
        configFactory.getConfiguration(
            Language.SPANISH,
            LayoutMode.LETTERS,
        )
        private set


    /**
     * Switches between LETTERS and SYMBOLS modes for the current language
     */
    fun switchLayout(): Boolean {
            val previousMode = currentMode
            currentMode =
                when (currentMode) {
                    LayoutMode.LETTERS -> LayoutMode.SYMBOLS
                    LayoutMode.SYMBOLS -> LayoutMode.LETTERS
                }

            // Actualizar configuración del teclado
            keyboardConfiguration = configFactory.getConfiguration(currentLanguage, currentMode)

            Log.d("KeyboardLayoutManager", "Layout switched from $previousMode to $currentMode")
            return previousMode != currentMode
        }

        /**
         * Toggles between case modes: lowercase ↔ UPPERCASE
         */
        fun switchCaseMode(): Boolean {
            val previousCaseMode = currentCaseMode
            currentCaseMode =
                when (currentCaseMode) {
                    CaseMode.LOWERCASE -> CaseMode.UPPERCASE
                    CaseMode.UPPERCASE -> CaseMode.LOWERCASE
                    CaseMode.CAPITALIZE -> CaseMode.LOWERCASE // Fallback for existing state
                }

            // Reset capitalize flag
            isCapitalizeNext = false

            Log.d("KeyboardLayoutManager", "Case mode switched from $previousCaseMode to $currentCaseMode")
            return previousCaseMode != currentCaseMode
        }

        /**
         * Switches the keyboard language and updates configuration
         */
        fun switchLanguage(): Boolean {
            val previousLanguage = currentLanguage
            currentLanguage =
                when (currentLanguage) {
                    Language.SPANISH -> Language.ENGLISH
                    Language.ENGLISH -> Language.SPANISH
                }

            // Actualizar configuración del teclado
            keyboardConfiguration = configFactory.getConfiguration(currentLanguage, currentMode)

            Log.d("KeyboardLayoutManager", "Language switched from $previousLanguage to $currentLanguage")
            return true
        }

        /**
         * Gets the current key data array based on active language and layout mode
         */
        fun getCurrentKeyData(): Array<Array<KeyData>> {
            return when (currentLanguage) {
                Language.SPANISH ->
                    when (currentMode) {
                        LayoutMode.LETTERS -> getSpanishLettersData()
                        LayoutMode.SYMBOLS -> getNumbersSymbolsData()
                    }
                Language.ENGLISH ->
                    when (currentMode) {
                        LayoutMode.LETTERS -> getEnglishLettersData()
                        LayoutMode.SYMBOLS -> getNumbersSymbolsData()
                    }
            }
        }

        /**
         * Aplica el modo de mayúsculas/minúsculas al carácter
         */
        fun applyCase(char: String): String {
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
         * Maneja el evento de escritura de un carácter para el modo capitalize
         */
        fun onCharacterWritten() {
            if (currentCaseMode == CaseMode.CAPITALIZE && isCapitalizeNext) {
                isCapitalizeNext = false
            }
        }

        /**
         * Switches to symbols mode
         */
        fun switchToSymbols(): Boolean {
            if (currentMode != LayoutMode.SYMBOLS) {
                currentMode = LayoutMode.SYMBOLS
                keyboardConfiguration = configFactory.getConfiguration(currentLanguage, currentMode)
                Log.d("KeyboardLayoutManager", "Switched to symbols mode")
                return true
            }
            return false
        }

        /**
         * Switches to letters mode
         */
        fun switchToLetters(): Boolean {
            if (currentMode != LayoutMode.LETTERS) {
                currentMode = LayoutMode.LETTERS
                keyboardConfiguration = configFactory.getConfiguration(currentLanguage, currentMode)
                Log.d("KeyboardLayoutManager", "Switched to letters mode")
                return true
            }
            return false
        }

        /**
         * Toggles caps lock (permanent uppercase)
         */
        fun toggleCapsLock(): Boolean {
            currentCaseMode = if (currentCaseMode == CaseMode.UPPERCASE) {
                CaseMode.LOWERCASE
            } else {
                CaseMode.UPPERCASE
            }
            isCapitalizeNext = false
            Log.d("KeyboardLayoutManager", "Caps lock toggled to: $currentCaseMode")
            return true
        }

        /**
         * Obtiene el título del teclado con información de estado
         */
        fun getKeyboardTitle(): String {
            return "OneHand Keyboard - ${currentLanguage.name} ${currentMode.name} (${currentCaseMode.name})"
        }

        /**
         * Gets the current layout mode display text for UI
         */
        fun getCurrentLayoutModeText(): String {
            return when (currentMode) {
                LayoutMode.LETTERS -> "ABC"
                LayoutMode.SYMBOLS -> "123"
            }
        }

        /**
         * Gets the current case mode display text for UI
         */
        fun getCurrentCaseModeText(): String {
            return when (currentCaseMode) {
                CaseMode.LOWERCASE -> "aa"
                CaseMode.UPPERCASE -> "AA"
                CaseMode.CAPITALIZE -> "Aa"
            }
        }

        /**
         * Gets the current language display text for UI
         */
        fun getCurrentLanguageText(): String {
            return when (currentLanguage) {
                Language.SPANISH -> "ES"
                Language.ENGLISH -> "EN"
            }
        }

        /**
         * Checks if current mode is LETTERS (affects case mode visibility)
         */
        fun isLettersMode(): Boolean {
            return currentMode == LayoutMode.LETTERS
        }

        /**
         * Gets current configuration state for debugging/logging
         */
        fun getCurrentConfigurationState(): String {
            return "Language: $currentLanguage, " +
                "Mode: $currentMode, " +
                "Case: $currentCaseMode"
        }

        /**
         * Gets cell configuration for a specific cell index
         */
        fun getCellConfiguration(cellIndex: Int): com.onehandkeyboard.config.CellConfiguration? {
            return keyboardConfiguration.cells.find { it.cellIndex == cellIndex }
        }

        // ============================================================================
        // PRIVATE: Data providers for different layouts
        // ============================================================================

        /**
         * Datos para layout español
         */
        private fun getSpanishLettersData(): Array<Array<KeyData>> {
            return arrayOf(
                arrayOf(
                    KeyData("ABC", "a", "áàâä"),
                    KeyData("DEF", "d", ""),
                    KeyData("GHI", "g", ""),
                ),
                arrayOf(
                    KeyData("JKL", "j", ""),
                    KeyData("MNÑ", "m", ""),
                    KeyData("OPQ", "o", "óòôö"),
                ),
                arrayOf(
                    KeyData("RST", "r", ""),
                    KeyData("UVW", "u", "úùûü"),
                    KeyData("XYZ", "x", ""),
                ),
            )
        }

        /**
         * Datos para layout inglés
         */
        private fun getEnglishLettersData(): Array<Array<KeyData>> {
            return arrayOf(
                arrayOf(
                    KeyData("ABC", "a", "àáâäã"),
                    KeyData("DEF", "d", ""),
                    KeyData("GHI", "g", ""),
                ),
                arrayOf(
                    KeyData("JKL", "j", ""),
                    KeyData("MNO", "m", ""),
                    KeyData("PQR", "p", ""),
                ),
                arrayOf(
                    KeyData("STU", "s", ""),
                    KeyData("VWX", "v", ""),
                    KeyData("YZ", "y", ""),
                ),
            )
        }

        /**
         * Datos para layout números y símbolos
         */
        private fun getNumbersSymbolsData(): Array<Array<KeyData>> {
            return arrayOf(
                arrayOf(
                    KeyData("123", "1", ""),
                    KeyData("456", "4", ""),
                    KeyData("789", "7", ""),
                ),
                arrayOf(
                    KeyData("*0#", "*", ""),
                    KeyData(".,?", ".", ""),
                    KeyData("!@$", "!", ""),
                ),
                arrayOf(
                    KeyData("()[]", "(", ""),
                    KeyData("+-=", "+", ""),
                    KeyData("\"';", "\"", ""),
                ),
            )
        }
    }
