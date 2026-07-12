// Author: PKQ32
// Created: 2026-07-11
// Description: 系统亮度动作处理器，需 WRITE_SETTINGS 特殊权限

package com.example.myapplication.platform.handlers

import android.provider.Settings
import com.example.myapplication.domain.ActionHandler
import com.example.myapplication.domain.ActionResult
import com.example.myapplication.domain.model.ActionTypes
import com.example.myapplication.platform.PlatformActionContext

class SetBrightnessHandler(
    private val ctx: PlatformActionContext,
) : ActionHandler {
    override val type = ActionTypes.SET_BRIGHTNESS

    override fun validate(params: Map<String, String>): String? {
        val percent = params["percent"]?.toIntOrNull() ?: return "需要 percent 参数（0–100）"
        return if (percent !in 0..100) "percent 必须在 0–100" else null
    }

    override suspend fun execute(params: Map<String, String>): ActionResult {
        val percent = params.getValue("percent").toInt()
        if (!Settings.System.canWrite(ctx.appContext)) {
            return ActionResult.Failed(
                "NEEDS_SETTINGS",
                "需要「修改系统设置」权限才能调节亮度，请在权限中心授权",
            )
        }
        return try {
            Settings.System.putInt(
                ctx.appContext.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
            )
            val value = (255 * percent / 100.0).toInt().coerceIn(0, 255)
            Settings.System.putInt(
                ctx.appContext.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                value,
            )
            ActionResult.Success
        } catch (e: SecurityException) {
            ActionResult.Failed(
                "NEEDS_SETTINGS",
                "需要「修改系统设置」权限才能调节亮度",
            )
        } catch (e: Exception) {
            ActionResult.Failed("BRIGHTNESS_FAILED", e.message ?: "调节亮度失败")
        }
    }
}
