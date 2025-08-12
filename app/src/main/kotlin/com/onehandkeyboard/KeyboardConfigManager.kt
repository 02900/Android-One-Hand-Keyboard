package com.onehandkeyboard

import android.util.Log
import com.onehandkeyboard.config.*

/**
 * Manages keyboard configuration, language switching, and layout state
 * 
 * Extracted from OneHandKeyboardService for better separation of concerns
 */
class KeyboardConfigManager {
    
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
    var keyboardConfiguration: KeyboardConfiguration = KeyboardConfiguration.getConfiguration(Language.SPANISH, LayoutMode.LETTERS)
        private set
    
    /**
     * Cambia entre modos LETTERS y SYMBOLS para el idioma actual
     */
    fun switchLayout(): Boolean {
        val previousMode = currentMode
        currentMode = when (currentMode) {
            LayoutMode.LETTERS -> LayoutMode.SYMBOLS
            LayoutMode.SYMBOLS -> LayoutMode.LETTERS
        }
        
        // Actualizar configuración del teclado
        keyboardConfiguration = KeyboardConfiguration.getConfiguration(currentLanguage, currentMode)
        
        Log.d("KeyboardConfigManager", "Layout switched from $previousMode to $currentMode")
        return previousMode != currentMode
    }
    
    /**
     * Cambia entre modos de mayúsculas
     */
    fun switchCaseMode(): Boolean {
        val previousCaseMode = currentCaseMode
        currentCaseMode = when (currentCaseMode) {
            CaseMode.LOWERCASE -> CaseMode.UPPERCASE
            CaseMode.UPPERCASE -> CaseMode.CAPITALIZE
            CaseMode.CAPITALIZE -> CaseMode.LOWERCASE
        }
        
        // Reset capitalize flag
        isCapitalizeNext = currentCaseMode == CaseMode.CAPITALIZE
        
        Log.d("KeyboardConfigManager", "Case mode switched from $previousCaseMode to $currentCaseMode")
        return previousCaseMode != currentCaseMode
    }
    
    /**
     * Cambia el idioma actual
     */
    fun switchLanguage(newLanguage: Language): Boolean {
        if (currentLanguage == newLanguage) return false
        
        val previousLanguage = currentLanguage
        currentLanguage = newLanguage
        
        // Actualizar configuración del teclado
        keyboardConfiguration = KeyboardConfiguration.getConfiguration(currentLanguage, currentMode)
        
        Log.d("KeyboardConfigManager", "Language switched from $previousLanguage to $currentLanguage")
        return true
    }
    
    /**
     * Obtiene los datos de las teclas según el idioma y modo actual
     */
    fun getCurrentKeyData(): Array<Array<KeyData>> {
        return when (currentLanguage) {
            Language.SPANISH -> when (currentMode) {
                LayoutMode.LETTERS -> getSpanishLettersData()
                LayoutMode.SYMBOLS -> getNumbersSymbolsData()
            }
            Language.ENGLISH -> when (currentMode) {
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
     * Obtiene el título del teclado con información de estado
     */
    fun getKeyboardTitle(): String {
        return "OneHand Keyboard - ${currentLanguage.name} ${currentMode.name} (${currentCaseMode.name})"
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
                KeyData("GHI", "g", "")
            ),
            arrayOf(
                KeyData("JKL", "j", ""),
                KeyData("MNÑ", "m", ""),
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
                KeyData("ABC", "a", "àáâäã"),
                KeyData("DEF", "d", ""),
                KeyData("GHI", "g", "")
            ),
            arrayOf(
                KeyData("JKL", "j", ""),
                KeyData("MNO", "m", ""),
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
                KeyData("123", "1", ""),
                KeyData("456", "4", ""),
                KeyData("789", "7", "")
            ),
            arrayOf(
                KeyData("*0#", "*", ""),
                KeyData(".,?", ".", ""),
                KeyData("!@$", "!", "")
            ),
            arrayOf(
                KeyData("()[]", "(", ""),
                KeyData("+-=", "+", ""),
                KeyData("\"';", "\"", "")
            )
        )
    }
}
