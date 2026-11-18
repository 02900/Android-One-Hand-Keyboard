package com.onehandkeyboard.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.onehandkeyboard.BuildConfig
import com.onehandkeyboard.R
import com.onehandkeyboard.core.design.theme.KeyboardThemeWithTokens
import com.onehandkeyboard.core.design.theme.ThemeManager
import com.onehandkeyboard.memes.MemeLibrary
import com.onehandkeyboard.prefs.KeyboardPreferences
import com.onehandkeyboard.spell.SpellEngines
import com.onehandkeyboard.update.AppSharer
import com.onehandkeyboard.update.SelfUpdater
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

/** Keyboard settings: one row per option, label on the left and its control on the right. */
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
        // The keyboard cannot show the permission dialog itself: it opens this screen to ask.
        val requestMicrophone =
            intent.getBooleanExtra(EXTRA_REQUEST_MICROPHONE, false) && savedInstanceState == null
        setContent {
            OneHandKeyboardTheme(themeManager = themeManager) {
                SettingsScreen(preferences, spellEngines::clearLearnedWords, requestMicrophone)
            }
        }
    }

    companion object {
        const val EXTRA_REQUEST_MICROPHONE = "request_microphone"
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
    requestMicrophone: Boolean = false,
) {
    val context = LocalContext.current
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        SettingRow("Teclado") {
            OutlinedButton(onClick = { openKeyboardSettings(context) }) { Text("Activar") }
        }
        SettingRow("Temas") {
            OutlinedButton(onClick = {
                context.startActivity(Intent(context, ThemeConfigurationActivity::class.java))
            }) { Text("Configurar") }
        }
        WritingSettings(preferences, onClearLearnedWords)
        VoiceSettings(requestMicrophone)
        MemeSettings(preferences)
        ShareAppRow()
        if (BuildConfig.SELF_UPDATE) UpdateSettings(preferences)
    }
}

/** Voice typing needs the microphone permission; asked here (right away when the keyboard sent us). */
@Composable
private fun VoiceSettings(requestOnOpen: Boolean) {
    val context = LocalContext.current

    fun granted() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    var allowed by remember { mutableStateOf(granted()) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            allowed = result
            if (result && requestOnOpen) {
                Toast.makeText(context, "Listo: vuelve al teclado y desliza ← en 123", Toast.LENGTH_LONG).show()
            }
        }
    LaunchedEffect(Unit) { if (requestOnOpen && !allowed) launcher.launch(Manifest.permission.RECORD_AUDIO) }
    SettingRow("Dictado por voz", value = if (allowed) "Micrófono permitido" else null) {
        if (!allowed) {
            OutlinedButton(onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) }) { Text("Permitir") }
        }
    }
}

@Composable
private fun ShareAppRow() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sharer = remember { AppSharer(context.applicationContext) }
    SettingRow("Compartir app") {
        OutlinedButton(onClick = {
            scope.launch {
                try {
                    val apk = withContext(Dispatchers.IO) { sharer.prepare() }
                    context.startActivity(sharer.intent(apk))
                } catch (_: IOException) {
                    Toast.makeText(context, "No se pudo preparar el APK", Toast.LENGTH_SHORT).show()
                }
            }
        }) { Text("Compartir") }
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
    val context = LocalContext.current
    var autocorrect by remember { mutableStateOf(preferences.autocorrectEnabled) }
    var suggestions by remember { mutableStateOf(preferences.suggestionsEnabled) }
    SettingRow("Corrección automática") {
        Switch(checked = autocorrect, onCheckedChange = {
            autocorrect = it
            preferences.autocorrectEnabled = it
        })
    }
    SettingRow("Barra de sugerencias") {
        Switch(checked = suggestions, onCheckedChange = {
            suggestions = it
            preferences.suggestionsEnabled = it
        })
    }
    var expertMode by remember { mutableStateOf(preferences.expertMode) }
    SettingRow("Modo experto") {
        Switch(checked = expertMode, onCheckedChange = {
            expertMode = it
            preferences.expertMode = it
        })
    }
    SettingRow("Palabras aprendidas") {
        OutlinedButton(onClick = {
            onClearLearnedWords()
            Toast.makeText(context, "Palabras aprendidas borradas", Toast.LENGTH_SHORT).show()
        }) { Text("Borrar") }
    }
}

