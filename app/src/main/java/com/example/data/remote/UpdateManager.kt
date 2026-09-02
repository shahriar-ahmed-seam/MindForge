package com.example.data.remote

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

sealed interface UpdateStatus {
    object Idle : UpdateStatus
    object Checking : UpdateStatus
    data class UpdateAvailable(
        val latestVersion: String,
        val releaseName: String,
        val changelog: String,
        val apkDownloadUrl: String,
        val apkSize: Long
    ) : UpdateStatus
    data class UpToDate(val currentVersion: String) : UpdateStatus
    data class Downloading(val progressPercent: Int) : UpdateStatus
    data class ReadyToInstall(val apkUri: Uri) : UpdateStatus
    data class Error(val message: String) : UpdateStatus
}

class UpdateManager(private val context: Context) {
    private val gitHubApi = NetworkClient.gitHubApi
    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    val currentVersionName: String = BuildConfig.VERSION_NAME
    val currentVersionCode: Int = BuildConfig.VERSION_CODE

    suspend fun checkForUpdates(repoOwner: String, repoName: String) = withContext(Dispatchers.IO) {
        _updateStatus.value = UpdateStatus.Checking
        try {
            val release = gitHubApi.getLatestRelease(repoOwner, repoName)
            val latestTag = release.tagName.removePrefix("v").trim()
            
            // Look for APK asset
            val apkAsset = release.assets?.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
            val downloadUrl = apkAsset?.downloadUrl ?: ""

            val isNewer = isVersionNewer(latestTag, currentVersionName)

            if (isNewer && downloadUrl.isNotBlank()) {
                _updateStatus.value = UpdateStatus.UpdateAvailable(
                    latestVersion = release.tagName,
                    releaseName = release.name ?: "Release ${release.tagName}",
                    changelog = release.body ?: "Bug fixes & improvements",
                    apkDownloadUrl = downloadUrl,
                    apkSize = apkAsset?.size ?: 0L
                )
            } else {
                _updateStatus.value = UpdateStatus.UpToDate(currentVersion = currentVersionName)
            }
        } catch (e: Exception) {
            _updateStatus.value = UpdateStatus.Error(
                message = e.localizedMessage ?: "Unable to connect to GitHub Releases API."
            )
        }
    }

    fun downloadAndInstallApk(downloadUrl: String, fileName: String = "MindCanvas-update.apk") {
        try {
            _updateStatus.value = UpdateStatus.Downloading(10)

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("Downloading Mind Canvas Update")
                setDescription("Fetching latest release APK")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val downloadId = downloadManager.enqueue(request)

            val onComplete = object : BroadcastReceiver() {
                override fun onReceive(ctxt: Context?, intent: Intent?) {
                    val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        try {
                            context.unregisterReceiver(this)
                        } catch (ignored: Exception) {}

                        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                        if (file.exists()) {
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            _updateStatus.value = UpdateStatus.ReadyToInstall(uri)
                            promptInstall(uri)
                        } else {
                            _updateStatus.value = UpdateStatus.Error("Downloaded APK file not found.")
                        }
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    onComplete,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                context.registerReceiver(
                    onComplete,
                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }
        } catch (e: Exception) {
            _updateStatus.value = UpdateStatus.Error("Download failed: ${e.message}")
        }
    }

    fun promptInstall(apkUri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            _updateStatus.value = UpdateStatus.Error("Failed to trigger installer: ${e.message}")
        }
    }

    private fun isVersionNewer(latest: String, current: String): Boolean {
        if (latest == current) return false
        val latestParts = latest.split(".").mapNotNull { it.filter { c -> c.isDigit() }.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.filter { c -> c.isDigit() }.toIntOrNull() }

        val length = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until length) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
