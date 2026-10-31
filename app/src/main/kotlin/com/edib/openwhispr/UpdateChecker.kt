package com.edib.openwhispr

import android.content.Context
import android.content.SharedPreferences
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.DigestInputStream
import java.security.MessageDigest

/** Lightweight in-app "new version available" check against this repo's
 * GitHub Releases, plus the download half of an in-app update: fetches the
 * release's .apk asset directly so the install flow never has to leave the
 * app for a browser. No backend involved -- just the public GitHub API. */
object UpdateChecker {
    // [security] apkSha256 carries the checksum GitHub publishes for the
    // release's .apk asset, so downloadApk can verify the file before it is
    // handed to the installer.
    data class UpdateInfo(
        val version: String,
        val url: String,
        val apkUrl: String?,
        val notes: String?,
        val apkSha256: String? = null
    )

    private val client = OkHttpClient()
    private const val CHECK_INTERVAL_MS = 12 * 60 * 60 * 1000L // don't hammer GitHub on every app open
    // The personal APKs are tagged gboard-dock-main-N and deliberately marked
    // as prereleases. GitHub /releases/latest ignores them, so inspect the list.
    private const val RELEASES_URL =
        "https://api.github.com/repos/Tylarcus/OpenWhispr/releases?per_page=100"
    private const val DOCK_VERSION_CODE_BASE = 40000L
    private val DOCK_RELEASE_TAG = Regex("^gboard-dock-main-(\\d+)$")

    data class CheckResult(val update: UpdateInfo?, val error: String? = null)

    fun dockBuildNumber(tag: String): Int? =
        DOCK_RELEASE_TAG.matchEntire(tag)?.groupValues?.get(1)?.toIntOrNull()

    fun isNewerDockBuild(tag: String, installedVersionCode: Long): Boolean =
        dockBuildNumber(tag)?.let { DOCK_VERSION_CODE_BASE + it > installedVersionCode } ?: false

    // Matches the <!--WHATS_NEW_START-->...<!--WHATS_NEW_END--> block the
    // release workflow wraps around that version's CHANGELOG.md section, so
    // the update dialog can show a short "what's new" instead of just a
    // version number.
    private val WHATS_NEW_REGEX = Regex(
        "<!--WHATS_NEW_START-->(.*?)<!--WHATS_NEW_END-->",
        RegexOption.DOT_MATCHES_ALL
    )

    /** True if [latest] (e.g. "v3.2.0") is a strictly newer version than
     * [current] (e.g. "3.1.1"). Compares numeric dot-separated parts. */
    fun isNewer(latest: String, current: String): Boolean {
        fun parts(v: String) = v.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val l = parts(latest)
        val c = parts(current)
        for (i in 0 until maxOf(l.size, c.size)) {
            val lv = l.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (lv != cv) return lv > cv
        }
        return false
    }

    // [security] GitHub's release API reports each asset's checksum as
    // "digest": "sha256:<64 hex chars>". Returns the lowercase hex, or null
    // if the field is missing or isn't a well-formed SHA-256.
    private val SHA256_HEX = Regex("^[0-9a-f]{64}$")

    fun parseSha256Digest(raw: String?): String? {
        val hex = raw?.trim()?.lowercase()?.removePrefix("sha256:") ?: return null
        return hex.takeIf { SHA256_HEX.matches(it) }
    }

    /** [security] True only if a checksum was published AND the downloaded
     * file's checksum matches it. A missing checksum counts as a failure,
     * so an unverifiable file is never installed. */
    fun checksumMatches(expectedSha256: String?, actualSha256: String): Boolean =
        expectedSha256 != null && expectedSha256.equals(actualSha256, ignoreCase = true)

