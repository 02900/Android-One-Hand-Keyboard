package com.onehandkeyboard.core.design.tokens

import androidx.compose.ui.graphics.Color

/**
 * Popular theme presets for keyboard styling
 * Includes popular developer themes like Monokai, Solarized, Tokyo Night, etc.
 */
object ThemePresets {
    /**
     * Monokai theme - popular dark theme for developers
     */
    val MonokaiDark = DesignTokens(
        colors = ColorTokens(
            // Core 5 IME keyboard tokens
            backgroundRoot = Color(0xFF000000),       // Monokai background
            backgroundPrimary = Color(0xFF000000),    // Grid cells
            backgroundSecondary = Color(0xFFF92672),  // Action buttons/space bar
            contentPrimary = Color(0xFFF8F8F2),       // Text on grid cells
            contentSecondary = Color(0xFFFFFFFF)      // Text on action buttons
        )
    )
    
    /**
     * Solarized Dark theme - popular low-contrast theme
     */
    val SolarizedDark = DesignTokens(
        colors = ColorTokens(
            // Core 5 IME keyboard tokens
            backgroundRoot = Color(0xFF002B36),       // Solarized base03
            backgroundPrimary = Color(0xFF073642),    // Grid cells
            backgroundSecondary = Color(0xFF268BD2),  // Action buttons/space bar
            contentPrimary = Color(0xFFFDF6E3),       // Text on grid cells
            contentSecondary = Color(0xFFFFFFFF)      // Text on action buttons
        )
    )
    
    /**
     * Dracula Dark theme - Popular vampire-inspired dark theme
     */
    val DraculaDark = DesignTokens(
        colors = ColorTokens(
            // Core 5 IME keyboard tokens
            backgroundRoot = Color(0xFF282A36),       // Dracula background
            backgroundPrimary = Color(0xFF44475A),    // Grid cells
            backgroundSecondary = Color(0xFFBD93F9),  // Action buttons/space bar
            contentPrimary = Color(0xFFF8F8F2),       // Text on grid cells
            contentSecondary = Color(0xFF282A36)      // Text on action buttons
        )
    )
    
    /**
     * Gruvbox Dark theme - Retro groove color scheme
     */
    val GruvboxDark = DesignTokens(
        colors = ColorTokens(
            // Core 5 IME keyboard tokens
            backgroundRoot = Color(0xFF282828),       // Gruvbox background
            backgroundPrimary = Color(0xFF3C3836),    // Grid cells
            backgroundSecondary = Color(0xFFD79921),  // Action buttons/space bar
            contentPrimary = Color(0xFFEBDBB2),       // Text on grid cells
            contentSecondary = Color(0xFF282828)      // Text on action buttons
        )
    )
    
    /**
     * Gruvbox Light theme - Retro groove color scheme (light variant)
     */
    val GruvboxLight = DesignTokens(
        colors = ColorTokens(
            // Core 5 IME keyboard tokens
            backgroundRoot = Color(0xFFFBF1C7),       // Gruvbox light background
            backgroundPrimary = Color(0xFFEBDBB2),    // Grid cells
            backgroundSecondary = Color(0xFFB57614),  // Action buttons/space bar
            contentPrimary = Color(0xFF3C3836),       // Text on grid cells
            contentSecondary = Color(0xFFFBF1C7)      // Text on action buttons
        )
    )

    
    /**
     * Get all available theme presets
     */
    fun getAllThemes(): Map<String, DesignTokens> = mapOf(
        "Monokai Dark" to MonokaiDark,
        "Solarized Dark" to SolarizedDark,
        "Dracula Dark" to DraculaDark,
        "Gruvbox Dark" to GruvboxDark,
        "Gruvbox Light" to GruvboxLight,
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
