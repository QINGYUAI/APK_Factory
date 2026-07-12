// Author: PKQ32
// Created: 2026-07-12
// Description: 播放视频：打开应用内播放页，支持暂停/停止与通知栏

package com.example.myapplication.platform.handlers

import android.content.Intent
import android.net.Uri
import com.example.myapplication.domain.ActionHandler
import com.example.myapplication.domain.ActionResult
import com.example.myapplication.domain.model.ActionTypes
import com.example.myapplication.platform.MediaUriHelper
import com.example.myapplication.platform.PlatformActionContext
import com.example.myapplication.platform.media.MediaPlaybackController
import com.example.myapplication.platform.media.VideoPlaybackActivity
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
        val title = params["displayName"]?.takeIf { it.isNotBlank() } ?: "视频播放"
        return try {
            MediaPlaybackController.init(ctx.appContext)
            val dataUri = resolvePlayUri(uriParam, pathParam)
                ?: return ActionResult.Failed("INVALID_PARAMS", "请先选择视频文件")
            val intent = Intent(ctx.appContext, VideoPlaybackActivity::class.java).apply {
                data = dataUri
                putExtra(VideoPlaybackActivity.EXTRA_TITLE, title)
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
            pathParam != null -> {
                val file = MediaUriHelper.resolvePlayableFile(ctx.appContext, pathParam)
                if (file != null) return fileProviderUri(file)
                if (uriParam != null) return Uri.parse(uriParam)
                return null
            }
            uriParam != null -> return Uri.parse(uriParam)
            else -> return null
        }
    }

    private fun fileProviderUri(file: File): Uri {
        val authority = "${ctx.appContext.packageName}.fileprovider"
        return androidx.core.content.FileProvider.getUriForFile(ctx.appContext, authority, file)
    }
}
