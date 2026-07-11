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
        val mode = params["mode"] ?: return "mode required"
        return if (mode !in setOf("silent", "vibrate", "normal")) {
            "mode must be silent, vibrate, or normal"
        } else {
            null
        }
    }

    override suspend fun execute(params: Map<String, String>): ActionResult {
        val mode = params.getValue("mode")
        val notificationManager =
            ctx.appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return ActionResult.Failed("NO_NOTIFICATION", "NotificationManager unavailable")
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            return ActionResult.Failed(
                "NO_PERMISSION",
                "ACCESS_NOTIFICATION_POLICY required; grant Do Not Disturb access in settings",
            )
        }
        val audioManager =
            ctx.appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return ActionResult.Failed("NO_AUDIO", "AudioManager unavailable")
        return try {
            val ringerMode = when (mode) {
                "silent" -> AudioManager.RINGER_MODE_SILENT
                "vibrate" -> AudioManager.RINGER_MODE_VIBRATE
                "normal" -> AudioManager.RINGER_MODE_NORMAL
                else -> return ActionResult.Failed("INVALID_PARAMS", "mode must be silent, vibrate, or normal")
            }
            audioManager.ringerMode = ringerMode
            ActionResult.Success
        } catch (e: SecurityException) {
            ActionResult.Failed("NO_PERMISSION", e.message ?: "ACCESS_NOTIFICATION_POLICY required")
        } catch (e: Exception) {
            ActionResult.Failed("RINGER_FAILED", e.message ?: "Failed to set ringer mode")
        }
    }
}
