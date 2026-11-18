package com.onehandkeyboard.core.design.tokens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Design tokens system for keyboard theming: a structured approach to visual design with
// semantic naming.

/**
 * Simplified color tokens for IME keyboard theming
 * Uses only 5 essential tokens for maximum simplicity and consistency
 */
data class ColorTokens(
    // Core IME keyboard tokens
    // Background color of the entire IME keyboard (default black)
    val backgroundRoot: Color,
    // Background for grid cells (main keys)
    val backgroundPrimary: Color,
    // Background for action buttons and space bar
    val backgroundSecondary: Color,
    // Text color applied over backgroundPrimary
    val contentPrimary: Color,
    // Text color applied over backgroundSecondary
    val contentSecondary: Color,
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
    val xxxl: Dp = 32.dp,
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
    val round: Dp = 50.dp,
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
    val xxl: Dp = 12.dp,
)

/**
 * Complete design token set
 */
data class DesignTokens(
    val colors: ColorTokens,
    val spacing: SpacingTokens = SpacingTokens(),
    val borderRadius: BorderRadiusTokens = BorderRadiusTokens(),
    val elevation: ElevationTokens = ElevationTokens(),
)
