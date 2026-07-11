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

class FlashlightHandler(
    private val ctx: PlatformActionContext,
) : ActionHandler {
    override val type = ActionTypes.FLASHLIGHT

    override fun validate(params: Map<String, String>): String? {
        val enabled = params["enabled"] ?: return "enabled required"
        return if (enabled != "true" && enabled != "false") {
            "enabled must be true or false"
        } else {
            null
        }
    }

    override suspend fun execute(params: Map<String, String>): ActionResult {
        val enabled = params.getValue("enabled").toBoolean()
        val cameraManager =
            ctx.appContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return ActionResult.Failed("NO_CAMERA", "CameraManager unavailable")
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return ActionResult.Failed("NO_FLASH", "Device has no flash unit")
            cameraManager.setTorchMode(cameraId, enabled)
            ActionResult.Success
        } catch (e: SecurityException) {
            ActionResult.Failed("NO_PERMISSION", e.message ?: "Camera/torch permission denied")
        } catch (e: Exception) {
            ActionResult.Failed("TORCH_FAILED", e.message ?: "Failed to set torch mode")
        }
    }
}
