package com.onehandkeyboard.update

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.onehandkeyboard.BuildConfig
import com.onehandkeyboard.memes.MemeSender
import java.io.File
import java.io.IOException

/** Sends the installed APK as a file (Telegram, WhatsApp, …) so a friend can install it. */
class AppSharer(
    private val context: Context,
    private val shareUri: (File) -> Uri = { FileProvider.getUriForFile(context, MemeSender.authority(context), it) },
) {
    /** Copies the installed APK to the shared cache folder. Blocking: call it off the main thread. */
    @Throws(IOException::class)
    fun prepare(): File {
        val folder = File(context.cacheDir, "share").apply { mkdirs() }
        val apk = File(folder, "OneHandKeyboard-${BuildConfig.GIT_COMMIT}.apk")
        folder.listFiles()?.filter { it != apk }?.forEach { it.delete() }
        val installed = File(context.applicationInfo.sourceDir)
        if (!apk.isFile || apk.length() != installed.length()) installed.copyTo(apk, overwrite = true)
        return apk
    }

    /** The share sheet for [apk], with read access only for the app that receives it. */
    fun intent(apk: File): Intent {
        val uri = shareUri(apk)
        val send =
            Intent(Intent.ACTION_SEND)
                .setType(APK_MIME)
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(apk.name, uri)
        return Intent.createChooser(send, null)
    }

    companion object {
        const val APK_MIME = "application/vnd.android.package-archive"
    }
}
