package com.onehandkeyboard.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.onehandkeyboard.memes.MemeLibrary
import com.onehandkeyboard.prefs.KeyboardPreferences
import com.onehandkeyboard.spell.SpellEngines
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

        // Meme library: a folder of GIFs and stickers picked by the user
        item {
            MemeSettings(preferences)
        }

        // Opens the system list of keyboards, where this one is enabled
        item {
            OutlinedButton(
                onClick = { openKeyboardSettings(context) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "⌨️ Activar el teclado",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/** Opens the system screen that enables keyboards (falls back to the main settings). */
private fun openKeyboardSettings(context: Context) {
    val flags = Intent.FLAG_ACTIVITY_NEW_TASK
    try {
        context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).addFlags(flags))
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(flags))
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
private fun MemeSettings(preferences: KeyboardPreferences) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val library = remember { MemeLibrary(context.applicationContext) }
    var folder by remember { mutableStateOf(preferences.memeFolderUri) }
    var summary by remember { mutableStateOf("") }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            val picked = uri.toString()
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
                Toast.makeText(context, "No se pudo dar acceso a esa carpeta", Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            folder?.takeIf { it != picked }?.let { releaseFolder(context, it) }
            preferences.memeFolderUri = picked
            preferences.recentMemes = emptyList()
            folder = picked
        }
    LaunchedEffect(folder) {
        summary = "Leyendo la carpeta…"
        summary = withContext(Dispatchers.IO) { describeLibrary(library, folder) }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🖼️ Biblioteca de memes",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text =
                    "Copia tu pack de GIFs y stickers a una carpeta del teléfono (una subcarpeta por " +
                        "categoría) y elígela aquí. En el teclado, desliza hacia abajo en la tecla 123.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = summary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(
                onClick = { picker.launch(null) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text(if (folder == null) "Elegir carpeta" else "Cambiar carpeta")
            }
            if (folder != null) {
                OutlinedButton(
                    onClick = {
                        folder?.let { releaseFolder(context, it) }
                        preferences.memeFolderUri = null
                        preferences.recentMemes = emptyList()
                        folder = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Quitar carpeta")
                }
            }
        }
    }
}

private fun describeLibrary(
    library: MemeLibrary,
    folder: String?,
): String =
    when (val result = library.load(folder)) {
        MemeLibrary.Result.NotConfigured -> "Sin carpeta elegida."
        MemeLibrary.Result.NoAccess -> "No se puede leer la carpeta (¿se movió o borró?). Vuelve a elegirla."
        is MemeLibrary.Result.Loaded -> {
            val name = folder?.let(library::folderName) ?: "Carpeta"
            val memes = result.categories.sumOf { it.memes.size }
            "📁 $name · $memes memes en ${result.categories.size} categorías"
        }
    }

/** Gives back the read access to a folder that is no longer used. */
private fun releaseFolder(
    context: Context,
    folder: String,
) {
    try {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        context.contentResolver.releasePersistableUriPermission(Uri.parse(folder), flags)
    } catch (_: SecurityException) {
        // Already released (e.g. the folder was deleted).
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
