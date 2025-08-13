package com.onehandkeyboard.core.design.tokens

import androidx.compose.ui.graphics.Color

/**
 * Popular theme presets for keyboard styling
 * Includes popular developer themes like Monokai, Solarized, Tokyo Night, etc.
 */
object ThemePresets {
    
    /**
     * Default light theme
     */
    val DefaultLight = DesignTokens(
        colors = ColorTokens(
            // Background colors
            keyboardBackground = Color(0xFFFFFBFE),
            keyBackground = Color(0xFFE7E0EC),
            keyBackgroundPressed = Color(0xFFD0C7D8),
            keyBackgroundDisabled = Color(0xFFF5F5F5),
            
            // Action key colors
            actionKeyBackground = Color(0xFF6750A4),
            actionKeyBackgroundPressed = Color(0xFF5A4A94),
            actionKeyBackgroundDisabled = Color(0xFFBDBDBD),
            
            // Control key colors
            controlKeyBackground = Color(0xFF79747E),
            controlKeyBackgroundPressed = Color(0xFF6B656F),
            controlKeyBackgroundActive = Color(0xFF6750A4),
            
            // Text colors
            keyText = Color(0xFF1C1B1F),
            keyTextSecondary = Color(0xFF49454F),
            keyTextDisabled = Color(0xFF9E9E9E),
            actionKeyText = Color(0xFFFFFFFF),
            controlKeyText = Color(0xFFFFFFFF),
            
            // Border colors
            keyBorder = Color(0xFFCAC4D0),
            keyBorderFocused = Color(0xFF6750A4),
            keyBorderPressed = Color(0xFF5A4A94),
            
            // Surface colors
            surface = Color(0xFFFFFBFE),
            surfaceVariant = Color(0xFFE7E0EC),
            onSurface = Color(0xFF1C1B1F),
            onSurfaceVariant = Color(0xFF49454F),
            
            // Accent colors
            primary = Color(0xFF6750A4),
            primaryVariant = Color(0xFF5A4A94),
            secondary = Color(0xFF625B71),
            secondaryVariant = Color(0xFF4A4458),
            
            // Status colors
            error = Color(0xFFBA1A1A),
            warning = Color(0xFFFF9800),
            success = Color(0xFF4CAF50),
            info = Color(0xFF2196F3)
        )
    )
    
    /**
     * Default dark theme
     */
    val DefaultDark = DesignTokens(
        colors = ColorTokens(
            // Background colors
            keyboardBackground = Color(0xFF1C1B1F),
            keyBackground = Color(0xFF49454F),
            keyBackgroundPressed = Color(0xFF625B71),
            keyBackgroundDisabled = Color(0xFF2C2C2C),
            
            // Action key colors
            actionKeyBackground = Color(0xFF7F67BE),
            actionKeyBackgroundPressed = Color(0xFF6F57AE),
            actionKeyBackgroundDisabled = Color(0xFF424242),
            
            // Control key colors
            controlKeyBackground = Color(0xFF625B71),
            controlKeyBackgroundPressed = Color(0xFF4A4458),
            controlKeyBackgroundActive = Color(0xFF7F67BE),
            
            // Text colors
            keyText = Color(0xFFE6E1E5),
            keyTextSecondary = Color(0xFFCAC4D0),
            keyTextDisabled = Color(0xFF616161),
            actionKeyText = Color(0xFFFFFFFF),
            controlKeyText = Color(0xFFFFFFFF),
            
            // Border colors
            keyBorder = Color(0xFF79747E),
            keyBorderFocused = Color(0xFF7F67BE),
            keyBorderPressed = Color(0xFF6F57AE),
            
            // Surface colors
            surface = Color(0xFF1C1B1F),
            surfaceVariant = Color(0xFF49454F),
            onSurface = Color(0xFFE6E1E5),
            onSurfaceVariant = Color(0xFFCAC4D0),
            
            // Accent colors
            primary = Color(0xFF7F67BE),
            primaryVariant = Color(0xFF6F57AE),
            secondary = Color(0xFFCAC4D0),
            secondaryVariant = Color(0xFF79747E),
            
            // Status colors
            error = Color(0xFFFFB4AB),
            warning = Color(0xFFFFCC02),
            success = Color(0xFF81C784),
            info = Color(0xFF64B5F6)
        )
    )
    
