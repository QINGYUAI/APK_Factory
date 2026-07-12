// Author: PKQ32
// Created: 2026-07-12
// Description: 播放视频动作：用系统播放器打开已选视频 URI

package com.example.myapplication.platform.handlers

import android.content.Intent
import android.net.Uri
import com.example.myapplication.domain.ActionHandler
import com.example.myapplication.domain.ActionResult
import com.example.myapplication.domain.model.ActionTypes
import com.example.myapplication.platform.PlatformActionContext

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
        val dataUri = when {
            uriParam != null -> Uri.parse(uriParam)
            pathParam != null -> Uri.parse("file://$pathParam")
            else -> return ActionResult.Failed("INVALID_PARAMS", "请先选择视频文件")
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(dataUri, "video/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ctx.appContext.startActivity(intent)
            ActionResult.Success
        } catch (e: SecurityException) {
            ActionResult.Failed("NO_PERMISSION", "无法读取所选视频，请重新在配置中选择文件")
        } catch (e: Exception) {
            ActionResult.Failed("PLAY_FAILED", e.message ?: "无法打开视频播放器")
        }
    }
}
