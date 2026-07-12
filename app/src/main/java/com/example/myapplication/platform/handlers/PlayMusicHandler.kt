// Author: PKQ32
// Created: 2026-07-11
// Description: 播放本地音频：走全局控制器，支持暂停/停止与通知栏

package com.example.myapplication.platform.handlers

import android.net.Uri
import com.example.myapplication.domain.ActionHandler
import com.example.myapplication.domain.ActionResult
import com.example.myapplication.domain.model.ActionTypes
import com.example.myapplication.platform.MediaUriHelper
import com.example.myapplication.platform.PlatformActionContext
import com.example.myapplication.platform.media.MediaPlaybackController
import java.io.IOException

class PlayMusicHandler(
    private val ctx: PlatformActionContext,
) : ActionHandler {
    override val type = ActionTypes.PLAY_MUSIC

    override fun validate(params: Map<String, String>): String? {
        val uri = params["uri"]
        val path = params["path"]
        if (uri.isNullOrBlank() && path.isNullOrBlank()) {
            return "请先选择音频文件"
        }
        return null
    }

    override suspend fun execute(params: Map<String, String>): ActionResult {
        val uriParam = params["uri"]?.takeIf { it.isNotBlank() }
        val pathParam = params["path"]?.takeIf { it.isNotBlank() }
        if (uriParam == null && pathParam == null) {
            return ActionResult.Failed("INVALID_PARAMS", "请先选择音频文件")
        }
        val title = params["displayName"]?.takeIf { it.isNotBlank() } ?: "音乐播放"

        return try {
            MediaPlaybackController.init(ctx.appContext)
            val filePath = resolveLocalPath(pathParam, uriParam)
                ?: return ActionResult.Failed(
                    "FILE_NOT_FOUND",
                    "找不到音频文件。请在工厂中重新选择并导出",
                )
            MediaPlaybackController.playAudio(filePath, title)
            ActionResult.Success
        } catch (e: SecurityException) {
            ActionResult.Failed(
                "NO_PERMISSION",
                "无法读取音频（权限不足）。请在工厂中重新选择文件后再导出",
            )
        } catch (e: IOException) {
            ActionResult.Failed(
                "FILE_NOT_FOUND",
                e.message?.takeIf { it.isNotBlank() }
                    ?: "无法打开音频。请在工厂中重新选择文件后再次导出",
            )
        } catch (e: IllegalArgumentException) {
            ActionResult.Failed("INVALID_SOURCE", "无效的音频来源")
        } catch (e: Exception) {
            ActionResult.Failed("PLAY_FAILED", e.message ?: "播放失败")
        }
    }

    private fun resolveLocalPath(pathParam: String?, uriParam: String?): String? {
        if (pathParam != null) {
            MediaUriHelper.resolvePlayableFile(ctx.appContext, pathParam)?.let {
                return it.absolutePath
            }
        }
        if (uriParam != null) {
            val copied = MediaUriHelper.copyToPrivateMedia(ctx.appContext, Uri.parse(uriParam))
            return copied.absolutePath
        }
        return null
    }
}
