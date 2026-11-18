package com.onehandkeyboard.core.design.theme

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.onehandkeyboard.core.design.tokens.DesignTokens
import com.onehandkeyboard.core.design.tokens.ThemePresets
import dagger.hilt.android.qualifiers.ApplicationContext
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
class ThemeManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val sharedPrefs: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        companion object {
            private const val PREFS_NAME = "theme_prefs"
            private const val PREF_THEME_NAME = "selected_theme_name"
            private const val PREF_INVERT_COLORS = "invert_background_colors"
            private const val DEFAULT_THEME_NAME = "Default Light"

            // Broadcast constants for theme changes
            const val ACTION_THEME_CHANGED = "com.onehandkeyboard.THEME_CHANGED"
            const val EXTRA_THEME_NAME = "theme_name"
        }

        private val _currentTheme = MutableStateFlow(ThemePresets.MonokaiDark)
        val currentTheme: StateFlow<DesignTokens> = _currentTheme.asStateFlow()

        private val _currentThemeName = MutableStateFlow(DEFAULT_THEME_NAME)
        val currentThemeName: StateFlow<String> = _currentThemeName.asStateFlow()

        private val _isDarkMode = MutableStateFlow(false)
        val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

        private val _isColorsInverted = MutableStateFlow(false)
        val isColorsInverted: StateFlow<Boolean> = _isColorsInverted.asStateFlow()

        init {
            // Load saved theme on initialization
            loadSavedTheme()
        }

        /**
         * Load saved theme from SharedPreferences
         */
        private fun loadSavedTheme() {
            val savedThemeName = sharedPrefs.getString(PREF_THEME_NAME, DEFAULT_THEME_NAME) ?: DEFAULT_THEME_NAME
            val isInverted = sharedPrefs.getBoolean(PREF_INVERT_COLORS, false)
            val theme = ThemePresets.getThemeByName(savedThemeName)

            if (theme != null) {
                _currentTheme.value = if (isInverted) applyColorInversion(theme) else theme
                _currentThemeName.value = savedThemeName
                _isDarkMode.value = isDarkTheme(savedThemeName)
                _isColorsInverted.value = isInverted
            } else {
                // Fallback to default theme if saved theme is not found
                _currentTheme.value = ThemePresets.MonokaiDark
                _currentThemeName.value = DEFAULT_THEME_NAME
                _isDarkMode.value = false
                _isColorsInverted.value = false
            }
        }

        /**
         * Refresh theme from SharedPreferences - useful for IME service to get latest theme
         */
        fun refreshTheme() {
            loadSavedTheme()
        }

        /**
         * Apply color inversion by swapping backgroundPrimary and backgroundSecondary
         * Also swaps contentPrimary and contentSecondary to maintain proper contrast
         */
        private fun applyColorInversion(theme: DesignTokens): DesignTokens {
            return theme.copy(
                colors =
                    theme.colors.copy(
                        backgroundPrimary = theme.colors.backgroundSecondary,
                        backgroundSecondary = theme.colors.backgroundPrimary,
                        contentPrimary = theme.colors.contentSecondary,
                        contentSecondary = theme.colors.contentPrimary,
                    ),
            )
        }

        /**
         * Send broadcast to notify IME service about theme change
         */
        private fun sendThemeChangedBroadcast(themeName: String) {
            val intent =
                Intent(ACTION_THEME_CHANGED).apply {
                    putExtra(EXTRA_THEME_NAME, themeName)
                    // Use explicit package to ensure it reaches our IME service
                    setPackage(context.packageName)
                }
            context.sendBroadcast(intent)
        }

        /**
         * Set theme by name and persist to SharedPreferences
         */
        fun setTheme(themeName: String) {
            val theme = ThemePresets.getThemeByName(themeName)
            if (theme != null) {
                val finalTheme = if (_isColorsInverted.value) applyColorInversion(theme) else theme
                _currentTheme.value = finalTheme
                _currentThemeName.value = themeName
                _isDarkMode.value = isDarkTheme(themeName)

                // Persist theme selection
                sharedPrefs.edit()
                    .putString(PREF_THEME_NAME, themeName)
                    .apply()

                // Notify IME service about theme change
                sendThemeChangedBroadcast(themeName)
            }
        }

        /**
         * Set custom theme with design tokens and persist
         */
        fun setCustomTheme(
            theme: DesignTokens,
            themeName: String,
        ) {
            val finalTheme = if (_isColorsInverted.value) applyColorInversion(theme) else theme
            _currentTheme.value = finalTheme
            _currentThemeName.value = themeName
            _isDarkMode.value = isDarkTheme(themeName)

            // Persist custom theme selection
            sharedPrefs.edit()
                .putString(PREF_THEME_NAME, themeName)
                .apply()

            // Notify IME service about theme change
            sendThemeChangedBroadcast(themeName)
        }

        /**
         * Toggle color inversion (swap backgroundPrimary and backgroundSecondary)
         */
        fun toggleColorInversion() {
            val newInvertedState = !_isColorsInverted.value
            _isColorsInverted.value = newInvertedState

            // Apply inversion to current theme
            val baseTheme = ThemePresets.getThemeByName(_currentThemeName.value)
            if (baseTheme != null) {
                val finalTheme = if (newInvertedState) applyColorInversion(baseTheme) else baseTheme
                _currentTheme.value = finalTheme
            }

            // Persist inversion preference
            sharedPrefs.edit()
                .putBoolean(PREF_INVERT_COLORS, newInvertedState)
                .apply()

            // Notify IME service about theme change
            sendThemeChangedBroadcast(_currentThemeName.value)
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
         * Get theme preview colors for UI display using the 5 core tokens
         */
        fun getThemePreview(themeName: String): ThemePreview? {
            val theme = ThemePresets.getThemeByName(themeName) ?: return null
            return ThemePreview(
                name = themeName,
                keyboardBackground = theme.colors.backgroundRoot,
                keyBackground = theme.colors.backgroundPrimary,
                actionKeyBackground = theme.colors.backgroundSecondary,
                keyText = theme.colors.contentPrimary,
                actionKeyText = theme.colors.contentSecondary,
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
    val actionKeyText: androidx.compose.ui.graphics.Color,
)
