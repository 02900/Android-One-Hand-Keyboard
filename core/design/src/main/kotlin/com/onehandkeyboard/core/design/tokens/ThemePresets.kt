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
     * Dracula Dark theme - Popular vampire-inspired dark theme
     */
    val DraculaDark = DesignTokens(
        colors = ColorTokens(
            // Background colors
            keyboardBackground = Color(0xFF282A36),
            keyBackground = Color(0xFF44475A),
            keyBackgroundPressed = Color(0xFF6272A4),
            keyBackgroundDisabled = Color(0xFF383A59),
            
            // Action key colors
            actionKeyBackground = Color(0xFF50FA7B),
            actionKeyBackgroundPressed = Color(0xFF69FF94),
            actionKeyBackgroundDisabled = Color(0xFF3D7C47),
            
            // Control key colors
            controlKeyBackground = Color(0xFFBD93F9),
            controlKeyBackgroundPressed = Color(0xFFD4AFFF),
            controlKeyBackgroundActive = Color(0xFF9580FF),
            
            // Text colors
            keyText = Color(0xFFF8F8F2),
            keyTextSecondary = Color(0xFF6272A4),
            keyTextDisabled = Color(0xFF44475A),
            actionKeyText = Color(0xFF282A36),
            controlKeyText = Color(0xFF282A36),
            
            // Border and outline colors
            keyBorder = Color(0xFF6272A4),
            keyBorderFocused = Color(0xFFBD93F9),
            keyBorderPressed = Color(0xFF50FA7B),
            
            // Surface colors
            surface = Color(0xFF44475A),
            surfaceVariant = Color(0xFF383A59),
            onSurface = Color(0xFFF8F8F2),
            onSurfaceVariant = Color(0xFF6272A4),
            
            // Accent colors
            primary = Color(0xFFBD93F9),
            primaryVariant = Color(0xFF9580FF),
            secondary = Color(0xFF50FA7B),
            secondaryVariant = Color(0xFF69FF94),
            
            // Status colors
            error = Color(0xFFFF5555),
            warning = Color(0xFFFFB86C),
            success = Color(0xFF50FA7B),
            info = Color(0xFF8BE9FD)
        )
    )
    
    /**
     * Gruvbox Dark theme - Retro groove color scheme
     */
    val GruvboxDark = DesignTokens(
        colors = ColorTokens(
            // Background colors
            keyboardBackground = Color(0xFF282828),
            keyBackground = Color(0xFF3C3836),
            keyBackgroundPressed = Color(0xFF504945),
            keyBackgroundDisabled = Color(0xFF32302F),
            
            // Action key colors
            actionKeyBackground = Color(0xFFB8BB26),
            actionKeyBackgroundPressed = Color(0xFFD3D52A),
            actionKeyBackgroundDisabled = Color(0xFF79740E),
            
            // Control key colors
            controlKeyBackground = Color(0xFFD3869B),
            controlKeyBackgroundPressed = Color(0xFFE8A2B8),
            controlKeyBackgroundActive = Color(0xFFB16286),
            
            // Text colors
            keyText = Color(0xFFEBDBB2),
            keyTextSecondary = Color(0xFFA89984),
            keyTextDisabled = Color(0xFF665C54),
            actionKeyText = Color(0xFF282828),
            controlKeyText = Color(0xFF282828),
            
            // Border and outline colors
            keyBorder = Color(0xFF504945),
            keyBorderFocused = Color(0xFFD3869B),
            keyBorderPressed = Color(0xFFB8BB26),
            
            // Surface colors
            surface = Color(0xFF3C3836),
            surfaceVariant = Color(0xFF32302F),
            onSurface = Color(0xFFEBDBB2),
            onSurfaceVariant = Color(0xFFA89984),
            
            // Accent colors
            primary = Color(0xFFD3869B),
            primaryVariant = Color(0xFFB16286),
            secondary = Color(0xFFB8BB26),
            secondaryVariant = Color(0xFF98971A),
            
            // Status colors
            error = Color(0xFFCC241D),
            warning = Color(0xFFD79921),
            success = Color(0xFFB8BB26),
            info = Color(0xFF458588)
        )
    )
    
    /**
     * Gruvbox Light theme - Retro groove color scheme (light variant)
     */
    val GruvboxLight = DesignTokens(
        colors = ColorTokens(
            // Background colors
            keyboardBackground = Color(0xFFFBF1C7),
            keyBackground = Color(0xFFEBDBB2),
            keyBackgroundPressed = Color(0xFFD5C4A1),
            keyBackgroundDisabled = Color(0xFFF2E5BC),
            
            // Action key colors
            actionKeyBackground = Color(0xFF98971A),
            actionKeyBackgroundPressed = Color(0xFFB8BB26),
            actionKeyBackgroundDisabled = Color(0xFF79740E),
            
            // Control key colors
            controlKeyBackground = Color(0xFFB16286),
            controlKeyBackgroundPressed = Color(0xFFD3869B),
            controlKeyBackgroundActive = Color(0xFF8F3F71),
            
            // Text colors
            keyText = Color(0xFF3C3836),
            keyTextSecondary = Color(0xFF665C54),
            keyTextDisabled = Color(0xFFA89984),
            actionKeyText = Color(0xFFFBF1C7),
            controlKeyText = Color(0xFFFBF1C7),
            
            // Border and outline colors
            keyBorder = Color(0xFFBDAE93),
            keyBorderFocused = Color(0xFFB16286),
            keyBorderPressed = Color(0xFF98971A),
            
            // Surface colors
            surface = Color(0xFFEBDBB2),
            surfaceVariant = Color(0xFFF2E5BC),
            onSurface = Color(0xFF3C3836),
            onSurfaceVariant = Color(0xFF665C54),
            
            // Accent colors
            primary = Color(0xFFB16286),
            primaryVariant = Color(0xFF8F3F71),
            secondary = Color(0xFF98971A),
            secondaryVariant = Color(0xFF79740E),
            
            // Status colors
            error = Color(0xFFCC241D),
            warning = Color(0xFFD79921),
            success = Color(0xFF98971A),
            info = Color(0xFF458588)
        )
    )
    
    /**
     * One Dark theme - Atom's iconic dark theme
     */
    val OneDark = DesignTokens(
        colors = ColorTokens(
            // Background colors
            keyboardBackground = Color(0xFF282C34),
            keyBackground = Color(0xFF3E4451),
            keyBackgroundPressed = Color(0xFF4F5666),
            keyBackgroundDisabled = Color(0xFF353B45),
            
            // Action key colors
            actionKeyBackground = Color(0xFF98C379),
            actionKeyBackgroundPressed = Color(0xFFABD192),
            actionKeyBackgroundDisabled = Color(0xFF6B8F4F),
            
            // Control key colors
            controlKeyBackground = Color(0xFF61AFEF),
            controlKeyBackgroundPressed = Color(0xFF7BC3F2),
            controlKeyBackgroundActive = Color(0xFF4A9BDC),
            
            // Text colors
            keyText = Color(0xFFABB2BF),
            keyTextSecondary = Color(0xFF5C6370),
            keyTextDisabled = Color(0xFF3E4451),
            actionKeyText = Color(0xFF282C34),
            controlKeyText = Color(0xFF282C34),
            
            // Border and outline colors
            keyBorder = Color(0xFF4F5666),
            keyBorderFocused = Color(0xFF61AFEF),
            keyBorderPressed = Color(0xFF98C379),
            
            // Surface colors
            surface = Color(0xFF3E4451),
            surfaceVariant = Color(0xFF353B45),
            onSurface = Color(0xFFABB2BF),
            onSurfaceVariant = Color(0xFF5C6370),
            
            // Accent colors
            primary = Color(0xFF61AFEF),
            primaryVariant = Color(0xFF4A9BDC),
            secondary = Color(0xFF98C379),
            secondaryVariant = Color(0xFF7FB069),
            
            // Status colors
            error = Color(0xFFE06C75),
            warning = Color(0xFFE5C07B),
            success = Color(0xFF98C379),
            info = Color(0xFF56B6C2)
        )
    )
    
    /**
     * Nord Dark theme - Arctic, north-bluish color palette
     */
    val NordDark = DesignTokens(
        colors = ColorTokens(
            // Background colors
            keyboardBackground = Color(0xFF2E3440),
            keyBackground = Color(0xFF3B4252),
            keyBackgroundPressed = Color(0xFF434C5E),
            keyBackgroundDisabled = Color(0xFF373E4D),
            
            // Action key colors
            actionKeyBackground = Color(0xFFA3BE8C),
            actionKeyBackgroundPressed = Color(0xFFB8D4A0),
            actionKeyBackgroundDisabled = Color(0xFF7A9B69),
            
            // Control key colors
            controlKeyBackground = Color(0xFF88C0D0),
            controlKeyBackgroundPressed = Color(0xFF9FCEE3),
            controlKeyBackgroundActive = Color(0xFF6FB3BD),
            
            // Text colors
            keyText = Color(0xFFECEFF4),
            keyTextSecondary = Color(0xFFD8DEE9),
            keyTextDisabled = Color(0xFF4C566A),
            actionKeyText = Color(0xFF2E3440),
            controlKeyText = Color(0xFF2E3440),
            
            // Border and outline colors
            keyBorder = Color(0xFF4C566A),
            keyBorderFocused = Color(0xFF88C0D0),
            keyBorderPressed = Color(0xFFA3BE8C),
            
            // Surface colors
            surface = Color(0xFF3B4252),
            surfaceVariant = Color(0xFF373E4D),
            onSurface = Color(0xFFECEFF4),
            onSurfaceVariant = Color(0xFFD8DEE9),
            
            // Accent colors
            primary = Color(0xFF88C0D0),
            primaryVariant = Color(0xFF6FB3BD),
            secondary = Color(0xFFA3BE8C),
            secondaryVariant = Color(0xFF8FBCBB),
            
            // Status colors
            error = Color(0xFFBF616A),
            warning = Color(0xFFEBCB8B),
            success = Color(0xFFA3BE8C),
            info = Color(0xFF81A1C1)
        )
    )
    
    /**
     * Catppuccin Mocha theme - Soothing pastel theme for the high-spirited
     */
    val CatppuccinMocha = DesignTokens(
        colors = ColorTokens(
            // Background colors
            keyboardBackground = Color(0xFF1E1E2E),
            keyBackground = Color(0xFF313244),
            keyBackgroundPressed = Color(0xFF45475A),
            keyBackgroundDisabled = Color(0xFF2A2A3A),
            
            // Action key colors
            actionKeyBackground = Color(0xFFA6E3A1),
            actionKeyBackgroundPressed = Color(0xFFB8E8B5),
            actionKeyBackgroundDisabled = Color(0xFF7FB078),
            
            // Control key colors
            controlKeyBackground = Color(0xFFCBA6F7),
            controlKeyBackgroundPressed = Color(0xFFD9BFFF),
            controlKeyBackgroundActive = Color(0xFFB794F6),
            
            // Text colors
            keyText = Color(0xFFCDD6F4),
            keyTextSecondary = Color(0xFF9399B2),
            keyTextDisabled = Color(0xFF585B70),
            actionKeyText = Color(0xFF1E1E2E),
            controlKeyText = Color(0xFF1E1E2E),
            
            // Border and outline colors
            keyBorder = Color(0xFF45475A),
            keyBorderFocused = Color(0xFFCBA6F7),
            keyBorderPressed = Color(0xFFA6E3A1),
            
            // Surface colors
            surface = Color(0xFF313244),
            surfaceVariant = Color(0xFF2A2A3A),
            onSurface = Color(0xFFCDD6F4),
            onSurfaceVariant = Color(0xFF9399B2),
            
            // Accent colors
            primary = Color(0xFFCBA6F7),
            primaryVariant = Color(0xFFB794F6),
            secondary = Color(0xFFA6E3A1),
            secondaryVariant = Color(0xFF94E2D5),
            
            // Status colors
            error = Color(0xFFF38BA8),
            warning = Color(0xFFFAB387),
            success = Color(0xFFA6E3A1),
            info = Color(0xFF89B4FA)
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
        "Tokyo Night Dark" to TokyoNightDark,
        "Dracula Dark" to DraculaDark,
        "Gruvbox Dark" to GruvboxDark,
        "Gruvbox Light" to GruvboxLight,
        "One Dark" to OneDark,
        "Nord Dark" to NordDark,
        "Catppuccin Mocha" to CatppuccinMocha
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
