// Author: PKQ32
// Created: 2026-11-16
// Description: 检查 GitHub 最新 Release、下载并校验更新包，交由系统安装器安装

package com.example.myapplication.update

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import com.android.apksig.ApkVerifier
import com.example.myapplication.export.ApkInstaller
import java.io.File
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 应用自身更新：
 * - 启动后自动检查（默认每天最多一次，可关闭）
 * - 手动检查
 * - 下载后校验包名、版本号与签名，再通过系统安装器由用户确认安装
 *
 * Android 不允许普通应用静默安装，因此「自动更新」止于自动检查与一键下载安装。
 */
class UpdateManager(private val app: Application) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val installedVersionName: String = runCatching {
        app.packageManager.getPackageInfo(app.packageName, 0).versionName
    }.getOrNull().orEmpty().ifBlank { "0" }

    private val installedVersionCode: Long = runCatching {
        app.packageManager.getPackageInfo(app.packageName, 0).longVersionCode
    }.getOrDefault(0L)

    private val _state = MutableStateFlow(
        UpdateUiState(
            currentVersion = installedVersionName,
            currentVersionCode = installedVersionCode,
            autoCheck = prefs.getBoolean(KEY_AUTO_CHECK, true),
        ),
    )
    val state = _state.asStateFlow()

    init {
        // 上一版本下载的安装包已无用途（安装器会自行暂存），启动时清理
        runCatching {
            File(app.cacheDir, UPDATE_DIR).listFiles()?.forEach { it.delete() }
        }
    }

    fun setAutoCheckEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CHECK, enabled).apply()
        _state.update { it.copy(autoCheck = enabled) }
    }

    /** 启动时调用；超过间隔或用户关闭自动检查时直接返回。 */
    fun maybeAutoCheck() {
        if (!_state.value.autoCheck) return
        val last = prefs.getLong(KEY_LAST_CHECK_AT, 0L)
        if (System.currentTimeMillis() - last < AUTO_CHECK_INTERVAL_MS) return
        check(auto = true)
    }

    fun checkManually() {
        check(auto = false)
    }

    fun dismissDialog() {
        _state.value.info?.let { info ->
            prefs.edit().putString(KEY_DISMISSED_VERSION, info.version).apply()
        }
        _state.update { it.copy(showDialog = false) }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun check(auto: Boolean) {
        if (_state.value.checking || _state.value.downloading) return
        scope.launch {
            _state.update { it.copy(checking = true, error = null, message = null) }
            try {
                val release = fetchLatestRelease()
                prefs.edit().putLong(KEY_LAST_CHECK_AT, System.currentTimeMillis()).apply()
                if (release == null) {
                    _state.update {
                        it.copy(
                            checking = false,
                            info = null,
                            showDialog = false,
                            message = if (auto) null else "暂无可用发布版本",
                        )
                    }
                    return@launch
                }
                if (VersionComparator.isNewer(release.version, installedVersionName)) {
                    val dismissed = prefs.getString(KEY_DISMISSED_VERSION, null)
                    _state.update {
                        it.copy(
                            checking = false,
                            info = release,
                            showDialog = !auto || dismissed != release.version,
                            error = null,
                            message = null,
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            checking = false,
                            info = null,
                            showDialog = false,
                            message = if (auto) null else "已是最新版本",
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        checking = false,
                        // 自动检查失败保持安静，只在手动检查时提示
                        error = if (auto) null else friendlyError(e),
                    )
                }
            }
        }
    }

    /** 下载并校验更新包，随后调起系统安装器。 */
    fun downloadAndInstall() {
        val info = _state.value.info ?: return
        if (_state.value.downloading) return
        if (!ApkInstaller.canRequestPackageInstalls(app)) {
            _state.update {
                it.copy(error = "请先允许应用工厂「安装未知应用」，返回后再点「立即更新」")
            }
            ApkInstaller.openUnknownSourcesSettings(app)
            return
        }
        scope.launch {
            _state.update { it.copy(downloading = true, progress = 0, error = null) }
            val dir = File(app.cacheDir, UPDATE_DIR)
            val target = File(dir, "app-update-${info.version}.apk")
            try {
                dir.mkdirs()
                dir.listFiles()?.forEach { if (it != target) it.delete() }
                download(info, target)
                _state.update { it.copy(progress = 100) }
                verifyDownloaded(target)
                _state.update {
                    it.copy(
                        downloading = false,
                        showDialog = false,
                        info = null,
                        error = null,
                        message = "更新包已下载，请按系统提示完成安装",
                    )
                }
                ApkInstaller.install(app, target)
            } catch (e: Exception) {
                target.delete()
                _state.update {
                    it.copy(
                        downloading = false,
                        error = "更新失败：${e.message ?: "未知错误"}",
                    )
                }
            }
        }
    }

    private fun fetchLatestRelease(): UpdateInfo? {
        val conn = (URL(RELEASES_API).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "APK-Factory/$installedVersionName")
        }
        try {
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_NOT_FOUND) return null
            if (code == 403 || code == 429) {
                throw IllegalStateException("GitHub 请求过于频繁，请稍后再试")
            }
            if (code !in 200..299) throw IllegalStateException("服务器返回 HTTP $code")
            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            return UpdateJson.parseRelease(body)
        } finally {
            conn.disconnect()
        }
    }

    private fun download(info: UpdateInfo, target: File) {
        val conn = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "APK-Factory/$installedVersionName")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IllegalStateException("下载失败（HTTP $code）")
            val total = conn.contentLengthLong
            var read = 0L
            conn.inputStream.use { input ->
                target.outputStream().use { out ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        read += n
                        if (total > 0) {
                            val percent = (read * 100 / total).toInt().coerceIn(0, 100)
                            if (percent != _state.value.progress) {
                                _state.update { it.copy(progress = percent) }
                            }
                        }
                    }
                }
            }
            if (total > 0 && read != total) {
                throw IllegalStateException("下载不完整，请重试")
            }
            if (info.apkSize > 0 && target.length() != info.apkSize) {
                throw IllegalStateException("安装包大小校验失败，请重试")
            }
        } finally {
            conn.disconnect()
        }
    }

    /** 包名、版本号与签名三重校验，避免安装被替换或降级的 APK。 */
    private fun verifyDownloaded(file: File) {
        val pm = app.packageManager
        val archive = pm.getPackageArchiveInfo(file.absolutePath, 0)
            ?: throw IllegalStateException("下载的安装包无法解析")
        if (archive.packageName != app.packageName) {
            throw IllegalStateException("安装包包名不匹配")
        }
        if (archive.longVersionCode <= installedVersionCode) {
            throw IllegalStateException("安装包版本号不高于当前版本")
        }

        val installed = pm.getPackageInfo(
            app.packageName,
            PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()),
        )
        val installedCerts = installed.signingInfo
            ?.apkContentsSigners
            ?.map { Base64.encodeToString(it.toByteArray(), Base64.NO_WRAP) }
            ?.toSet()
            .orEmpty()
        if (installedCerts.isEmpty()) {
            throw IllegalStateException("无法读取当前应用签名")
        }

        val result = ApkVerifier.Builder(file)
            .setMinCheckedPlatformVersion(34)
            .setMaxCheckedPlatformVersion(36)
            .build()
            .verify()
        if (!result.isVerified) {
            val reason = result.errors.firstOrNull()?.toString() ?: "签名校验未通过"
            throw IllegalStateException("安装包签名无效：$reason")
        }
        val downloadedCerts = result.signerCertificates
            .map { Base64.encodeToString(it.encoded, Base64.NO_WRAP) }
            .toSet()
        if (downloadedCerts != installedCerts) {
            throw IllegalStateException("安装包签名与应用不一致，请从 GitHub Releases 重新下载")
        }
    }

    private fun friendlyError(e: Exception): String = when (e) {
        is UnknownHostException -> "无法连接 GitHub，请检查网络后重试"
        is SocketTimeoutException -> "连接 GitHub 超时，请稍后重试"
        else -> "检查更新失败：${e.message ?: "未知错误"}"
    }

    companion object {
        private const val RELEASES_API =
            "https://api.github.com/repos/QINGYUAI/APK_Factory/releases/latest"
        private const val PREFS_NAME = "app_update"
        private const val KEY_LAST_CHECK_AT = "last_check_at"
        private const val KEY_DISMISSED_VERSION = "dismissed_version"
        private const val KEY_AUTO_CHECK = "auto_check"
        private const val AUTO_CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000
        private const val UPDATE_DIR = "updates"
    }
}
