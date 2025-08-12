package com.onehandkeyboard

import android.util.Log
import com.onehandkeyboard.core.domain.model.KeyData
import com.onehandkeyboard.core.domain.model.CaseMode
import com.onehandkeyboard.enums.Language
import com.onehandkeyboard.enums.LayoutMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages keyboard layout configuration, switching, and state management.
 * 
 * This component handles:
 * - Layout mode switching (LETTERS ↔ SYMBOLS)
 * - Case mode cycling (lowercase → UPPERCASE → Capitalize)
 * - Key data retrieval for current configuration
 * - Layout state coordination
 * 
 * Extracted from OneHandKeyboardService.kt as part of SRP refactoring.
 */
@Singleton
class KeyboardLayoutManager @Inject constructor(
    private val configManager: KeyboardConfigManager
) {
    
    /**
     * Switches between LETTERS and SYMBOLS modes for the current language
     */
    fun switchLayout() {
        Log.d("KeyboardLayoutManager", "Switching layout from ${configManager.currentMode}")
        configManager.switchLayout()
        Log.d("KeyboardLayoutManager", "Layout switched to ${configManager.currentMode}")
    }
    
    /**
     * Cycles through case modes: lowercase → UPPERCASE → Capitalize
     */
    fun switchCaseMode() {
        Log.d("KeyboardLayoutManager", "Switching case mode from ${configManager.currentCaseMode}")
        configManager.switchCaseMode()
        Log.d("KeyboardLayoutManager", "Case mode switched to ${configManager.currentCaseMode}")
    }
    
    /**
     * Gets the current key data array based on active language and layout mode
     */
    fun getCurrentKeyData(): Array<Array<KeyData>> {
        return configManager.getCurrentKeyData()
    }
    
    /**
     * Gets the current layout mode display text for UI
     */
    fun getCurrentLayoutModeText(): String {
        return when (configManager.currentMode) {
            LayoutMode.LETTERS -> "ABC"
            LayoutMode.SYMBOLS -> "123"
        }
    }
    
    /**
     * Gets the current case mode display text for UI
     */
    fun getCurrentCaseModeText(): String {
        return when (configManager.currentCaseMode) {
            CaseMode.LOWERCASE -> "aa"
            CaseMode.UPPERCASE -> "AA"
            CaseMode.CAPITALIZE -> "Aa"
        }
    }
    
    /**
     * Gets the current language display text for UI
     */
    fun getCurrentLanguageText(): String {
        return when (configManager.currentLanguage) {
            Language.SPANISH -> "ES"
            Language.ENGLISH -> "EN"
        }
    }
    
    /**
     * Checks if current mode is LETTERS (affects case mode visibility)
     */
    fun isLettersMode(): Boolean {
        return configManager.currentMode == LayoutMode.LETTERS
    }
    
    /**
     * Gets current configuration state for debugging/logging
     */
    fun getCurrentConfigurationState(): String {
        return "Language: ${configManager.currentLanguage}, " +
               "Mode: ${configManager.currentMode}, " +
               "Case: ${configManager.currentCaseMode}"
    }
    
    /**
     * Gets cell configuration for a specific cell index
     */
    fun getCellConfiguration(cellIndex: Int): com.onehandkeyboard.config.CellConfiguration? {
        val configuration = configManager.keyboardConfiguration
        return configuration.cells.find { it.cellIndex == cellIndex }
    }
}
