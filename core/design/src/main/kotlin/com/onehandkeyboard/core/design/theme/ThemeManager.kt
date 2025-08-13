package com.onehandkeyboard.core.design.theme

import com.onehandkeyboard.core.design.tokens.DesignTokens
import com.onehandkeyboard.core.design.tokens.ThemePresets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Theme manager for handling keyboard theme switching and persistence
 * Manages the current active theme and provides theme-related utilities
 */
@Singleton
class ThemeManager @Inject constructor() {
    
    private val _currentTheme = MutableStateFlow(ThemePresets.DefaultLight)
    val currentTheme: StateFlow<DesignTokens> = _currentTheme.asStateFlow()
    
    private val _currentThemeName = MutableStateFlow("Default Light")
    val currentThemeName: StateFlow<String> = _currentThemeName.asStateFlow()
    
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()
    
    /**
     * Set theme by name
     */
    fun setTheme(themeName: String) {
        val theme = ThemePresets.getThemeByName(themeName)
        if (theme != null) {
            _currentTheme.value = theme
            _currentThemeName.value = themeName
            _isDarkMode.value = isDarkTheme(themeName)
        }
    }
    
    /**
     * Set custom theme with design tokens
     */
    fun setCustomTheme(theme: DesignTokens, themeName: String = "Custom") {
        _currentTheme.value = theme
        _currentThemeName.value = themeName
        _isDarkMode.value = isDarkTheme(themeName)
    }
    
    /**
     * Get all available theme names
     */
    fun getAvailableThemes(): List<String> = ThemePresets.getThemeNames()
    
    /**
     * Get current theme tokens
     */
    fun getCurrentTokens(): DesignTokens = _currentTheme.value
    
    /**
     * Check if current theme is dark
     */
    fun isCurrentThemeDark(): Boolean = _isDarkMode.value
    
    /**
     * Switch to system default theme based on dark mode preference
     */
    fun setSystemDefaultTheme(isDark: Boolean) {
        val themeName = if (isDark) "Default Dark" else "Default Light"
        setTheme(themeName)
    }
    
    /**
     * Reset to default light theme
     */
    fun resetToDefault() {
        setTheme("Default Light")
    }
    
    /**
     * Determine if a theme name represents a dark theme
     */
    private fun isDarkTheme(themeName: String): Boolean {
        return themeName.contains("Dark", ignoreCase = true) ||
               themeName.contains("Monokai", ignoreCase = true) ||
               themeName.contains("Tokyo", ignoreCase = true)
    }
    
    /**
     * Get theme preview colors for UI display
     */
    fun getThemePreview(themeName: String): ThemePreview? {
        val theme = ThemePresets.getThemeByName(themeName) ?: return null
        return ThemePreview(
            name = themeName,
            keyboardBackground = theme.colors.keyboardBackground,
            keyBackground = theme.colors.keyBackground,
            actionKeyBackground = theme.colors.actionKeyBackground,
            keyText = theme.colors.keyText,
            actionKeyText = theme.colors.actionKeyText
        )
    }
    
    /**
     * Get all theme previews for theme selection UI
     */
    fun getAllThemePreviews(): List<ThemePreview> {
        return getAvailableThemes().mapNotNull { getThemePreview(it) }
    }
}

/**
 * Theme preview data for UI display
 */
data class ThemePreview(
    val name: String,
    val keyboardBackground: androidx.compose.ui.graphics.Color,
    val keyBackground: androidx.compose.ui.graphics.Color,
    val actionKeyBackground: androidx.compose.ui.graphics.Color,
    val keyText: androidx.compose.ui.graphics.Color,
    val actionKeyText: androidx.compose.ui.graphics.Color
)