@Composable
private fun MemeSettings(preferences: KeyboardPreferences) {
    val context = LocalContext.current
    val library = remember { MemeLibrary(context.applicationContext) }
    var folder by remember { mutableStateOf(preferences.memeFolderUri) }
    var summary by remember { mutableStateOf<String?>(null) }
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
        summary = null
        summary = withContext(Dispatchers.IO) { describeLibrary(library, folder) }
    }
    SettingRow("Memes", value = summary) {
        if (folder != null) {
            TextButton(onClick = {
                folder?.let { releaseFolder(context, it) }
                preferences.memeFolderUri = null
                preferences.recentMemes = emptyList()
                folder = null
            }) { Text("Quitar") }
        }
        OutlinedButton(onClick = { picker.launch(null) }) {
            Text(if (folder == null) "Elegir carpeta" else "Cambiar")
        }
    }
}

/** Short state of the meme folder, or null when none is chosen. */
private fun describeLibrary(
    library: MemeLibrary,
    folder: String?,
): String? =
    when (val result = library.load(folder)) {
        MemeLibrary.Result.NotConfigured -> null
        MemeLibrary.Result.NoAccess -> "⚠️ Sin acceso, vuelve a elegirla"
        is MemeLibrary.Result.Loaded -> {
            val name = folder?.let(library::folderName) ?: "Carpeta"
            "📁 $name · ${result.categories.sumOf { it.memes.size }}"
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

/** What the update button shows. */
private sealed interface UpdateState {
    data object Checking : UpdateState

    data object NoServer : UpdateState

    data object UpToDate : UpdateState

    data class Available(
        val update: SelfUpdater.Check.Available,
    ) : UpdateState

    data class Downloading(
        val percent: Int,
    ) : UpdateState

    data object Failed : UpdateState
}

@Composable
private fun UpdateSettings(preferences: KeyboardPreferences) {
    val context = LocalContext.current
    val updater = remember { SelfUpdater(context.applicationContext, preferences) }
    val scope = rememberCoroutineScope()
    var auto by remember { mutableStateOf(preferences.autoUpdate) }
    var state by remember { mutableStateOf<UpdateState>(UpdateState.Checking) }
    LaunchedEffect(Unit) { state = checkForUpdate(updater) }

    SettingRow("Actualizar sola") {
        Switch(checked = auto, onCheckedChange = {
            auto = it
            preferences.autoUpdate = it
        })
    }
    val installed = "${BuildConfig.VERSION_NAME} · ${BuildConfig.GIT_COMMIT}"
    val current = state
    SettingRow(
        "Versión",
        value = if (current is UpdateState.Available) "$installed → ${current.update.info.commit}" else installed,
    ) {
        val busy = current is UpdateState.Checking || current is UpdateState.Downloading
        val onClick: () -> Unit = {
            scope.launch {
                if (current is UpdateState.Available) {
                    state = installUpdate(context, updater, current) { state = it }
                } else {
                    state = UpdateState.Checking
                    state = checkForUpdate(updater)
                }
            }
        }
        if (current is UpdateState.Available) {
            Button(onClick = onClick) { Text("Actualizar") }
        } else {
            OutlinedButton(onClick = onClick, enabled = !busy) { Text(updateLabel(current)) }
        }
    }
}

private fun updateLabel(state: UpdateState): String =
    when (state) {
        UpdateState.Checking -> "Buscando…"
        UpdateState.NoServer -> "Sin conexión"
        UpdateState.UpToDate -> "Al día ✓"
        UpdateState.Failed -> "Reintentar"
        is UpdateState.Downloading -> "${state.percent} %"
        is UpdateState.Available -> "Actualizar"
    }

private suspend fun checkForUpdate(updater: SelfUpdater): UpdateState =
    when (val check = withContext(Dispatchers.IO) { updater.check() }) {
        SelfUpdater.Check.NoServer -> UpdateState.NoServer
        SelfUpdater.Check.UpToDate -> UpdateState.UpToDate
        is SelfUpdater.Check.Available -> UpdateState.Available(check)
    }

/** Downloads and opens Android's install screen; the button goes back to "Actualizar" meanwhile. */
private suspend fun installUpdate(
    context: Context,
    updater: SelfUpdater,
    available: UpdateState.Available,
    onState: (UpdateState) -> Unit,
): UpdateState {
    if (!updater.installer.canInstall()) {
        Toast.makeText(context, "Permite instalar apps de esta fuente", Toast.LENGTH_SHORT).show()
        val uri = Uri.parse("package:${context.packageName}")
        context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, uri))
        return available
    }
    return try {
        withContext(Dispatchers.IO) {
            val apk = updater.download(available.update) { onState(UpdateState.Downloading(it)) }
            updater.installer.install(apk, interactive = true)
        }
        available
    } catch (_: IOException) {
        UpdateState.Failed
    }
}

@Composable
private fun SettingRow(
    label: String,
    value: String? = null,
    action: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
            if (value != null) {
                Text(text = value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        action()
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
