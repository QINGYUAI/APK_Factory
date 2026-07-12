// Author: PKQ32
// Created: 2026-07-12
// Description: 导出前将本地媒体嵌入 APK assets，并改写步骤 path 为 asset: 引用

package com.example.myapplication.export

import android.content.Context
import com.example.myapplication.domain.model.ActionTypes
import com.example.myapplication.domain.model.MiniApp
import com.example.myapplication.platform.MediaUriHelper
import java.io.File
import java.util.UUID

data class PreparedMediaExport(
    val miniApp: MiniApp,
    /** APK 内路径（含 assets/ 前缀）→ 文件字节 */
    val assetFiles: Map<String, ByteArray>,
)

object MediaExportHelper {

    fun prepare(context: Context, miniApp: MiniApp): PreparedMediaExport {
        val assets = linkedMapOf<String, ByteArray>()
        val steps = miniApp.steps.map { step ->
            if (step.type != ActionTypes.PLAY_MUSIC && step.type != ActionTypes.PLAY_VIDEO) {
                return@map step
            }
            val params = step.params.toMutableMap()
            val local = resolveLocalFile(context, params) ?: return@map step
            val ext = local.extension.ifBlank { if (step.type == ActionTypes.PLAY_MUSIC) "mp3" else "mp4" }
            val assetRel = "media/${step.id}_${UUID.randomUUID().toString().take(8)}.$ext"
            assets["assets/$assetRel"] = local.readBytes()
            params["path"] = MediaUriHelper.ASSET_PREFIX + assetRel
            params["uri"] = ""
            if (params["displayName"].isNullOrBlank()) {
                params["displayName"] = local.name
            }
            step.copy(params = params)
        }
        return PreparedMediaExport(
            miniApp = miniApp.copy(steps = steps),
            assetFiles = assets,
        )
    }

    private fun resolveLocalFile(context: Context, params: Map<String, String>): File? {
        val path = params["path"]?.takeIf { it.isNotBlank() }
        if (path != null && !MediaUriHelper.isAssetPath(path)) {
            val file = File(path)
            if (file.exists() && file.isFile && file.length() > 0L) return file
        }
        val uri = params["uri"]?.takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            MediaUriHelper.copyToPrivateMedia(context, android.net.Uri.parse(uri))
        }.getOrNull()
    }
}
