package com.onehandkeyboard.update

import org.json.JSONException
import org.json.JSONObject

/** A build published by `tools/serve_updates.py` (its `update.json`). */
data class UpdateInfo(
    val commit: String,
    val commitTime: Long,
    val versionName: String,
    val apk: String,
    val size: Long,
    val sha256: String,
)

object UpdateManifest {
    const val FILE_NAME = "update.json"
    const val MAX_APK_BYTES = 200L * 1024 * 1024

    private val COMMIT = Regex("[0-9a-f]{4,40}")
    private val APK_NAME = Regex("[A-Za-z0-9._-]+\\.apk")
    private val SHA256 = Regex("[0-9a-f]{64}")

    /** Null when the text is not a valid manifest (the file name can't point outside the server folder). */
    fun parse(json: String): UpdateInfo? =
        try {
            val root = JSONObject(json)
            UpdateInfo(
                commit = root.getString("commit"),
                commitTime = root.getLong("commitTime"),
                versionName = root.optString("versionName"),
                apk = root.getString("apk"),
                size = root.getLong("size"),
                sha256 = root.getString("sha256").lowercase(),
            ).takeIf(::isValid)
        } catch (_: JSONException) {
            null
        }

    private fun isValid(info: UpdateInfo): Boolean =
        COMMIT.matches(info.commit) &&
            APK_NAME.matches(info.apk) &&
            SHA256.matches(info.sha256) &&
            info.size in 1..MAX_APK_BYTES

    /**
     * A different commit that is not older than the installed one. Same commit = already installed,
     * so installing an update can never loop.
     */
    fun isNewer(
        info: UpdateInfo,
        installedCommit: String,
        installedCommitTime: Long,
    ): Boolean = !sameCommit(info.commit, installedCommit) && info.commitTime >= installedCommitTime

    /** Short hashes can have different lengths (git lengthens them on bigger clones). */
    fun sameCommit(
        a: String,
        b: String,
    ): Boolean = a.startsWith(b) || b.startsWith(a)

    /** The newest of the builds found on each server that is an update, with its server. */
    fun newest(
        found: Map<String, UpdateInfo>,
        installedCommit: String,
        installedCommitTime: Long,
    ): Pair<String, UpdateInfo>? =
        found.entries
            .filter { isNewer(it.value, installedCommit, installedCommitTime) }
            .maxByOrNull { it.value.commitTime }
            ?.toPair()
}
