// Author: PKQ32
// Created: 2026-07-11
// Description: 蓝牙开关动作处理器，需 BLUETOOTH_CONNECT；受限 API 返回 UNSUPPORTED

package com.example.myapplication.platform.handlers

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.myapplication.domain.ActionHandler
import com.example.myapplication.domain.ActionResult
import com.example.myapplication.domain.model.ActionTypes
import com.example.myapplication.platform.PlatformActionContext

class BluetoothHandler(
    private val ctx: PlatformActionContext,
) : ActionHandler {
    override val type = ActionTypes.BLUETOOTH

    override fun validate(params: Map<String, String>): String? {
        val enabled = params["enabled"] ?: return "需要 enabled 参数（true/false）"
        return if (enabled != "true" && enabled != "false") {
            "enabled 只能是 true 或 false"
        } else {
            null
        }
    }

    @Suppress("DEPRECATION")
    override suspend fun execute(params: Map<String, String>): ActionResult {
        val enabled = params.getValue("enabled").toBoolean()
        if (ContextCompat.checkSelfPermission(ctx.appContext, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return ActionResult.Failed("NO_PERMISSION", "需要「附近的设备 / 蓝牙」权限，请到应用权限设置中开启")
        }
        val adapter =
            (ctx.appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)
                ?.adapter
                ?: return ActionResult.Failed("NO_BLUETOOTH", "无法获取蓝牙适配器")
        return try {
            val ok = if (enabled) adapter.enable() else adapter.disable()
            if (ok) {
                ActionResult.Success
            } else {
                ActionResult.Failed(
                    "UNSUPPORTED",
                    "当前系统（API ${Build.VERSION.SDK_INT}）不允许应用直接开关蓝牙，请到系统设置操作",
                )
            }
        } catch (e: SecurityException) {
            ActionResult.Failed("NO_PERMISSION", "需要「附近的设备 / 蓝牙」权限")
        } catch (e: Exception) {
            ActionResult.Failed("UNSUPPORTED", e.message ?: "切换蓝牙失败")
        }
    }
}
