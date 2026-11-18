package com.onehandkeyboard.update

import android.os.SystemClock
import com.onehandkeyboard.BuildConfig
import com.onehandkeyboard.prefs.KeyboardPreferences
import java.io.IOException
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Keyboard side of the self-update: looks for the server now and then while the user types and,
 * if a newer build can go in without asking, installs it when the keyboard hides (the update
 * restarts the keyboard, so never in the middle of typing).
 */
class KeyboardUpdates(
    private val updater: SelfUpdater,
    private val preferences: KeyboardPreferences,
    private val main: Executor,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) {
    private val executor = Executors.newSingleThreadExecutor()
    private var lastCheck: Long? = null
    private var pending: SelfUpdater.Background = SelfUpdater.Background.None

    /** A newer build is waiting for a tap in settings. */
    var waiting = false
        private set

    var onWaitingChanged: (Boolean) -> Unit = {}

    fun onKeyboardShown() {
        val now = clock()
        val due = lastCheck?.let { now - it >= CHECK_INTERVAL_MS } ?: true
        if (!BuildConfig.SELF_UPDATE || !preferences.autoUpdate || !due) return
        lastCheck = now
        executor.execute {
            val result = updater.prepareBackgroundUpdate()
            main.execute {
                pending = result
                waiting = result == SelfUpdater.Background.NeedsTap
                onWaitingChanged(waiting)
            }
        }
    }

    fun onKeyboardHidden() {
        val ready = pending as? SelfUpdater.Background.Ready ?: return
        pending = SelfUpdater.Background.None
        executor.execute {
            try {
                updater.installer.install(ready.apk, interactive = false)
            } catch (_: IOException) {
                main.execute { lastCheck = null } // try again next time
            }
        }
    }

    fun shutdown() {
        executor.shutdownNow()
    }

    private companion object {
        const val CHECK_INTERVAL_MS = 10 * 60 * 1000L
    }
}
