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
}