    /** Checks this fork's independently signed main-build releases, including
     * prereleases. Uses the APK's monotonically increasing versionCode rather
     * than its constant display version (4.0.0-docktest).
     * Never mistakes a network failure for "up to date". */
    fun checkForUpdate(
        prefs: SharedPreferences,
        installedVersionCode: Long,
        force: Boolean = false,
        callback: (CheckResult) -> Unit
    ) {
        val now = System.currentTimeMillis()
        val lastCheck = prefs.getLong("dock_last_update_check", 0L)
        val cachedBuild = prefs.getInt("dock_cached_build", -1)
        val cachedUrl = prefs.getString("dock_cached_url", null)
        val cachedApkUrl = prefs.getString("dock_cached_apk_url", null)
        val cachedNotes = prefs.getString("dock_cached_notes", null)
        val cachedSha = prefs.getString("dock_cached_sha256", null)

        fun cachedResult(): UpdateInfo? =
            if (cachedBuild >= 0 &&
                DOCK_VERSION_CODE_BASE + cachedBuild > installedVersionCode &&
                !cachedUrl.isNullOrBlank() && !cachedApkUrl.isNullOrBlank() &&
                cachedSha != null
            ) {
                UpdateInfo("4.0.0-docktest build #$cachedBuild", cachedUrl, cachedApkUrl, cachedNotes, cachedSha)
            } else null

        if (!force && now - lastCheck < CHECK_INTERVAL_MS) {
            callback(CheckResult(cachedResult()))
            return
        }

        val request = Request.Builder()
            .url(RELEASES_URL)
            .header("Accept", "application/vnd.github+json")
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(CheckResult(cachedResult(), "Could not reach GitHub: ${e.message ?: "network error"}"))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        callback(CheckResult(cachedResult(), "GitHub update check returned HTTP ${it.code}."))
                        return
                    }
                    try {
                        val releases = JSONArray(it.body?.string().orEmpty())
                        var newestBuild = -1
                        var newestInfo: UpdateInfo? = null
                        for (i in 0 until releases.length()) {
                            val release = releases.optJSONObject(i) ?: continue
                            if (release.optBoolean("draft", false)) continue
                            val build = dockBuildNumber(release.optString("tag_name", "")) ?: continue
                            if (build <= newestBuild) continue
                            val assets = release.optJSONArray("assets") ?: continue
                            var apkUrl: String? = null
                            var digest: String? = null
                            for (j in 0 until assets.length()) {
                                val asset = assets.optJSONObject(j) ?: continue
                                if (!asset.optString("name", "").endsWith(".apk")) continue
                                val sha = parseSha256Digest(asset.optString("digest", null))
                                val downloadUrl = asset.optString("browser_download_url", "")
                                if (sha == null || !downloadUrl.startsWith(
                                        "https://github.com/Tylarcus/OpenWhispr/releases/download/"
                                    )) continue
                                apkUrl = downloadUrl
                                digest = sha
                                break
                            }
                            if (apkUrl == null || digest == null) continue
                            val notesRaw = release.optString("body", "")
                            val notes = WHATS_NEW_REGEX.find(notesRaw)?.groupValues?.get(1)?.trim()
                                ?.takeIf { it.isNotBlank() }
                                ?: notesRaw.take(900).takeIf { it.isNotBlank() }
                            val url = release.optString("html_url", "")
                            if (!url.startsWith("https://github.com/Tylarcus/OpenWhispr/releases/tag/")) continue
                            newestBuild = build
                            newestInfo = UpdateInfo(
                                "4.0.0-docktest build #$build", url, apkUrl, notes, digest
                            )
                        }

                        if (newestInfo == null) {
                            callback(CheckResult(null,
                                "No verified signed main-branch APK releases were found in this fork."))
                            return
                        }
                        prefs.edit()
                            .putLong("dock_last_update_check", now)
                            .putInt("dock_cached_build", newestBuild)
                            .putString("dock_cached_url", newestInfo.url)
                            .putString("dock_cached_apk_url", newestInfo.apkUrl)
                            .putString("dock_cached_notes", newestInfo.notes)
                            .putString("dock_cached_sha256", newestInfo.apkSha256)
                            .apply()
                        callback(CheckResult(if (DOCK_VERSION_CODE_BASE + newestBuild > installedVersionCode)
                            newestInfo else null))
                    } catch (e: Exception) {
                        callback(CheckResult(cachedResult(),
                            "Couldn't read GitHub releases: ${e.message ?: "invalid response"}"))
                    }
                }
            }
        })
    }

    /** Downloads [apkUrl] into the app's private cache dir and calls back
     * with the resulting file, or null + an error message on failure.
     * Runs on a background thread (OkHttp's own dispatcher); the caller is
     * responsible for hopping back to the UI thread before touching views.
     *
     * [security] The file is hashed while it downloads and only returned if
     * its SHA-256 matches [expectedSha256] (GitHub's published checksum for
     * the asset). On a mismatch, or if no checksum was published, the file is
     * deleted and an error returned, so it never reaches the installer. This
     * catches corrupted or swapped downloads; it cannot protect against a
     * malicious release published by the repo owner.
     */
    fun downloadApk(
        context: Context,
        apkUrl: String,
        expectedSha256: String?,
        callback: (File?, String?) -> Unit
    ) {
        val request = Request.Builder().url(apkUrl).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    callback(null, "HTTP ${response.code}")
                    return
                }
                try {
                    val body = response.body
                    if (body == null) {
                        callback(null, "Empty response body")
                        return
                    }
                    val dir = File(context.cacheDir, "updates").apply { mkdirs() }
                    val file = File(dir, "openwhispr-update.apk")
                    // [security] Hash while copying (no second pass over the file).
                    val digest = MessageDigest.getInstance("SHA-256")
                    DigestInputStream(body.byteStream(), digest).use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                    val actualSha256 = digest.digest().joinToString("") { "%02x".format(it) }
                    if (!checksumMatches(expectedSha256, actualSha256)) {
                        file.delete()
                        callback(
                            null,
                            if (expectedSha256 == null) "No checksum available for this download"
                            else "Checksum mismatch"
                        )
                        return
                    }
                    callback(file, null)
                } catch (e: Exception) {
                    callback(null, e.message)
                }
            }
        })
    }
}
