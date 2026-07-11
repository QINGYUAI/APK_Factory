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
        val enabled = params["enabled"] ?: return "enabled required"
        return if (enabled != "true" && enabled != "false") {
            "enabled must be true or false"
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
            return ActionResult.Failed("NO_PERMISSION", "BLUETOOTH_CONNECT required")
        }
        val adapter =
            (ctx.appContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)
                ?.adapter
                ?: return ActionResult.Failed("NO_BLUETOOTH", "BluetoothAdapter unavailable")
        return try {
            val ok = if (enabled) adapter.enable() else adapter.disable()
            if (ok) {
                ActionResult.Success
            } else {
                // Android 13+ 起 enable/disable 对普通应用常被拒绝
                ActionResult.Failed(
                    "UNSUPPORTED",
                    "Bluetooth enable/disable not allowed for this app (API ${Build.VERSION.SDK_INT})",
                )
            }
        } catch (e: SecurityException) {
            ActionResult.Failed("NO_PERMISSION", e.message ?: "BLUETOOTH_CONNECT required")
        } catch (e: Exception) {
            ActionResult.Failed("UNSUPPORTED", e.message ?: "Failed to toggle Bluetooth")
        }
    }
}
