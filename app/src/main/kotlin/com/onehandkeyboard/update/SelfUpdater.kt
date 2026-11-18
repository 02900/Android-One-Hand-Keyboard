package com.onehandkeyboard.update

import android.content.Context
import com.onehandkeyboard.BuildConfig
import com.onehandkeyboard.prefs.KeyboardPreferences
import java.io.File
import java.io.IOException

/**
 * Updates the app from the latest GitHub release and from `tools/serve_updates.py` on a PC of the
 * same network (to try builds before publishing them); the newest of both wins. When neither
 * answers nothing happens. All functions block (network): call them off the main thread.
 */
class SelfUpdater(
    context: Context,
    private val preferences: KeyboardPreferences,
    private val discovery: UpdateDiscovery = UpdateDiscovery(),
    private val client: UpdateClient = UpdateClient(),
    private val releasesUrl: String = BuildConfig.UPDATE_RELEASES_URL,
    val installer: UpdateInstaller = UpdateInstaller(context),
) {
    private val folder = File(context.cacheDir, "updates")

    sealed interface Check {
        data object NoServer : Check

        data object UpToDate : Check

        data class Available(
            val server: String,
            val info: UpdateInfo,
        ) : Check
    }

    /** What the keyboard can do by itself. */
    sealed interface Background {
        data object None : Background

        /** There is a newer build but installing it needs a tap (in settings). */
        data object NeedsTap : Background

        data class Ready(
            val apk: File,
        ) : Background
    }

    fun check(): Check {
        val lan = discovery.find()?.also { preferences.updateServer = it } ?: preferences.updateServer
        val found =
            listOfNotNull(lan, releasesUrl.ifEmpty { null })
                .mapNotNull { server -> fetch(server)?.let { server to it } }
                .toMap()
        val newest = UpdateManifest.newest(found, BuildConfig.GIT_COMMIT, BuildConfig.GIT_COMMIT_TIME)
        return when {
            newest != null -> Check.Available(newest.first, newest.second)
            found.isEmpty() -> Check.NoServer
            else -> Check.UpToDate.also { preferences.updateNeedsConfirmation = false }
        }
    }

    private fun fetch(server: String): UpdateInfo? =
        try {
            client.fetch(server)
        } catch (_: IOException) {
            null
        }

    /** The verified APK of [update] (reused if it was already downloaded). */
    @Throws(IOException::class)
    fun download(
        update: Check.Available,
        onProgress: (Int) -> Unit = {},
    ): File {
        folder.mkdirs()
        val apk = File(folder, "${update.info.commit}.apk")
        folder.listFiles()?.filter { it != apk }?.forEach { it.delete() }
        if (!apk.isFile || apk.length() != update.info.size) {
            client.download(update.server, update.info, apk, onProgress)
        }
        return apk
    }

    /** For the keyboard: downloads a newer build only if it can be installed without asking. */
    fun prepareBackgroundUpdate(): Background {
        val update = check() as? Check.Available ?: return Background.None
        val silent = installer.canInstallSilently() && !preferences.updateNeedsConfirmation
        return if (silent) {
            try {
                Background.Ready(download(update))
            } catch (_: IOException) {
                Background.None
            }
        } else {
            Background.NeedsTap
        }
    }
}
