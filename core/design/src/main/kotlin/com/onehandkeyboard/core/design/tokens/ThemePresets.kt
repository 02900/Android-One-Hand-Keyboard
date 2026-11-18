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
    val MonokaiDark =
        DesignTokens(
            colors =
                ColorTokens(
                    // Core 5 IME keyboard tokens
                    // Monokai background
                    backgroundRoot = Color(0xFF000000),
                    // Grid cells
                    backgroundPrimary = Color(0xFF2E2E2E),
                    // Action buttons/space bar
                    backgroundSecondary = Color(0xFFF92672),
                    // Text on grid cells
                    contentPrimary = Color(0xFFF8F8F2),
                    // Text on action buttons
                    contentSecondary = Color(0xFFFFFFFF),
                ),
        )

    /**
     * Solarized Dark theme - popular low-contrast theme
     */
    val SolarizedDark =
        DesignTokens(
            colors =
                ColorTokens(
                    // Core 5 IME keyboard tokens
                    // Solarized base03
                    backgroundRoot = Color(0xFF002B36),
                    // Grid cells
                    backgroundPrimary = Color(0xFF073642),
                    // Action buttons/space bar
                    backgroundSecondary = Color(0xFF268BD2),
                    // Text on grid cells
                    contentPrimary = Color(0xFFFDF6E3),
                    // Text on action buttons
                    contentSecondary = Color(0xFFFFFFFF),
                ),
        )

    /**
     * Dracula Dark theme - Popular vampire-inspired dark theme
     */
    val DraculaDark =
        DesignTokens(
            colors =
                ColorTokens(
                    // Core 5 IME keyboard tokens
                    // Dracula background
                    backgroundRoot = Color(0xFF282A36),
                    // Grid cells
                    backgroundPrimary = Color(0xFF44475A),
                    // Action buttons/space bar
                    backgroundSecondary = Color(0xFFBD93F9),
                    // Text on grid cells
                    contentPrimary = Color(0xFFF8F8F2),
                    // Text on action buttons
                    contentSecondary = Color(0xFF282A36),
                ),
        )

    /**
     * Gruvbox Dark theme - Retro groove color scheme
     */
    val GruvboxDark =
        DesignTokens(
            colors =
                ColorTokens(
                    // Core 5 IME keyboard tokens
                    // Gruvbox background
                    backgroundRoot = Color(0xFF282828),
                    // Grid cells
                    backgroundPrimary = Color(0xFF3C3836),
                    // Action buttons/space bar
                    backgroundSecondary = Color(0xFFD79921),
                    // Text on grid cells
                    contentPrimary = Color(0xFFEBDBB2),
                    // Text on action buttons
                    contentSecondary = Color(0xFF282828),
                ),
        )

    /**
     * Gruvbox Light theme - Retro groove color scheme (light variant)
     */
    val GruvboxLight =
        DesignTokens(
            colors =
                ColorTokens(
                    // Core 5 IME keyboard tokens
                    // Gruvbox light background
                    backgroundRoot = Color(0xFFFBF1C7),
                    // Grid cells
                    backgroundPrimary = Color(0xFFEBDBB2),
                    // Action buttons/space bar
                    backgroundSecondary = Color(0xFFB57614),
                    // Text on grid cells
                    contentPrimary = Color(0xFF3C3836),
                    // Text on action buttons
                    contentSecondary = Color(0xFFFBF1C7),
                ),
        )

    /**
     * Tokyo Night theme - Modern dark theme with purple accents
     */
    val TokyoNight =
        DesignTokens(
            colors =
                ColorTokens(
                    // Core 5 IME keyboard tokens
                    // Tokyo Night background
                    backgroundRoot = Color(0xFF1A1B26),
                    // Grid cells
                    backgroundPrimary = Color(0xFF24283B),
                    // Action buttons/space bar
                    backgroundSecondary = Color(0xFF7AA2F7),
                    // Text on grid cells
                    contentPrimary = Color(0xFFC0CAF5),
                    // Text on action buttons
                    contentSecondary = Color(0xFF1A1B26),
                ),
        )

    /**
     * Ocean Blue theme - Cool blue theme
     */
    val OceanBlue =
        DesignTokens(
            colors =
                ColorTokens(
                    // Core 5 IME keyboard tokens
                    // Deep ocean background
                    backgroundRoot = Color(0xFF0F172A),
                    // Grid cells
                    backgroundPrimary = Color(0xFF1E293B),
                    // Action buttons/space bar
                    backgroundSecondary = Color(0xFF0EA5E9),
                    // Text on grid cells
                    contentPrimary = Color(0xFFE2E8F0),
                    // Text on action buttons
                    contentSecondary = Color(0xFF0F172A),
                ),
        )

    /**
     * Forest Green theme - Nature-inspired green theme
     */
    val ForestGreen =
        DesignTokens(
            colors =
                ColorTokens(
                    // Core 5 IME keyboard tokens
                    // Dark forest background
                    backgroundRoot = Color(0xFF0F1419),
                    // Grid cells
                    backgroundPrimary = Color(0xFF1B2B1B),
                    // Action buttons/space bar
                    backgroundSecondary = Color(0xFF22C55E),
                    // Text on grid cells
                    contentPrimary = Color(0xFFD1FAE5),
                    // Text on action buttons
                    contentSecondary = Color(0xFF0F1419),
                ),
        )

    /**
     * Get all available theme presets
     */
    fun getAllThemes(): Map<String, DesignTokens> =
        mapOf(
            "Monokai Dark" to MonokaiDark,
            "Solarized Dark" to SolarizedDark,
            "Dracula Dark" to DraculaDark,
            "Gruvbox Dark" to GruvboxDark,
            "Gruvbox Light" to GruvboxLight,
            "Tokyo Night" to TokyoNight,
            "Ocean Blue" to OceanBlue,
            "Forest Green" to ForestGreen,
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
