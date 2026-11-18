package com.onehandkeyboard.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateManifestTest {
    private fun build(
        commit: String,
        time: Long,
    ) = UpdateInfo(commit, time, "1.0.0-debug", "OneHandKeyboard-$commit.apk", 1, "0".repeat(64))

    @Test
    fun `short hashes of different length are the same commit`() {
        assertTrue(UpdateManifest.sameCommit("b6127bb", "b6127bbd1"))
        assertFalse(UpdateManifest.sameCommit("b6127bb", "7596eb0"))
        assertFalse(UpdateManifest.isNewer(build("b6127bbd1", 200), "b6127bb", 100))
    }

    @Test
    fun `the newest update among the servers wins`() {
        val found =
            mapOf(
                "http://192.168.0.20:8000" to build("aaaaaaa", 300),
                "https://github.com/x/releases/latest/download" to build("bbbbbbb", 200),
            )

        val newest = UpdateManifest.newest(found, "ccccccc", 100)

        assertEquals("http://192.168.0.20:8000", newest?.first)
        assertEquals("aaaaaaa", newest?.second?.commit)
    }

    @Test
    fun `nothing newer than the installed build is no update`() {
        val found = mapOf("https://github.com/x" to build("aaaaaaa", 100))

        assertNull(UpdateManifest.newest(found, "aaaaaaa", 100))
        assertNull(UpdateManifest.newest(found, "ccccccc", 200))
        assertNull(UpdateManifest.newest(emptyMap(), "ccccccc", 100))
    }
}
