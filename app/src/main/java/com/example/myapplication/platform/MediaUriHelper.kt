// Author: PKQ32
// Created: 2026-07-12
// Description: 媒体文件 URI 持久化、复制到私有目录，供工厂播放与导出嵌入

package com.example.myapplication.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.IOException
import java.util.UUID

object MediaUriHelper {

    const val ASSET_PREFIX = "asset:"
    /** 同步到独立小应用私有目录后的相对媒体路径前缀 */
    const val SYNC_PREFIX = "sync:"
    private const val SYNC_ROOT = "synced_config"

    /**
     * 对 SAF 选取的 URI 申请持久读权限，便于重启后仍可播放。
     */
    fun takePersistableRead(context: Context, uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (_: SecurityException) {
            // 部分来源不支持持久化，运行时仍可用临时权限
        }
    }

    /**
     * 将所选媒体复制到应用私有目录。
     * 导出 APK 时再嵌入 assets，避免 content URI 权限无法跨应用转移。
     */
    fun copyToPrivateMedia(context: Context, uri: Uri): File {
        val name = displayName(context, uri.toString())
            ?.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            ?.ifBlank { null }
            ?: "media_${UUID.randomUUID()}"
        val dir = File(context.filesDir, "media").apply { mkdirs() }
        val target = File(dir, "${UUID.randomUUID()}_$name")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IOException("无法读取所选文件")
        if (!target.exists() || target.length() <= 0L) {
            target.delete()
            throw IOException("复制媒体文件失败")
        }
        return target
    }

    fun displayName(context: Context, uriString: String): String? {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return null
        if (uri.scheme == "file") {
            return uri.lastPathSegment
        }
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else {
                        null
                    }
                }
        }.getOrNull() ?: uri.lastPathSegment
    }

    fun isAssetPath(path: String): Boolean = path.startsWith(ASSET_PREFIX)

    fun assetPathOf(path: String): String = path.removePrefix(ASSET_PREFIX)

    fun isSyncPath(path: String): Boolean = path.startsWith(SYNC_PREFIX)

    fun syncRelativeOf(path: String): String = path.removePrefix(SYNC_PREFIX)

    fun resolveSyncFile(context: Context, path: String): File {
        val rel = syncRelativeOf(path).trimStart('/', '\\')
        require(rel.isNotBlank() && !rel.contains("..")) { "非法同步媒体路径" }
        return File(File(context.filesDir, SYNC_ROOT), rel)
    }

    /**
     * 解析可播放的本地文件：sync: / asset: / 绝对路径。
     */
    fun resolvePlayableFile(context: Context, path: String): File? {
        return when {
            isSyncPath(path) -> {
                val f = resolveSyncFile(context, path)
                f.takeIf { it.exists() && it.isFile && it.length() > 0L }
            }
            isAssetPath(path) -> runCatching { extractAssetToCache(context, path) }.getOrNull()
            else -> {
                val f = File(path)
                f.takeIf { it.exists() && it.isFile && it.length() > 0L }
            }
        }
    }

    /**
     * 将 assets 内媒体复制到缓存文件。
     * 压缩的 APK entry 无法 openFd，统一走文件路径更稳妥。
     */
    fun extractAssetToCache(context: Context, assetPath: String): File {
        val rel = assetPathOf(assetPath).ifBlank { assetPath }
        val dir = File(context.cacheDir, "media-play").apply { mkdirs() }
        val name = File(rel).name.ifBlank { "media_${UUID.randomUUID()}" }
        val out = File(dir, name)
        context.assets.open(rel).use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        }
        if (!out.exists() || out.length() <= 0L) {
            out.delete()
            throw IOException("无法读取内嵌音频：$rel")
        }
        return out
    }
}
