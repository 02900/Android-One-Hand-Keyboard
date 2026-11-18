package com.onehandkeyboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onehandkeyboard.core.design.theme.ThemeManager
import com.onehandkeyboard.core.design.theme.ThemePreview

/**
 * Theme selector component for choosing keyboard themes
 */
@Composable
fun ThemeSelector(
    themeManager: ThemeManager,
    modifier: Modifier = Modifier,
) {
    val currentThemeName by themeManager.currentThemeName.collectAsState()
    val isColorsInverted by themeManager.isColorsInverted.collectAsState()
    val availableThemes = themeManager.getAvailableThemes()

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = "🎨 Theme Selection",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        // Color inversion toggle
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "🔄 Invertir Colores de Fondo",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text =
                            "Intercambia los colores entre el grid y la columna de acciones " +
                                "para máximo contraste AAA",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                Switch(
                    checked = isColorsInverted,
                    onCheckedChange = { themeManager.toggleColorInversion() },
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(availableThemes) { themeName ->
                val themePreview = themeManager.getThemePreview(themeName)
                if (themePreview != null) {
                    ThemePreviewCard(
                        themePreview = themePreview,
                        isSelected = themeName == currentThemeName,
                        onThemeSelected = { themeManager.setTheme(themeName) },
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeSelectorContent(
    themeManager: ThemeManager,
    modifier: Modifier = Modifier,
) {
    val currentThemeName by themeManager.currentThemeName.collectAsState()
    val availableThemes = themeManager.getAvailableThemes()

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = "🎨 Theme Selection",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        // Display themes as individual cards without LazyColumn
        availableThemes.forEach { themeName ->
            val themePreview = themeManager.getThemePreview(themeName)
            if (themePreview != null) {
                ThemePreviewCard(
                    themePreview = themePreview,
                    isSelected = themeName == currentThemeName,
                    onThemeSelected = { themeManager.setTheme(themeName) },
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
        }
    }
}

/**
 * Individual theme preview card
 */
@Composable
private fun ThemePreviewCard(
    themePreview: ThemePreview,
    isSelected: Boolean,
    onThemeSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onThemeSelected() }
                .then(
                    if (isSelected) {
                        Modifier.border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp),
                        )
                    } else {
                        Modifier
                    },
                ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Theme preview mini keyboard
            ThemePreviewMiniKeyboard(
                themePreview = themePreview,
                modifier = Modifier.size(80.dp, 60.dp),
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Theme info
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = themePreview.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                )

                Text(
                    text = getThemeDescription(themePreview.name),
                    fontSize = 12.sp,
                    color = Color.Black.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // Selection indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

/**
 * Mini keyboard preview showing theme colors
 */
@Composable
private fun ThemePreviewMiniKeyboard(
    themePreview: ThemePreview,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .background(
                    color = themePreview.keyboardBackground,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(6.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            // First row of keys
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                repeat(3) {
                    MiniKey(
                        backgroundColor = themePreview.keyBackground,
                        textColor = themePreview.keyText,
                        modifier = Modifier.size(18.dp, 14.dp),
                    )
                }
            }

            // Second row with action key
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                repeat(2) {
                    MiniKey(
                        backgroundColor = themePreview.keyBackground,
                        textColor = themePreview.keyText,
                        modifier = Modifier.size(18.dp, 14.dp),
                    )
                }
                MiniKey(
                    backgroundColor = themePreview.actionKeyBackground,
                    textColor = themePreview.actionKeyText,
                    modifier = Modifier.size(18.dp, 14.dp),
                )
            }

            // Spacebar
            MiniKey(
                backgroundColor = themePreview.actionKeyBackground,
                textColor = themePreview.actionKeyText,
                modifier = Modifier.fillMaxWidth().height(10.dp),
            )
        }
    }
}

/**
 * Individual mini key for preview
 */
@Composable
private fun MiniKey(
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(2.dp),
                ),
    )
}

/**
 * Get theme description for display
 */
private fun getThemeDescription(themeName: String): String =
    when {
        themeName.contains("Default") -> "System default theme"
        themeName.contains("Monokai") -> "Popular developer theme"
        themeName.contains("Solarized") -> "Low-contrast, eye-friendly"
        themeName.contains("Tokyo") -> "Modern, elegant theme"
        themeName.contains("Dracula") -> "Vampire theme"
        themeName.contains("Gruvbox") -> "Retro groove"
        else -> "Custom theme"
    }

/**
 * Calculate contrasting text color based on background luminance
 * Uses simplified luminance calculation for better contrast
 */
private fun getContrastingTextColor(backgroundColor: Color): Color {
    // Simplified luminance calculation using perceived brightness
    val r = backgroundColor.red
    val g = backgroundColor.green
    val b = backgroundColor.blue

    // Calculate perceived brightness (0.299*R + 0.587*G + 0.114*B)
    val brightness = (0.299f * r + 0.587f * g + 0.114f * b)

    // Return white for dark backgrounds, black for light backgrounds
    return if (brightness > 0.5f) Color.Black else Color.White
}

@Preview(showBackground = true)
@Composable
private fun ThemeSelectorPreview() {
    MaterialTheme {
        Surface {
            // Preview with mock data - won't work without ThemeManager injection
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Text("Theme Selector Preview")
                Text("(Requires ThemeManager injection to function)")
            }
        }
    }
}
