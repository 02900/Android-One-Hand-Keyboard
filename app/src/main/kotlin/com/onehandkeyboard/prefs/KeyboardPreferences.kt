package com.onehandkeyboard.prefs

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistent keyboard settings (survive service restarts and view re-creations).
 */
@Singleton
class KeyboardPreferences
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        var hapticsEnabled: Boolean
            get() = prefs.getBoolean(KEY_HAPTICS, true)
            set(value) = prefs.edit().putBoolean(KEY_HAPTICS, value).apply()

        var soundEnabled: Boolean
            get() = prefs.getBoolean(KEY_SOUND, false)
            set(value) = prefs.edit().putBoolean(KEY_SOUND, value).apply()

        /** Replace unknown words with a confident correction on space / punctuation. */
        var autocorrectEnabled: Boolean
            get() = prefs.getBoolean(KEY_AUTOCORRECT, true)
            set(value) = prefs.edit().putBoolean(KEY_AUTOCORRECT, value).apply()

        /** Suggestion strip above the keys. */
        var suggestionsEnabled: Boolean
            get() = prefs.getBoolean(KEY_SUGGESTIONS, true)
            set(value) = prefs.edit().putBoolean(KEY_SUGGESTIONS, value).apply()

        /** Pasting removes tracking parameters from links (see LinkCleaner). */
        var cleanPastedLinks: Boolean
            get() = prefs.getBoolean(KEY_CLEAN_PASTED_LINKS, true)
            set(value) = prefs.edit().putBoolean(KEY_CLEAN_PASTED_LINKS, value).apply()

        /** Keys without labels or preview, for users who know the layout by heart. */
        var expertMode: Boolean
            get() = prefs.getBoolean(KEY_EXPERT_MODE, false)
            set(value) = prefs.edit().putBoolean(KEY_EXPERT_MODE, value).apply()

        /** Folder (Storage Access Framework tree URI) with the user's memes, or null if not set. */
        var memeFolderUri: String?
            get() = prefs.getString(KEY_MEME_FOLDER, null)
            set(value) = prefs.edit().putString(KEY_MEME_FOLDER, value).apply()

        /** URIs of the last memes sent, most recent first. */
        var recentMemes: List<String>
            get() = prefs.getString(KEY_RECENT_MEMES, null)?.split(LIST_SEPARATOR)?.filter { it.isNotEmpty() }.orEmpty()
            set(value) = prefs.edit().putString(KEY_RECENT_MEMES, value.joinToString(LIST_SEPARATOR)).apply()

        /** The last emojis typed from the emoji panel, most recent first. */
        var recentEmojis: List<String>
            get() =
                prefs.getString(
                    KEY_RECENT_EMOJIS,
                    null,
                )?.split(LIST_SEPARATOR)?.filter { it.isNotEmpty() }.orEmpty()
            set(value) = prefs.edit().putString(KEY_RECENT_EMOJIS, value.joinToString(LIST_SEPARATOR)).apply()

        /** Height of the meme panel chosen with its slider: 0 = the keyboard's, 1 = the tallest. */
        var memePanelSize: Float
            get() = prefs.getFloat(KEY_MEME_PANEL_SIZE, 0f)
            set(value) = prefs.edit().putFloat(KEY_MEME_PANEL_SIZE, value.coerceIn(0f, 1f)).apply()

        /** Install new builds from the update server by themselves (debug builds). */
        var autoUpdate: Boolean
            get() = prefs.getBoolean(KEY_AUTO_UPDATE, true)
            set(value) = prefs.edit().putBoolean(KEY_AUTO_UPDATE, value).apply()

        /** Last address where the update server answered (used when the broadcast gets no answer). */
        var updateServer: String?
            get() = prefs.getString(KEY_UPDATE_SERVER, null)
            set(value) = prefs.edit().putString(KEY_UPDATE_SERVER, value).apply()

        /** A background install needed a tap: wait for the user to update from settings. */
        var updateNeedsConfirmation: Boolean
            get() = prefs.getBoolean(KEY_UPDATE_NEEDS_CONFIRMATION, false)
            set(value) = prefs.edit().putBoolean(KEY_UPDATE_NEEDS_CONFIRMATION, value).apply()

        /** Commit of the build that ran last, to notice that the app was updated. */
        var lastRunCommit: String?
            get() = prefs.getString(KEY_LAST_RUN_COMMIT, null)
            set(value) = prefs.edit().putString(KEY_LAST_RUN_COMMIT, value).apply()

        companion object {
            private const val KEY_EXPERT_MODE = "expert_mode"
            private const val KEY_AUTO_UPDATE = "auto_update"
            private const val KEY_UPDATE_SERVER = "update_server"
            private const val KEY_UPDATE_NEEDS_CONFIRMATION = "update_needs_confirmation"
            private const val KEY_LAST_RUN_COMMIT = "last_run_commit"
            private const val KEY_MEME_FOLDER = "meme_folder_uri"
            private const val KEY_RECENT_MEMES = "recent_memes"
            private const val KEY_MEME_PANEL_SIZE = "meme_panel_size"
            private const val KEY_RECENT_EMOJIS = "recent_emojis"
            private const val LIST_SEPARATOR = "\n"
            private const val KEY_AUTOCORRECT = "autocorrect_enabled"
            private const val KEY_SUGGESTIONS = "suggestions_enabled"
            private const val KEY_CLEAN_PASTED_LINKS = "clean_pasted_links"
            private const val PREFS_NAME = "keyboard_prefs"
            private const val KEY_HAPTICS = "haptics_enabled"
            private const val KEY_SOUND = "sound_enabled"
        }
    }
