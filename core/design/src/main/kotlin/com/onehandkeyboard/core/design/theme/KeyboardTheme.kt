package com.onehandkeyboard.core.design.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.onehandkeyboard.core.design.tokens.DesignTokens
import com.onehandkeyboard.core.design.tokens.ThemePresets


/**
 * Modern keyboard theme using design tokens
 * Integrates with ThemeManager for dynamic theme switching
 */
@Composable
fun KeyboardThemeWithTokens(
    themeManager: ThemeManager? = null,
    fallbackDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val designTokens = if (themeManager != null) {
        val currentTheme by themeManager.currentTheme.collectAsState()
        currentTheme
    } else {
        // Fallback to default themes when ThemeManager is not available
        if (fallbackDarkTheme) ThemePresets.MonokaiDark else ThemePresets.GruvboxLight
    }
    
    val colorScheme = createColorSchemeFromTokens(designTokens)
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = KeyboardTypography,
        content = content,
    )
}

/**
 * Create Material3 ColorScheme from design tokens
 */
private fun createColorSchemeFromTokens(tokens: DesignTokens) = lightColorScheme(
    primary = tokens.colors.backgroundSecondary,
    onPrimary = tokens.colors.contentSecondary,
    primaryContainer = tokens.colors.backgroundSecondary,
    onPrimaryContainer = tokens.colors.contentPrimary,
    secondary = tokens.colors.backgroundPrimary,
    onSecondary = tokens.colors.contentSecondary,
    secondaryContainer = tokens.colors.backgroundPrimary,
    onSecondaryContainer = tokens.colors.contentPrimary,
    surface = tokens.colors.backgroundRoot,
    onSurface = tokens.colors.contentPrimary,
    surfaceVariant = tokens.colors.backgroundPrimary,
    onSurfaceVariant = tokens.colors.contentPrimary,
    error = Color.Red,
    onError = Color.White,
    background = tokens.colors.backgroundRoot,
    onBackground = tokens.colors.contentPrimary
)
