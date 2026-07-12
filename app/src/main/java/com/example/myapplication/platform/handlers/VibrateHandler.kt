// Author: PKQ32
// Created: 2026-07-11
// Description: 震动动作处理器，通过 VibratorManager 触发短震动

package com.example.myapplication.platform.handlers

import android.os.VibrationEffect
import android.os.VibratorManager
import com.example.myapplication.domain.ActionHandler
import com.example.myapplication.domain.ActionResult
import com.example.myapplication.domain.model.ActionTypes
import com.example.myapplication.platform.PlatformActionContext

class VibrateHandler(
    private val ctx: PlatformActionContext,
) : ActionHandler {
    override val type = ActionTypes.VIBRATE

    override fun validate(params: Map<String, String>): String? {
        val raw = params["millis"] ?: return null
        val ms = raw.toLongOrNull() ?: return "millis 必须是数字"
        return if (ms < 0) "millis 必须 ≥ 0" else null
    }

    override suspend fun execute(params: Map<String, String>): ActionResult {
        val millis = params["millis"]?.toLongOrNull() ?: DEFAULT_MILLIS
        val vibratorManager =
            ctx.appContext.getSystemService(VibratorManager::class.java)
                ?: return ActionResult.Failed("NO_VIBRATOR", "无法获取震动服务")
        val vibrator = vibratorManager.defaultVibrator
        if (!vibrator.hasVibrator()) {
            return ActionResult.Failed("NO_VIBRATOR", "当前设备不支持震动")
        }
        vibrator.vibrate(
            VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE),
        )
        return ActionResult.Success
    }

    companion object {
        private const val DEFAULT_MILLIS = 200L
    }
}