    /**
     * Monokai theme - popular dark theme for developers
     */
    val MonokaiDark = DesignTokens(
        colors = ColorTokens(
            // Background colors
            keyboardBackground = Color(0xFF272822),
            keyBackground = Color(0xFF3E3D32),
            keyBackgroundPressed = Color(0xFF49483E),
            keyBackgroundDisabled = Color(0xFF2C2C2C),
            
            // Action key colors
            actionKeyBackground = Color(0xFFF92672),
            actionKeyBackgroundPressed = Color(0xFFE91E63),
            actionKeyBackgroundDisabled = Color(0xFF424242),
            
            // Control key colors
            controlKeyBackground = Color(0xFFAE81FF),
            controlKeyBackgroundPressed = Color(0xFF9C6EFF),
            controlKeyBackgroundActive = Color(0xFFF92672),
            
            // Text colors
            keyText = Color(0xFFF8F8F2),
            keyTextSecondary = Color(0xFF75715E),
            keyTextDisabled = Color(0xFF616161),
            actionKeyText = Color(0xFFFFFFFF),
            controlKeyText = Color(0xFFFFFFFF),
            
            // Border colors
            keyBorder = Color(0xFF49483E),
            keyBorderFocused = Color(0xFFF92672),
            keyBorderPressed = Color(0xFFE91E63),
            
            // Surface colors
            surface = Color(0xFF272822),
            surfaceVariant = Color(0xFF3E3D32),
            onSurface = Color(0xFFF8F8F2),
            onSurfaceVariant = Color(0xFF75715E),
            
            // Accent colors
            primary = Color(0xFFF92672),
            primaryVariant = Color(0xFFE91E63),
            secondary = Color(0xFFAE81FF),
            secondaryVariant = Color(0xFF9C6EFF),
            
            // Status colors
            error = Color(0xFFF92672),
            warning = Color(0xFFF4BF75),
            success = Color(0xFFA6E22E),
            info = Color(0xFF66D9EF)
        )
    )
    
    /**
     * Solarized Dark theme - popular low-contrast theme
     */
    val SolarizedDark = DesignTokens(
        colors = ColorTokens(
            // Background colors
            keyboardBackground = Color(0xFF002B36),
            keyBackground = Color(0xFF073642),
            keyBackgroundPressed = Color(0xFF0F4C5C),
            keyBackgroundDisabled = Color(0xFF2C2C2C),
            
            // Action key colors
            actionKeyBackground = Color(0xFFDC322F),
            actionKeyBackgroundPressed = Color(0xFFCB4B16),
            actionKeyBackgroundDisabled = Color(0xFF424242),
            
            // Control key colors
            controlKeyBackground = Color(0xFF268BD2),
            controlKeyBackgroundPressed = Color(0xFF2AA198),
            controlKeyBackgroundActive = Color(0xFFDC322F),
            
            // Text colors
            keyText = Color(0xFFFDF6E3),
            keyTextSecondary = Color(0xFF93A1A1),
            keyTextDisabled = Color(0xFF616161),
            actionKeyText = Color(0xFFFFFFFF),
            controlKeyText = Color(0xFFFFFFFF),
            
            // Border colors
            keyBorder = Color(0xFF586E75),
            keyBorderFocused = Color(0xFFDC322F),
            keyBorderPressed = Color(0xFFCB4B16),
            
            // Surface colors
            surface = Color(0xFF002B36),
            surfaceVariant = Color(0xFF073642),
            onSurface = Color(0xFFFDF6E3),
            onSurfaceVariant = Color(0xFF93A1A1),
            
            // Accent colors
            primary = Color(0xFFDC322F),
            primaryVariant = Color(0xFFCB4B16),
            secondary = Color(0xFF268BD2),
            secondaryVariant = Color(0xFF2AA198),
            
            // Status colors
            error = Color(0xFFDC322F),
            warning = Color(0xFFB58900),
            success = Color(0xFF859900),
            info = Color(0xFF268BD2)
        )
    )
    
