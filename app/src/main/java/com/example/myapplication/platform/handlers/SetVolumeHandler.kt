// Author: PKQ32
// Created: 2026-07-11
// Description: 音量动作处理器，按百分比调节媒体或铃声音量

package com.example.myapplication.platform.handlers

import android.content.Context
import android.media.AudioManager
import com.example.myapplication.domain.ActionHandler
import com.example.myapplication.domain.ActionResult
import com.example.myapplication.domain.model.ActionTypes
import com.example.myapplication.platform.PlatformActionContext

class SetVolumeHandler(
    private val ctx: PlatformActionContext,
) : ActionHandler {
    override val type = ActionTypes.SET_VOLUME

    override fun validate(params: Map<String, String>): String? {
        val stream = params["stream"] ?: return "需要 stream 参数（music/ring）"
        if (stream != "music" && stream != "ring") {
            return "stream 只能是 music 或 ring"
        }
        val percent = params["percent"]?.toIntOrNull() ?: return "需要 percent 参数（0–100）"
        return if (percent !in 0..100) "percent 必须在 0–100" else null
    }

    override suspend fun execute(params: Map<String, String>): ActionResult {
        val streamType = when (params.getValue("stream")) {
            "music" -> AudioManager.STREAM_MUSIC
            "ring" -> AudioManager.STREAM_RING
            else -> return ActionResult.Failed("INVALID_PARAMS", "stream 只能是 music 或 ring")
        }
        val percent = params.getValue("percent").toInt()
        val audioManager =
            ctx.appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return ActionResult.Failed("NO_AUDIO", "无法获取音频服务")
        val max = audioManager.getStreamMaxVolume(streamType)
        val volume = (max * percent / 100.0).toInt().coerceIn(0, max)
        audioManager.setStreamVolume(streamType, volume, 0)
        return ActionResult.Success
    }
}
