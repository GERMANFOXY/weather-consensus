package com.weatherconsensus.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.weatherconsensus.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

class AppUpdateRepository(
    private val context: Context,
    private val client: OkHttpClient,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val installedVersionName: String
        get() = installedPackageInfo().versionName ?: BuildConfig.VERSION_NAME

    val installedVersionCode: Long
        get() {
            val info = installedPackageInfo()
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
        }

    suspend fun checkForUpdate(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        if (BuildConfig.UPDATE_MANIFEST_URL.isBlank()) return@withContext null

        val request = Request.Builder()
            .url(BuildConfig.UPDATE_MANIFEST_URL)
            .cacheControl(CacheControl.FORCE_NETWORK)
            .header("Cache-Control", "no-cache")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return@withContext null

        val body = response.body?.string() ?: return@withContext null
        val manifest = json.decodeFromString<AppUpdateManifest>(body)
        if (!manifest.enabled) return@withContext null
        if (manifest.versionCode <= installedVersionCode) return@withContext null
        if (manifest.apkUrl.isBlank()) return@withContext null

        AppUpdateInfo(
            versionCode = manifest.versionCode,
            versionName = manifest.versionName,
            apkUrl = manifest.apkUrl,
            releaseNotes = manifest.releaseNotes.trim(),
        )
    }

    suspend fun downloadApk(
        apkUrl: String,
        onProgress: (Float) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(apkUrl)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Download fehlgeschlagen (${response.code})")
        }

        val body = response.body ?: throw IOException("Leere Antwort beim Download")
        val totalBytes = body.contentLength().coerceAtLeast(0L)
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(updatesDir, "weather-consensus-update.apk")
        if (target.exists()) target.delete()

        body.byteStream().use { input ->
            target.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var downloaded = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    downloaded += read
                    if (totalBytes > 0L) {
                        onProgress((downloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f))
                    }
                }
                output.flush()
            }
        }

        if (totalBytes <= 0L) onProgress(1f)
        target
    }

    fun canInstallPackages(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        return context.packageManager.canRequestPackageInstalls()
    }

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun installApk(apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun installedPackageInfo() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(
            context.packageName,
            android.content.pm.PackageManager.PackageInfoFlags.of(0),
        )
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
}
