package com.example.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val latestVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val apkSize: Long
)

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class Available(val updateInfo: UpdateInfo) : UpdateStatus()
    object UpToDate : UpdateStatus()
    data class Downloading(
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val remainingBytes: Long,
        val speedBytesPerSec: Long
    ) : UpdateStatus()
    data class ReadyToInstall(val apkFile: File) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

class AppUpdateManager(private val context: Context) {

    private val tag = "AppUpdateManager"
    private val repoOwner = "zxiu86"
    private val repoName = "ZiosTV"
    private val githubApiUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val _status = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val status: StateFlow<UpdateStatus> = _status.asStateFlow()

    private var isCancelled = false

    suspend fun checkForUpdates(isManualCheck: Boolean = false) {
        withContext(Dispatchers.IO) {
            try {
                _status.value = UpdateStatus.Checking
                val request = Request.Builder()
                    .url(githubApiUrl)
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "SoualJawab-Android-App")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    val msg = "فشل التحقق من التحديثات (${response.code})"
                    Log.w(tag, msg)
                    if (isManualCheck) {
                        _status.value = UpdateStatus.Error(msg)
                    } else {
                        _status.value = UpdateStatus.Idle
                    }
                    return@withContext
                }

                val responseBody = response.body?.string() ?: ""
                val json = JSONObject(responseBody)

                val tagName = json.optString("tag_name", "").removePrefix("v").trim()
                val releaseTitle = json.optString("name", "إصدار جديد")
                val releaseNotes = json.optString("body", "تحديث جديد يتضمن تحسينات ومزايا جديدة.")

                // Look for an APK asset
                var apkUrl: String? = null
                var apkSize: Long = 0
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url")
                            apkSize = asset.optLong("size", 0L)
                            break
                        }
                    }
                }

                val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v").trim()
                Log.d(tag, "Current version: $currentVersion, Latest GitHub release: $tagName")

                if (apkUrl != null && isNewerVersion(currentVersion, tagName)) {
                    val updateInfo = UpdateInfo(
                        latestVersion = tagName,
                        releaseTitle = releaseTitle,
                        releaseNotes = releaseNotes,
                        apkDownloadUrl = apkUrl,
                        apkSize = apkSize
                    )
                    _status.value = UpdateStatus.Available(updateInfo)
                } else {
                    Log.d(tag, "App is up to date ($currentVersion)")
                    _status.value = if (isManualCheck) UpdateStatus.UpToDate else UpdateStatus.Idle
                }
            } catch (e: Exception) {
                Log.e(tag, "Check for update error: ${e.message}")
                if (isManualCheck) {
                    _status.value = UpdateStatus.Error("تعذر الاتصال بخادم التحديثات: ${e.message}")
                } else {
                    _status.value = UpdateStatus.Idle
                }
            }
        }
    }

    suspend fun startDownload(updateInfo: UpdateInfo) {
        isCancelled = false
        withContext(Dispatchers.IO) {
            try {
                val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
                val apkFile = File(targetDir, "SoualJawab-${updateInfo.latestVersion}.apk")
                if (apkFile.exists()) {
                    apkFile.delete()
                }

                val request = Request.Builder()
                    .url(updateInfo.apkDownloadUrl)
                    .header("User-Agent", "SoualJawab-Android-App")
                    .build()

                val response = httpClient.newCall(request).execute()
                val body = response.body
                if (!response.isSuccessful || body == null) {
                    _status.value = UpdateStatus.Error("فشل بدء التنزيل من السيرفر (${response.code})")
                    return@withContext
                }

                val totalLength = if (updateInfo.apkSize > 0) updateInfo.apkSize else body.contentLength()
                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(apkFile)

                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                var totalBytesRead = 0L

                var lastSpeedCalcTime = System.currentTimeMillis()
                var bytesSinceLastCalc = 0L
                var currentSpeed = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (isCancelled) {
                        outputStream.close()
                        inputStream.close()
                        apkFile.delete()
                        _status.value = UpdateStatus.Idle
                        return@withContext
                    }

                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    bytesSinceLastCalc += bytesRead

                    val now = System.currentTimeMillis()
                    val elapsed = now - lastSpeedCalcTime
                    if (elapsed >= 500) {
                        currentSpeed = (bytesSinceLastCalc * 1000) / elapsed
                        lastSpeedCalcTime = now
                        bytesSinceLastCalc = 0
                    }

                    val progress = if (totalLength > 0) {
                        (totalBytesRead.toFloat() / totalLength.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    val remainingBytes = if (totalLength > totalBytesRead) totalLength - totalBytesRead else 0L

                    _status.value = UpdateStatus.Downloading(
                        progress = progress,
                        downloadedBytes = totalBytesRead,
                        totalBytes = totalLength,
                        remainingBytes = remainingBytes,
                        speedBytesPerSec = currentSpeed
                    )
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                Log.d(tag, "Download completed: ${apkFile.absolutePath} (${apkFile.length()} bytes)")
                _status.value = UpdateStatus.ReadyToInstall(apkFile)

                // Launch system installer automatically
                installApk(apkFile)
            } catch (e: Exception) {
                Log.e(tag, "Download failed: ${e.message}")
                _status.value = UpdateStatus.Error("انقطع التنزيل: ${e.message}")
            }
        }
    }

    fun cancelDownload() {
        isCancelled = true
        _status.value = UpdateStatus.Idle
    }

    fun dismissUpdate() {
        _status.value = UpdateStatus.Idle
    }

    fun installApk(file: File) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(tag, "Error launching package installer: ${e.message}")
            _status.value = UpdateStatus.Error("تعذر فتح مثبت الحزم: ${e.message}")
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        try {
            val currParts = current.split(".").mapNotNull { it.toIntOrNull() }
            val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }

            val maxLen = maxOf(currParts.size, latestParts.size)
            for (i in 0 until maxLen) {
                val c = currParts.getOrElse(i) { 0 }
                val l = latestParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
            return false
        } catch (_: Exception) {
            return latest != current
        }
    }
}
