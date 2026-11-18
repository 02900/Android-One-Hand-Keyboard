package com.onehandkeyboard.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.os.Build
import android.widget.Toast
import androidx.core.content.IntentCompat
import com.onehandkeyboard.prefs.KeyboardPreferences
import java.io.File
import java.io.IOException

/**
 * Installs a downloaded build over this app with [PackageInstaller]. Android only accepts it if it
 * is signed with the same key, so an APK from someone else on the network can't replace the app.
 */
class UpdateInstaller(
    private val context: Context,
) {
    /** Android lets this app open the install screen ("install unknown apps" allowed). */
    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    /**
     * Updates can go in without asking (Android 12+) once this app installed itself at least once:
     * the first in-app update asks for confirmation, the following ones don't.
     */
    fun canInstallSilently(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || !canInstall()) return false
        val installer =
            try {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
            } catch (_: android.content.pm.PackageManager.NameNotFoundException) {
                null
            }
        return installer == context.packageName
    }

    @Throws(IOException::class)
    fun install(
        apk: File,
        interactive: Boolean,
    ) {
        val installer = context.packageManager.packageInstaller
        val params =
            PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(context.packageName)
                setSize(apk.length())
                if (!interactive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
            }
        val sessionId = installer.createSession(params)
        try {
            installer.openSession(sessionId).use { session ->
                write(session, apk)
                session.commit(statusReceiver(sessionId, interactive))
            }
        } catch (e: IOException) {
            installer.abandonSession(sessionId)
            throw e
        } catch (e: SecurityException) {
            installer.abandonSession(sessionId)
            throw IOException("Install rejected", e)
        }
    }

    private fun write(
        session: PackageInstaller.Session,
        apk: File,
    ) {
        session.openWrite("update.apk", 0, apk.length()).use { output ->
            apk.inputStream().use { it.copyTo(output) }
            session.fsync(output)
        }
    }

    private fun statusReceiver(
        sessionId: Int,
        interactive: Boolean,
    ): IntentSender {
        val intent =
            Intent(context, UpdateReceiver::class.java)
                .putExtra(EXTRA_INTERACTIVE, interactive)
        // Mutable: the installer adds the result to the intent.
        val mutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        return PendingIntent
            .getBroadcast(context, sessionId, intent, PendingIntent.FLAG_UPDATE_CURRENT or mutable)
            .intentSender
    }

    companion object {
        const val EXTRA_INTERACTIVE = "com.onehandkeyboard.update.INTERACTIVE"
    }
}

/** Result of an install session (success usually never arrives: the update restarts the app). */
class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val interactive = intent.getBooleanExtra(UpdateInstaller.EXTRA_INTERACTIVE, false)
        when {
            status == PackageInstaller.STATUS_SUCCESS -> Unit
            status == PackageInstaller.STATUS_PENDING_USER_ACTION && interactive -> confirm(context, intent)
            status == PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // A background update needs a tap after all: leave it to the settings screen.
                abandon(context, intent)
                KeyboardPreferences(context.applicationContext).updateNeedsConfirmation = true
            }
            interactive -> Toast.makeText(context, "No se pudo instalar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirm(
        context: Context,
        intent: Intent,
    ) {
        val confirm = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java) ?: return
        context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun abandon(
        context: Context,
        intent: Intent,
    ) {
        val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
        if (sessionId < 0) return
        try {
            context.packageManager.packageInstaller.abandonSession(sessionId)
        } catch (_: SecurityException) {
            // Already gone.
        }
    }
}
