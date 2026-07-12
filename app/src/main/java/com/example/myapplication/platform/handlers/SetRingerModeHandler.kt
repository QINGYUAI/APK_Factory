// Author: PKQ32
// Created: 2026-07-11
// Description: 响铃模式动作处理器，需 ACCESS_NOTIFICATION_POLICY（勿扰策略）

package com.example.myapplication.platform.handlers

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import com.example.myapplication.domain.ActionHandler
import com.example.myapplication.domain.ActionResult
import com.example.myapplication.domain.model.ActionTypes
import com.example.myapplication.platform.PlatformActionContext

class SetRingerModeHandler(
    private val ctx: PlatformActionContext,
) : ActionHandler {
    override val type = ActionTypes.SET_RINGER_MODE

    override fun validate(params: Map<String, String>): String? {
        val mode = params["mode"] ?: return "需要 mode 参数（silent/vibrate/normal）"
        return if (mode !in setOf("silent", "vibrate", "normal")) {
            "mode 只能是 silent / vibrate / normal"
        } else {
            null
        }
    }

    override suspend fun execute(params: Map<String, String>): ActionResult {
        val mode = params.getValue("mode")
        val notificationManager =
            ctx.appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return ActionResult.Failed("NO_NOTIFICATION", "无法获取通知服务")
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            return ActionResult.Failed(
                "NO_PERMISSION",
                "需要「勿扰权限」才能切换响铃模式，请在权限中心或系统设置中授权",
            )
        }
        val audioManager =
            ctx.appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return ActionResult.Failed("NO_AUDIO", "无法获取音频服务")
        return try {
            val ringerMode = when (mode) {
                "silent" -> AudioManager.RINGER_MODE_SILENT
                "vibrate" -> AudioManager.RINGER_MODE_VIBRATE
                "normal" -> AudioManager.RINGER_MODE_NORMAL
                else -> return ActionResult.Failed("INVALID_PARAMS", "模式只能是 silent / vibrate / normal")
            }
            audioManager.ringerMode = ringerMode
            ActionResult.Success
        } catch (e: SecurityException) {
            ActionResult.Failed(
                "NO_PERMISSION",
                "需要「勿扰权限」才能切换响铃模式，请在权限中心或系统设置中授权",
            )
        } catch (e: Exception) {
            ActionResult.Failed("RINGER_FAILED", e.message ?: "切换响铃模式失败")
        }
    }
}
