package com.parthjivani.ledgerly.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.parthjivani.ledgerly.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class GitHubAsset(
    @param:Json(name = "name") val name: String,
    @param:Json(name = "browser_download_url") val browserDownloadUrl: String,
    @param:Json(name = "size") val size: Long? = 0
)

data class GitHubRelease(
    @param:Json(name = "tag_name") val tagName: String,
    @param:Json(name = "name") val name: String?,
    @param:Json(name = "body") val body: String?,
    @param:Json(name = "published_at") val publishedAt: String?,
    @param:Json(name = "assets") val assets: List<GitHubAsset>?
)

data class AppUpdateInfo(
    val latestVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val isUpdateAvailable: Boolean,
    val publishedAt: String? = null
)

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Float, val bytesDownloaded: Long, val totalBytes: Long) : DownloadState()
    data class ReadyToInstall(val apkFile: File) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class UpdateManager(
    private val repoOwner: String = "JivaniParth",
    private val repoName: String = "Ledgerly"
) {
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState

    suspend fun checkForUpdates(): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Ledgerly-Android")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("GitHub API returned HTTP ${response.code}"))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty release response"))
            val adapter = moshi.adapter(GitHubRelease::class.java)
            val release = adapter.fromJson(body) ?: return@withContext Result.failure(Exception("Failed to parse release information"))

            val currentVersion = BuildConfig.VERSION_NAME
            val rawTag = release.tagName.trim()
            val cleanRemoteVersion = rawTag.removePrefix("v").removePrefix("V")
            val isNewer = isVersionNewer(cleanRemoteVersion, currentVersion)

            val apkAsset = release.assets?.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
            val downloadUrl = apkAsset?.browserDownloadUrl ?: ""

            val info = AppUpdateInfo(
                latestVersion = rawTag,
                releaseTitle = release.name ?: "Version $rawTag",
                releaseNotes = release.body?.takeIf { it.isNotBlank() } ?: "Bug fixes and performance improvements.",
                downloadUrl = downloadUrl,
                isUpdateAvailable = isNewer && downloadUrl.isNotBlank(),
                publishedAt = release.publishedAt
            )
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isVersionNewer(remote: String, current: String): Boolean {
        try {
            val rParts = remote.split("-")[0].split(".").mapNotNull { it.toIntOrNull() }
            val cParts = current.split("-")[0].split(".").mapNotNull { it.toIntOrNull() }
            val maxLength = maxOf(rParts.size, cParts.size)
            for (i in 0 until maxLength) {
                val r = rParts.getOrElse(i) { 0 }
                val c = cParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        } catch (e: Exception) {
            return remote != current
        }
    }

    suspend fun downloadUpdate(context: Context, downloadUrl: String): File? = withContext(Dispatchers.IO) {
        _downloadState.value = DownloadState.Downloading(0f, 0, 0)
        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "Ledgerly-Android")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                _downloadState.value = DownloadState.Error("Server error HTTP ${response.code}")
                return@withContext null
            }

            val body = response.body ?: run {
                _downloadState.value = DownloadState.Error("Empty download stream")
                return@withContext null
            }

            val contentLength = body.contentLength()
            val downloadDir = context.getExternalFilesDir(null) ?: context.cacheDir
            val updateFile = File(downloadDir, "ledgerly_update.apk")
            if (updateFile.exists()) {
                updateFile.delete()
            }

            body.byteStream().use { input ->
                FileOutputStream(updateFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        val progress = if (contentLength > 0) totalRead.toFloat() / contentLength else 0f
                        _downloadState.value = DownloadState.Downloading(progress, totalRead, contentLength)
                    }
                    output.flush()
                }
            }

            _downloadState.value = DownloadState.ReadyToInstall(updateFile)
            updateFile
        } catch (e: Exception) {
            _downloadState.value = DownloadState.Error(e.localizedMessage ?: "Download failed")
            null
        }
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    return
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            _downloadState.value = DownloadState.Error("Failed to launch installer: ${e.localizedMessage}")
        }
    }

    fun resetState() {
        _downloadState.value = DownloadState.Idle
    }
}
