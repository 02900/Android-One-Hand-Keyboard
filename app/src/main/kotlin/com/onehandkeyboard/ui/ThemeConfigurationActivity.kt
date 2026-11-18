package com.onehandkeyboard.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onehandkeyboard.core.design.theme.ThemeManager
import com.onehandkeyboard.core.design.tokens.ThemePresets
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Independent theme configuration activity
 * Provides a dedicated interface for theme selection with differentiated cell colors
 */
@AndroidEntryPoint
class ThemeConfigurationActivity : ComponentActivity() {
    @Inject
    lateinit var themeManager: ThemeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeConfigurationScreen(
                themeManager = themeManager,
                onBackPressed = { finish() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeConfigurationScreen(
    themeManager: ThemeManager,
    onBackPressed: () -> Unit,
) {
    val availableThemes = ThemePresets.getAllThemes()
    val currentTheme by themeManager.currentTheme.collectAsState()

    // Direct HEX colors for better contrast
    val backgroundColor = Color(0xFF121212)
    val surfaceColor = Color(0xFF1E1E1E)
    val onSurfaceColor = Color(0xFFE0E0E0)
    val primaryColor = Color(0xFF6750A4)

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "🎨 Configuración de Temas",
                        color = onSurfaceColor,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = onSurfaceColor,
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = surfaceColor,
                        titleContentColor = onSurfaceColor,
                        navigationIconContentColor = onSurfaceColor,
                    ),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "Selecciona un tema para tu teclado",
                    style = MaterialTheme.typography.bodyLarge,
                    color = onSurfaceColor,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }

            items(availableThemes.toList()) { (themeName, designTokens) ->
                ThemeCard(
                    themeName = themeName,
                    designTokens = designTokens,
                    isSelected = designTokens == currentTheme,
                    onThemeSelected = { themeManager.setCustomTheme(designTokens, themeName) },
                    surfaceColor = surfaceColor,
                    onSurfaceColor = onSurfaceColor,
                    primaryColor = primaryColor,
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Theme card showing differentiated colors for primary and secondary cell groups
 */
@Composable
private fun ThemeCard(
    themeName: String,
    designTokens: com.onehandkeyboard.core.design.tokens.DesignTokens,
    isSelected: Boolean,
    onThemeSelected: () -> Unit,
    surfaceColor: Color,
    onSurfaceColor: Color,
    primaryColor: Color,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onThemeSelected() }
                .then(
                    if (isSelected) {
                        Modifier.border(
                            width = 2.dp,
                            color = primaryColor,
                            shape = RoundedCornerShape(12.dp),
                        )
                    } else {
                        Modifier
                    },
                ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = surfaceColor,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Keyboard preview with differentiated colors
            KeyboardPreview(
                primaryColor = designTokens.colors.backgroundPrimary,
                secondaryColor = designTokens.colors.backgroundSecondary,
                backgroundColor = designTokens.colors.backgroundRoot,
                modifier = Modifier.size(120.dp, 80.dp),
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Theme info
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = themeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = onSurfaceColor,
                )

                Text(
                    text = getThemeDescription(themeName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = onSurfaceColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp),
                )

                // Color legend
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ColorLegendItem(
                        color = designTokens.colors.backgroundPrimary,
                        label = "Primario",
                        textColor = onSurfaceColor,
                    )
                    ColorLegendItem(
                        color = designTokens.colors.backgroundSecondary,
                        label = "Secundario",
                        textColor = onSurfaceColor,
                    )
                }
            }

            // Selection indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Seleccionado",
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun KeyboardPreview(
    primaryColor: Color,
    secondaryColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .background(backgroundColor, RoundedCornerShape(8.dp))
                .padding(8.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Row 0: cells 00, 01, 02, 03
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(3) {
                    Box(
                        modifier =
                            Modifier
                                .size(16.dp, 12.dp)
                                .background(primaryColor, RoundedCornerShape(2.dp)),
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .size(16.dp, 12.dp)
                            .background(secondaryColor, RoundedCornerShape(2.dp)),
                )
            }

            // Row 1: cells 10, 11, 12, 13
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(3) {
                    Box(
                        modifier =
                            Modifier
                                .size(16.dp, 12.dp)
                                .background(primaryColor, RoundedCornerShape(2.dp)),
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .size(16.dp, 12.dp)
                            .background(secondaryColor, RoundedCornerShape(2.dp)),
                )
            }

            // Row 2: cells 20, 21, 22, 23
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(3) {
                    Box(
                        modifier =
                            Modifier
                                .size(16.dp, 12.dp)
                                .background(primaryColor, RoundedCornerShape(2.dp)),
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .size(16.dp, 12.dp)
                            .background(secondaryColor, RoundedCornerShape(2.dp)),
                )
            }

            // Row 3: cells 30, 31, 32, 33 (all secondary)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(4) {
                    Box(
                        modifier =
                            Modifier
                                .size(16.dp, 12.dp)
                                .background(secondaryColor, RoundedCornerShape(2.dp)),
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorLegendItem(
    color: Color,
    label: String,
    textColor: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(12.dp)
                    .background(color, RoundedCornerShape(2.dp)),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            fontSize = 11.sp,
        )
    }
}

/**
 * Get theme description for display
 */
private fun getThemeDescription(themeName: String): String =
    when {
        themeName.contains("Monokai") -> "Tema popular para desarrolladores"
        themeName.contains("Solarized") -> "Bajo contraste, amigable para los ojos"
        themeName.contains("Dracula") -> "Tema vampiro elegante"
        themeName.contains("Gruvbox") && themeName.contains("Dark") -> "Esquema retro groove oscuro"
        themeName.contains("Gruvbox") && themeName.contains("Light") -> "Esquema retro groove claro"
        themeName.contains("Tokyo") -> "Tema moderno con acentos morados"
        themeName.contains("Ocean") -> "Tema fresco inspirado en el océano"
        themeName.contains("Forest") -> "Tema natural inspirado en el bosque"
        else -> "Tema personalizado"
    }