    /**
     * Solarized Light theme
     */
    val SolarizedLight = DesignTokens(
        colors = ColorTokens(
            // Background colors
            keyboardBackground = Color(0xFFFDF6E3),
            keyBackground = Color(0xFFEEE8D5),
            keyBackgroundPressed = Color(0xFFDDD6C1),
            keyBackgroundDisabled = Color(0xFFF5F5F5),
            
            // Action key colors
            actionKeyBackground = Color(0xFFDC322F),
            actionKeyBackgroundPressed = Color(0xFFCB4B16),
            actionKeyBackgroundDisabled = Color(0xFFBDBDBD),
            
            // Control key colors
            controlKeyBackground = Color(0xFF268BD2),
            controlKeyBackgroundPressed = Color(0xFF2AA198),
            controlKeyBackgroundActive = Color(0xFFDC322F),
            
            // Text colors
            keyText = Color(0xFF002B36),
            keyTextSecondary = Color(0xFF586E75),
            keyTextDisabled = Color(0xFF9E9E9E),
            actionKeyText = Color(0xFFFFFFFF),
            controlKeyText = Color(0xFFFFFFFF),
            
            // Border colors
            keyBorder = Color(0xFF93A1A1),
            keyBorderFocused = Color(0xFFDC322F),
            keyBorderPressed = Color(0xFFCB4B16),
            
            // Surface colors
            surface = Color(0xFFFDF6E3),
            surfaceVariant = Color(0xFFEEE8D5),
            onSurface = Color(0xFF002B36),
            onSurfaceVariant = Color(0xFF586E75),
            
            // Accent colors
            primary = Color(0xFFDC322F),
            primaryVariant = Color(0xFFCB4B16),
            secondary = Color(0xFF268BD2),
            secondaryVariant = Color(0xFF2AA198),
            
            // Status colors
            error = Color(0xFFDC322F),
            warning = Color(0xFFB58900),
            success = Color(0xFF859900),
            info = Color(0xFF268BD2)
        )
    )
    
    /**
     * Tokyo Night theme - popular modern dark theme
     */
    val TokyoNightDark = DesignTokens(
        colors = ColorTokens(
            // Background colors
            keyboardBackground = Color(0xFF1A1B26),
            keyBackground = Color(0xFF24283B),
            keyBackgroundPressed = Color(0xFF292E42),
            keyBackgroundDisabled = Color(0xFF2C2C2C),
            
            // Action key colors
            actionKeyBackground = Color(0xFFF7768E),
            actionKeyBackgroundPressed = Color(0xFFE06B83),
            actionKeyBackgroundDisabled = Color(0xFF424242),
            
            // Control key colors
            controlKeyBackground = Color(0xFF7AA2F7),
            controlKeyBackgroundPressed = Color(0xFF6B92E6),
            controlKeyBackgroundActive = Color(0xFFF7768E),
            
            // Text colors
            keyText = Color(0xFFC0CAF5),
            keyTextSecondary = Color(0xFF9AA5CE),
            keyTextDisabled = Color(0xFF616161),
            actionKeyText = Color(0xFFFFFFFF),
            controlKeyText = Color(0xFFFFFFFF),
            
            // Border colors
            keyBorder = Color(0xFF414868),
            keyBorderFocused = Color(0xFFF7768E),
            keyBorderPressed = Color(0xFFE06B83),
            
            // Surface colors
            surface = Color(0xFF1A1B26),
            surfaceVariant = Color(0xFF24283B),
            onSurface = Color(0xFFC0CAF5),
            onSurfaceVariant = Color(0xFF9AA5CE),
            
            // Accent colors
            primary = Color(0xFFF7768E),
            primaryVariant = Color(0xFFE06B83),
            secondary = Color(0xFF7AA2F7),
            secondaryVariant = Color(0xFF6B92E6),
            
            // Status colors
            error = Color(0xFFF7768E),
            warning = Color(0xFFE0AF68),
            success = Color(0xFF9ECE6A),
            info = Color(0xFF7DCFFF)
        )
    )
    
    /**
     * Get all available theme presets
     */
    fun getAllThemes(): Map<String, DesignTokens> = mapOf(
        "Default Light" to DefaultLight,
        "Default Dark" to DefaultDark,
        "Monokai Dark" to MonokaiDark,
        "Solarized Dark" to SolarizedDark,
        "Solarized Light" to SolarizedLight,
        "Tokyo Night Dark" to TokyoNightDark
    )
    
    /**
     * Get theme by name
     */
    fun getThemeByName(name: String): DesignTokens? = getAllThemes()[name]
    
    /**
     * Get all theme names
     */
    fun getThemeNames(): List<String> = getAllThemes().keys.toList()
}
