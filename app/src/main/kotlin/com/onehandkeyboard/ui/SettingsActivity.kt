package com.onehandkeyboard.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onehandkeyboard.R
import com.onehandkeyboard.core.design.theme.KeyboardThemeWithTokens
import com.onehandkeyboard.core.design.theme.ThemeManager
import com.onehandkeyboard.prefs.KeyboardPreferences
import com.onehandkeyboard.spell.SpellEngines
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Settings activity for OneHandKeyboard
 *
 * This is a placeholder activity that will be expanded in later stages
 * to provide full configuration options for the keyboard.
 */
@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    @Inject
    lateinit var themeManager: ThemeManager

    @Inject
    lateinit var preferences: KeyboardPreferences

    @Inject
    lateinit var spellEngines: SpellEngines

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OneHandKeyboardTheme(themeManager = themeManager) {
                SettingsScreen(preferences, spellEngines::clearLearnedWords)
            }
        }
    }
}

@Composable
private fun OneHandKeyboardTheme(
    themeManager: ThemeManager,
    content: @Composable () -> Unit,
) {
    KeyboardThemeWithTokens(
        themeManager = themeManager,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            content = content,
        )
    }
}

@Composable
private fun SettingsScreen(
    preferences: KeyboardPreferences,
    onClearLearnedWords: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header Section
        item {
            Column {
                Text(
                    text = stringResource(R.string.settings_title),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                Text(
                    text = "Customize your OneHandKeyboard experience",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
        }

        // Theme Configuration Button
        item {
            Button(
                onClick = {
                    val intent = Intent(context, ThemeConfigurationActivity::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
            ) {
                Text(
                    text = "🎨 Configurar Temas",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // Writing: autocorrect and suggestions
        item {
            WritingSettings(preferences, onClearLearnedWords)
        }

        // Divider
        item {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }

        // Keyboard Setup Instructions
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = "🚀 Setup Instructions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    Text(
                        text = "To use OneHandKeyboard, enable it in your device settings:",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    Text(
                        text = "Settings → System → Languages & input → On-screen keyboard → OneHandKeyboard",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun WritingSettings(
    preferences: KeyboardPreferences,
    onClearLearnedWords: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var autocorrect by remember { mutableStateOf(preferences.autocorrectEnabled) }
    var suggestions by remember { mutableStateOf(preferences.suggestionsEnabled) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "✍️ Escritura",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SettingSwitch(
                title = "Corrección automática",
                subtitle = "Corrige palabras y tildes al poner espacio. Borrar justo después la deshace.",
                checked = autocorrect,
            ) {
                autocorrect = it
                preferences.autocorrectEnabled = it
            }
            SettingSwitch(
                title = "Barra de sugerencias",
                subtitle = "Muestra alternativas encima del teclado mientras escribes.",
                checked = suggestions,
            ) {
                suggestions = it
                preferences.suggestionsEnabled = it
            }
            OutlinedButton(
                onClick = {
                    onClearLearnedWords()
                    Toast.makeText(context, "Palabras aprendidas borradas", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text("Borrar palabras aprendidas")
            }
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    MaterialTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Text("Settings Screen Preview")
                Text("(Requires ThemeManager injection to function)")
            }
        }
    }
}
