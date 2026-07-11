// Author: PKQ32
// Created: 2026-07-11
// Description: Wi‑Fi 开关动作处理器；现代 Android 可能因 API 限制返回 UNSUPPORTED

package com.example.myapplication.platform.handlers

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import com.example.myapplication.domain.ActionHandler
import com.example.myapplication.domain.ActionResult
import com.example.myapplication.domain.model.ActionTypes
import com.example.myapplication.platform.PlatformActionContext

class WifiHandler(
    private val ctx: PlatformActionContext,
) : ActionHandler {
    override val type = ActionTypes.WIFI

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
        // Android 10+ 普通应用无法直接开关 Wi‑Fi
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ActionResult.Failed(
                "UNSUPPORTED",
                "Apps cannot toggle Wi-Fi on Android 10+; use system Settings",
            )
        }
        val wifiManager =
            ctx.appContext.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                ?: return ActionResult.Failed("NO_WIFI", "WifiManager unavailable")
        return try {
            val ok = wifiManager.setWifiEnabled(enabled)
            if (ok) {
                ActionResult.Success
            } else {
                ActionResult.Failed("UNSUPPORTED", "WifiManager.setWifiEnabled returned false")
            }
        } catch (e: SecurityException) {
            ActionResult.Failed("NO_PERMISSION", e.message ?: "Wi-Fi permission denied")
        } catch (e: Exception) {
            ActionResult.Failed("UNSUPPORTED", e.message ?: "Failed to toggle Wi-Fi")
        }
    }
}
