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

        var cellHeightDp: Float
            get() = prefs.getFloat(KEY_CELL_HEIGHT, DEFAULT_CELL_HEIGHT_DP)
            set(value) = prefs.edit().putFloat(KEY_CELL_HEIGHT, value).apply()

        companion object {
            const val DEFAULT_CELL_HEIGHT_DP = 60f
            private const val PREFS_NAME = "keyboard_prefs"
            private const val KEY_HAPTICS = "haptics_enabled"
            private const val KEY_SOUND = "sound_enabled"
            private const val KEY_CELL_HEIGHT = "cell_height_dp"
        }
    }
