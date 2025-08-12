package com.onehandkeyboard.core.design.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Color tokens for the keyboard theme
 */
object KeyboardColors {
    // Primary colors
    val Primary = Color(0xFF6750A4)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFEADDFF)
    val OnPrimaryContainer = Color(0xFF21005D)

    // Surface colors
    val Surface = Color(0xFFFFFBFE)
    val OnSurface = Color(0xFF1C1B1F)
    val SurfaceVariant = Color(0xFFE7E0EC)
    val OnSurfaceVariant = Color(0xFF49454F)

    // Key colors
    val KeyBackground = Color(0xFFE7E0EC)
    val KeyBackgroundPressed = Color(0xFFD0C7D8)
    val KeyText = Color(0xFF1C1B1F)
    val KeyTextSecondary = Color(0xFF49454F)

    // Action key colors
    val ActionKeyBackground = Color(0xFF6750A4)
    val ActionKeyText = Color(0xFFFFFFFF)

    // Dark theme colors
    val SurfaceDark = Color(0xFF1C1B1F)
    val OnSurfaceDark = Color(0xFFE6E1E5)
    val SurfaceVariantDark = Color(0xFF49454F)
    val OnSurfaceVariantDark = Color(0xFFCAC4D0)

    val KeyBackgroundDark = Color(0xFF49454F)
    val KeyBackgroundPressedDark = Color(0xFF625B71)
    val KeyTextDark = Color(0xFFE6E1E5)
    val KeyTextSecondaryDark = Color(0xFFCAC4D0)

    val ActionKeyBackgroundDark = Color(0xFF7F67BE)
    val ActionKeyTextDark = Color(0xFFFFFFFF)
}

private val LightColorScheme =
    lightColorScheme(
        primary = KeyboardColors.Primary,
        onPrimary = KeyboardColors.OnPrimary,
        primaryContainer = KeyboardColors.PrimaryContainer,
        onPrimaryContainer = KeyboardColors.OnPrimaryContainer,
        surface = KeyboardColors.Surface,
        onSurface = KeyboardColors.OnSurface,
        surfaceVariant = KeyboardColors.SurfaceVariant,
        onSurfaceVariant = KeyboardColors.OnSurfaceVariant,
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = KeyboardColors.Primary,
        onPrimary = KeyboardColors.OnPrimary,
        primaryContainer = KeyboardColors.PrimaryContainer,
        onPrimaryContainer = KeyboardColors.OnPrimaryContainer,
        surface = KeyboardColors.SurfaceDark,
        onSurface = KeyboardColors.OnSurfaceDark,
        surfaceVariant = KeyboardColors.SurfaceVariantDark,
        onSurfaceVariant = KeyboardColors.OnSurfaceVariantDark,
    )

@Composable
fun KeyboardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KeyboardTypography,
        content = content,
    )
}
