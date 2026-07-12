// Author: PKQ32
// Created: 2026-07-11
// Description: 手电筒动作处理器，通过 CameraManager.setTorchMode 开关闪光灯

package com.example.myapplication.platform.handlers

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import com.example.myapplication.domain.ActionHandler
import com.example.myapplication.domain.ActionResult
import com.example.myapplication.domain.model.ActionTypes
import com.example.myapplication.platform.PlatformActionContext
import com.example.myapplication.platform.RuntimeEffectTracker

class FlashlightHandler(
    private val ctx: PlatformActionContext,
) : ActionHandler {
    override val type = ActionTypes.FLASHLIGHT

    override fun validate(params: Map<String, String>): String? {
        val enabled = params["enabled"] ?: return "需要 enabled 参数（true/false）"
        return if (enabled != "true" && enabled != "false") {
            "enabled 只能是 true 或 false"
        } else {
            null
        }
    }

    override suspend fun execute(params: Map<String, String>): ActionResult {
        val enabled = params.getValue("enabled").toBoolean()
        val cameraManager =
            ctx.appContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return ActionResult.Failed("NO_CAMERA", "无法获取相机服务")
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return ActionResult.Failed("NO_FLASH", "当前设备没有闪光灯")
            cameraManager.setTorchMode(cameraId, enabled)
            RuntimeEffectTracker.setFlashlightOn(enabled)
            ActionResult.Success
        } catch (e: SecurityException) {
            ActionResult.Failed("NO_PERMISSION", "手电筒权限被拒绝")
        } catch (e: Exception) {
            ActionResult.Failed("TORCH_FAILED", e.message ?: "开关手电筒失败")
        }
    }
}
