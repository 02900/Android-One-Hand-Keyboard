package com.onehandkeyboard.core.design.tokens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design tokens system for keyboard theming
 * Provides a structured approach to visual design with semantic naming
 */

/**
 * Simplified color tokens for IME keyboard theming
 * Uses only 5 essential tokens for maximum simplicity and consistency
 */
data class ColorTokens(
    // Core IME keyboard tokens
    val backgroundRoot: Color,        // Background color of the entire IME keyboard (default black)
    val backgroundPrimary: Color,     // Background for grid cells (main keys)
    val backgroundSecondary: Color,   // Background for action buttons and space bar
    val contentPrimary: Color,        // Text color applied over backgroundPrimary
    val contentSecondary: Color,      // Text color applied over backgroundSecondary
    
    // Legacy tokens for configuration app compatibility (can use hardcoded colors if needed)
    val keyboardBackground: Color = backgroundRoot,
    val keyBackground: Color = backgroundPrimary,
    val keyBackgroundPressed: Color = backgroundPrimary,
    val keyBackgroundDisabled: Color = backgroundPrimary,
    val actionKeyBackground: Color = backgroundSecondary,
    val actionKeyBackgroundPressed: Color = backgroundSecondary,
    val actionKeyBackgroundDisabled: Color = backgroundSecondary,
    val controlKeyBackground: Color = backgroundSecondary,
    val controlKeyBackgroundPressed: Color = backgroundSecondary,
    val controlKeyBackgroundActive: Color = backgroundSecondary,
    val keyText: Color = contentPrimary,
    val keyTextSecondary: Color = contentPrimary,
    val keyTextDisabled: Color = contentPrimary,
    val actionKeyText: Color = contentSecondary,
    val controlKeyText: Color = contentSecondary,
    val keyBorder: Color = backgroundPrimary,
    val keyBorderFocused: Color = backgroundSecondary,
    val keyBorderPressed: Color = backgroundSecondary,
    val surface: Color = backgroundRoot,
    val surfaceVariant: Color = backgroundPrimary,
    val onSurface: Color = contentPrimary,
    val onSurfaceVariant: Color = contentPrimary,
    val primary: Color = backgroundSecondary,
    val primaryVariant: Color = backgroundSecondary,
    val secondary: Color = backgroundPrimary,
    val secondaryVariant: Color = backgroundPrimary,
    val error: Color = Color.Red,
    val warning: Color = Color.Yellow,
    val success: Color = Color.Green,
    val info: Color = Color.Blue
)

/**
 * Spacing tokens for consistent layout
 */
data class SpacingTokens(
    val none: Dp = 0.dp,
    val xs: Dp = 2.dp,
    val sm: Dp = 4.dp,
    val md: Dp = 8.dp,
    val lg: Dp = 12.dp,
    val xl: Dp = 16.dp,
    val xxl: Dp = 24.dp,
    val xxxl: Dp = 32.dp
)

/**
 * Border radius tokens for consistent corner styling
 */
data class BorderRadiusTokens(
    val none: Dp = 0.dp,
    val sm: Dp = 4.dp,
    val md: Dp = 8.dp,
    val lg: Dp = 12.dp,
    val xl: Dp = 16.dp,
    val round: Dp = 50.dp
)

/**
 * Elevation tokens for depth and shadow
 */
data class ElevationTokens(
    val none: Dp = 0.dp,
    val sm: Dp = 1.dp,
    val md: Dp = 3.dp,
    val lg: Dp = 6.dp,
    val xl: Dp = 8.dp,
    val xxl: Dp = 12.dp
)

/**
 * Complete design token set
 */
data class DesignTokens(
    val colors: ColorTokens,
    val spacing: SpacingTokens = SpacingTokens(),
    val borderRadius: BorderRadiusTokens = BorderRadiusTokens(),
    val elevation: ElevationTokens = ElevationTokens()
)
