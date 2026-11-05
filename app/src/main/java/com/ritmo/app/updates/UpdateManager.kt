package com.ritmo.app.updates

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.ritmo.app.BuildConfig
import com.ritmo.app.MainActivity
import com.ritmo.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class UpdateInfo(val version: String, val apkUrl: String, val pageUrl: String)
sealed interface UpdateState { data object Idle : UpdateState; data object Checking : UpdateState; data object UpToDate : UpdateState; data class Available(val info: UpdateInfo) : UpdateState; data object Downloading : UpdateState; data class Error(val message: String) : UpdateState }

@Singleton
class UpdateManager @Inject constructor(@ApplicationContext private val context: Context) {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state = _state.asStateFlow()

    suspend fun check(notify: Boolean = false) = withContext(Dispatchers.IO) {
        _state.value = UpdateState.Checking
        runCatching {
            val connection = URL(LATEST_RELEASE).openConnection() as HttpURLConnection
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "Ritmo/${BuildConfig.VERSION_NAME}")
            connection.connectTimeout = 10_000; connection.readTimeout = 15_000
            val json = connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
            val tag = json.getString("tag_name").removePrefix("v")
            val assets = json.getJSONArray("assets")
            val apk = (0 until assets.length()).map { assets.getJSONObject(it) }.firstOrNull { it.optString("name").endsWith(".apk", true) }?.getString("browser_download_url")
            if (apk != null && isNewer(tag, BuildConfig.VERSION_NAME)) UpdateInfo(tag, apk, json.getString("html_url")) else null
        }.onSuccess { info ->
            _state.value = if (info == null) UpdateState.UpToDate else UpdateState.Available(info)
            if (notify && info != null) notify(info)
        }.onFailure { _state.value = UpdateState.Error(it.message ?: "Не удалось проверить обновления") }
    }

    suspend fun downloadAndInstall(info: UpdateInfo) = withContext(Dispatchers.IO) {
        if (android.os.Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            _state.value = UpdateState.Available(info)
            return@withContext
        }
        _state.value = UpdateState.Downloading
        runCatching {
            val target = File(context.cacheDir, "updates/ritmo-${info.version}.apk").apply { parentFile?.mkdirs() }
            val connection = URL(info.apkUrl).openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "Ritmo/${BuildConfig.VERSION_NAME}")
            connection.connectTimeout = 15_000; connection.readTimeout = 60_000
            connection.inputStream.use { input -> target.outputStream().use(input::copyTo) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", target)
            context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION))
        }.onFailure { _state.value = UpdateState.Error(it.message ?: "Не удалось скачать обновление") }
    }

    private fun notify(info: UpdateInfo) {
        if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Обновления Ritmo", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(context, 9021, Intent(context, MainActivity::class.java).setData(Uri.parse("ritmo://updates")).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(9021, NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_notification).setContentTitle("Доступна Ritmo ${info.version}").setContentText("Нажми, чтобы обновить приложение").setContentIntent(open).setAutoCancel(true).build())
    }

    private fun isNewer(remote: String, local: String): Boolean {
        val left = remote.split('.', '-', '+').map { it.toIntOrNull() ?: 0 }; val right = local.split('.', '-', '+').map { it.toIntOrNull() ?: 0 }
        return (0 until maxOf(left.size, right.size)).firstNotNullOfOrNull { i -> ((left.getOrElse(i) { 0 }).compareTo(right.getOrElse(i) { 0 })).takeIf { it != 0 } }?.let { it > 0 } ?: false
    }

    companion object { private const val LATEST_RELEASE = "https://api.github.com/repos/Euvlos/Ritmo/releases/latest"; private const val CHANNEL = "app_updates" }
}
