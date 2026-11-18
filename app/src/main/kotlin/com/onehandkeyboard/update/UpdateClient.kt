package com.onehandkeyboard.update

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Reads `update.json` and downloads the APK from the update server (plain HTTP on the LAN). */
class UpdateClient {
    /** The published build, or null if the server answered something that isn't a manifest. */
    @Throws(IOException::class)
    fun fetch(server: String): UpdateInfo? =
        request("$server/${UpdateManifest.FILE_NAME}") { input ->
            UpdateManifest.parse(readLimited(input, MAX_MANIFEST_BYTES).decodeToString())
        }

    /**
     * Downloads the APK into [dest], checking size and SHA-256 so a cut download is never installed.
     * [onProgress] gets 0..100.
     */
    @Throws(IOException::class)
    fun download(
        server: String,
        info: UpdateInfo,
        dest: File,
        onProgress: (Int) -> Unit = {},
    ) {
        val partial = File(dest.parentFile, "${dest.name}.part")
        val digest = MessageDigest.getInstance("SHA-256")
        request("$server/${info.apk}") { input ->
            partial.outputStream().use { output -> copy(input, output, digest, info.size, onProgress) }
        }
        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        if (partial.length() != info.size || hash != info.sha256) {
            partial.delete()
            throw IOException("Downloaded APK does not match update.json")
        }
        if (!partial.renameTo(dest)) throw IOException("Could not save the APK")
    }

    private fun <T> request(
        url: String,
        read: (InputStream) -> T,
    ): T {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.useCaches = false
        try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code for $url")
            return connection.inputStream.use(read)
        } finally {
            connection.disconnect()
        }
    }

    private fun copy(
        input: InputStream,
        output: OutputStream,
        digest: MessageDigest,
        expected: Long,
        onProgress: (Int) -> Unit,
    ) {
        val buffer = ByteArray(BUFFER_BYTES)
        var total = 0L
        var lastPercent = -1
        var read = input.read(buffer)
        while (read >= 0) {
            total += read
            if (total > expected) throw IOException("APK is bigger than announced")
            output.write(buffer, 0, read)
            digest.update(buffer, 0, read)
            val percent = (total * PERCENT / expected).toInt()
            if (percent != lastPercent) onProgress(percent).also { lastPercent = percent }
            read = input.read(buffer)
        }
    }

    private fun readLimited(
        input: InputStream,
        max: Int,
    ): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_BYTES)
        var read = input.read(buffer)
        while (read >= 0) {
            output.write(buffer, 0, read)
            if (output.size() > max) throw IOException("update.json is too big")
            read = input.read(buffer)
        }
        return output.toByteArray()
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 3000
        const val READ_TIMEOUT_MS = 10000
        const val BUFFER_BYTES = 64 * 1024
        const val MAX_MANIFEST_BYTES = 16 * 1024
        const val PERCENT = 100
    }
}
