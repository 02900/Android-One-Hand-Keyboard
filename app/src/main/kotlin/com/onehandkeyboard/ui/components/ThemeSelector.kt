package com.onehandkeyboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    modifier: Modifier = Modifier
) {
    val currentThemeName by themeManager.currentThemeName.collectAsState()
    val availableThemes = themeManager.getAvailableThemes()

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "🎨 Theme Selection",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(availableThemes) { themeName ->
                val themePreview = themeManager.getThemePreview(themeName)
                if (themePreview != null) {
                    ThemePreviewCard(
                        themePreview = themePreview,
                        isSelected = themeName == currentThemeName,
                        onThemeSelected = { themeManager.setTheme(themeName) }
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeSelectorContent(
    themeManager: ThemeManager,
    modifier: Modifier = Modifier
) {
    val currentThemeName by themeManager.currentThemeName.collectAsState()
    val availableThemes = themeManager.getAvailableThemes()

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "🎨 Theme Selection",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Display themes as individual cards without LazyColumn
        availableThemes.forEach { themeName ->
            val themePreview = themeManager.getThemePreview(themeName)
            if (themePreview != null) {
                ThemePreviewCard(
                    themePreview = themePreview,
                    isSelected = themeName == currentThemeName,
                    onThemeSelected = { themeManager.setTheme(themeName) },
                    modifier = Modifier.padding(bottom = 12.dp)
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onThemeSelected() }
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Theme preview mini keyboard
            ThemePreviewMiniKeyboard(
                themePreview = themePreview,
                modifier = Modifier.size(80.dp, 60.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Theme info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = themePreview.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = getThemeDescription(themePreview.name),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // Selection indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
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
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = themePreview.keyboardBackground,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(6.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // First row of keys
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                repeat(3) {
                    MiniKey(
                        backgroundColor = themePreview.keyBackground,
                        textColor = themePreview.keyText,
                        modifier = Modifier.size(18.dp, 14.dp)
                    )
                }
            }
            
            // Second row with action key
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                repeat(2) {
                    MiniKey(
                        backgroundColor = themePreview.keyBackground,
                        textColor = themePreview.keyText,
                        modifier = Modifier.size(18.dp, 14.dp)
                    )
                }
                MiniKey(
                    backgroundColor = themePreview.actionKeyBackground,
                    textColor = themePreview.actionKeyText,
                    modifier = Modifier.size(18.dp, 14.dp)
                )
            }
            
            // Spacebar
            MiniKey(
                backgroundColor = themePreview.actionKeyBackground,
                textColor = themePreview.actionKeyText,
                modifier = Modifier.fillMaxWidth().height(10.dp)
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
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(2.dp)
            )
    )
}



/**
 * Get theme description for display
 */
private fun getThemeDescription(themeName: String): String = when {
    themeName.contains("Default") -> "System default theme"
    themeName.contains("Monokai") -> "Popular developer theme"
    themeName.contains("Solarized") -> "Low-contrast, eye-friendly"
    themeName.contains("Tokyo") -> "Modern, elegant theme"
    else -> "Custom theme"
}

@Preview(showBackground = true)
@Composable
private fun ThemeSelectorPreview() {
    MaterialTheme {
        Surface {
            // Preview with mock data - won't work without ThemeManager injection
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Theme Selector Preview")
                Text("(Requires ThemeManager injection to function)")
            }
        }
    }
}
