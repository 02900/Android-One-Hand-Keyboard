package com.onehandkeyboard.update

import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.security.MessageDigest
import kotlin.concurrent.thread

/** Manifest parsing (org.json needs Robolectric) and a real HTTP round trip on localhost. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class UpdateClientTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val apkBytes = ByteArray(150_000) { (it % 251).toByte() }
    private val files = mutableMapOf<String, ByteArray>()
    private val server = ServerSocket(0, 50, InetAddress.getLoopbackAddress())
    private val base = "http://127.0.0.1:${server.localPort}"

    /** Minimal HTTP/1.0 server: GET /<name> answers files[name] or 404. */
    @Before
    fun startServer() {
        thread(isDaemon = true) {
            while (!server.isClosed) {
                val socket = runCatching { server.accept() }.getOrNull() ?: break
                socket.use {
                    val requestLine = it.getInputStream().bufferedReader().readLine().orEmpty()
                    val path = requestLine.split(" ").getOrElse(1) { "/" }
                    val body = files[path.removePrefix("/")]
                    val status = if (body == null) "404 Not Found" else "200 OK"
                    val bytes = body ?: ByteArray(0)
                    it.getOutputStream().apply {
                        val head = "HTTP/1.0 $status\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
                        write(head.toByteArray())
                        write(bytes)
                        flush()
                    }
                }
            }
        }
    }

    @After
    fun stopServer() = server.close()

    private fun sha256(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun manifest(
        apk: String = "OneHandKeyboard-abc1234.apk",
        sha: String = sha256(apkBytes),
        size: Long = apkBytes.size.toLong(),
    ) = """{"commit":"abc1234","commitTime":1700000000,"versionName":"1.0.0-debug",""" +
        """"apk":"$apk","size":$size,"sha256":"$sha"}"""

    @Test
    fun `parses a valid manifest`() {
        val info = UpdateManifest.parse(manifest())

        assertNotNull(info)
        assertEquals("abc1234", info!!.commit)
        assertEquals(1_700_000_000L, info.commitTime)
        assertEquals(apkBytes.size.toLong(), info.size)
    }

    @Test
    fun `rejects manifests that point outside the folder or are broken`() {
        assertNull(UpdateManifest.parse(manifest(apk = "../evil.apk")))
        assertNull(UpdateManifest.parse(manifest(apk = "http://x/evil.apk")))
        assertNull(UpdateManifest.parse(manifest(sha = "123")))
        assertNull(UpdateManifest.parse(manifest(size = 0)))
        assertNull(UpdateManifest.parse("<html>not json</html>"))
    }

    @Test
    fun `only a different, not older commit is an update`() {
        val info = UpdateManifest.parse(manifest())!!

        assertTrue(UpdateManifest.isNewer(info, "0314f60", 1_600_000_000L))
        assertTrue(UpdateManifest.isNewer(info, "0314f60", 1_700_000_000L))
        assertFalse(UpdateManifest.isNewer(info, "abc1234", 1_600_000_000L))
        assertFalse(UpdateManifest.isNewer(info, "0314f60", 1_800_000_000L))
    }

    @Test
    fun `fetches the manifest and downloads a verified apk`() {
        files["update.json"] = manifest().toByteArray()
        files["OneHandKeyboard-abc1234.apk"] = apkBytes
        val client = UpdateClient()
        val progress = mutableListOf<Int>()

        val info = client.fetch(base)!!
        val dest = File(temp.root, "update.apk")
        client.download(base, info, dest) { progress += it }

        assertArrayEquals(apkBytes, dest.readBytes())
        assertEquals(100, progress.last())
    }

    @Test
    fun `a corrupt download is thrown away`() {
        files["OneHandKeyboard-abc1234.apk"] = apkBytes.copyOf().also { it[10] = 99 }
        val info = UpdateManifest.parse(manifest())!!
        val dest = File(temp.root, "update.apk")

        val error = runCatching { UpdateClient().download(base, info, dest) }.exceptionOrNull()

        assertTrue(error is IOException)
        assertFalse(dest.exists())
        assertFalse(File(temp.root, "update.apk.part").exists())
    }

    @Test
    fun `missing manifest is an error`() {
        val error = runCatching { UpdateClient().fetch(base) }.exceptionOrNull()

        assertTrue(error is IOException)
    }
}
