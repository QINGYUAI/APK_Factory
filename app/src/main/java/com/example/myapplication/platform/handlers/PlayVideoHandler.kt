// Author: PKQ32
// Created: 2026-07-12
// Description: 播放视频：支持私有文件、导出 asset: 与 content URI（经 FileProvider 授权）

package com.example.myapplication.platform.handlers

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.myapplication.domain.ActionHandler
import com.example.myapplication.domain.ActionResult
import com.example.myapplication.domain.model.ActionTypes
import com.example.myapplication.platform.MediaUriHelper
import com.example.myapplication.platform.PlatformActionContext
import java.io.File
import java.io.IOException

class PlayVideoHandler(
    private val ctx: PlatformActionContext,
) : ActionHandler {
    override val type = ActionTypes.PLAY_VIDEO

    override fun validate(params: Map<String, String>): String? {
        if (params["uri"].isNullOrBlank() && params["path"].isNullOrBlank()) {
            return "请先选择视频文件"
        }
        return null
    }

    override suspend fun execute(params: Map<String, String>): ActionResult {
        val uriParam = params["uri"]?.takeIf { it.isNotBlank() }
        val pathParam = params["path"]?.takeIf { it.isNotBlank() }
        return try {
            val dataUri = resolvePlayUri(uriParam, pathParam)
                ?: return ActionResult.Failed("INVALID_PARAMS", "请先选择视频文件")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(dataUri, "video/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ctx.appContext.startActivity(intent)
            ActionResult.Success
        } catch (e: SecurityException) {
            ActionResult.Failed(
                "NO_PERMISSION",
                "无法读取视频（权限不足）。请在工厂中重新选择文件后再导出",
            )
        } catch (e: IOException) {
            ActionResult.Failed("FILE_NOT_FOUND", "无法打开视频，请重新选择文件")
        } catch (e: Exception) {
            ActionResult.Failed("PLAY_FAILED", e.message ?: "无法打开视频播放器")
        }
    }

    private fun resolvePlayUri(uriParam: String?, pathParam: String?): Uri? {
        when {
            pathParam != null && MediaUriHelper.isAssetPath(pathParam) -> {
                val asset = MediaUriHelper.assetPathOf(pathParam)
                val cacheDir = File(ctx.appContext.cacheDir, "media-play").apply { mkdirs() }
                val out = File(cacheDir, File(asset).name)
                ctx.appContext.assets.open(asset).use { input ->
                    out.outputStream().use { output -> input.copyTo(output) }
                }
                return fileProviderUri(out)
            }
            pathParam != null -> {
                val file = File(pathParam)
                if (file.exists() && file.isFile) {
                    return fileProviderUri(file)
                }
                if (uriParam != null) return Uri.parse(uriParam)
                return null
            }
            uriParam != null -> return Uri.parse(uriParam)
            else -> return null
        }
    }

    private fun fileProviderUri(file: File): Uri {
        val authority = "${ctx.appContext.packageName}.fileprovider"
        return FileProvider.getUriForFile(ctx.appContext, authority, file)
    }
}
