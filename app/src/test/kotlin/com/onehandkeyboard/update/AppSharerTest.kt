package com.onehandkeyboard.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import androidx.test.core.app.ApplicationProvider
import com.onehandkeyboard.BuildConfig
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AppSharerTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val sharer = AppSharer(context) { Uri.parse("content://share.test/${it.name}") }

    private fun installedApk(bytes: ByteArray): File =
        temp.newFile("base.apk").apply {
            writeBytes(bytes)
            context.applicationInfo.sourceDir = path
        }

    @Test
    fun `copies the installed apk with the build in its name`() {
        installedApk(byteArrayOf(1, 2, 3, 4))

        val apk = sharer.prepare()

        assertEquals("OneHandKeyboard-${BuildConfig.BUILD_TYPE}-${BuildConfig.GIT_COMMIT}.apk", apk.name)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), apk.readBytes())
    }

    @Test
    fun `share sheet sends the apk as a readable file`() {
        installedApk(byteArrayOf(9))
        val apk = sharer.prepare()

        val chooser = sharer.intent(apk)
        val send = IntentCompat.getParcelableExtra(chooser, Intent.EXTRA_INTENT, Intent::class.java)!!

        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        assertEquals(AppSharer.APK_MIME, send.type)
        assertEquals(
            Uri.parse("content://share.test/${apk.name}"),
            IntentCompat.getParcelableExtra(send, Intent.EXTRA_STREAM, Uri::class.java),
        )
        assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }
}
