package com.onehandkeyboard.core.design.tokens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design tokens system for keyboard theming
 * Provides a structured approach to visual design with semantic naming
 */

/**
 * Color tokens for keyboard components
 */
data class ColorTokens(
    // Background colors
    val keyboardBackground: Color,
    val keyBackground: Color,
    val keyBackgroundPressed: Color,
    val keyBackgroundDisabled: Color,
    
    // Action key colors (space, backspace, enter, etc.)
    val actionKeyBackground: Color,
    val actionKeyBackgroundPressed: Color,
    val actionKeyBackgroundDisabled: Color,
    
    // Control key colors (ABC/123, Aa, etc.)
    val controlKeyBackground: Color,
    val controlKeyBackgroundPressed: Color,
    val controlKeyBackgroundActive: Color,
    
    // Text colors
    val keyText: Color,
    val keyTextSecondary: Color,
    val keyTextDisabled: Color,
    val actionKeyText: Color,
    val controlKeyText: Color,
    
    // Border and outline colors
    val keyBorder: Color,
    val keyBorderFocused: Color,
    val keyBorderPressed: Color,
    
    // Surface colors
    val surface: Color,
    val surfaceVariant: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    
    // Accent colors
    val primary: Color,
    val primaryVariant: Color,
    val secondary: Color,
    val secondaryVariant: Color,
    
    // Status colors
    val error: Color,
    val warning: Color,
    val success: Color,
    val info: Color
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
