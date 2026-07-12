// Author: PKQ32
// Created: 2026-07-12
// Description: 媒体文件 URI 持久化授权与显示名解析

package com.example.myapplication.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns

object MediaUriHelper {

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
}
