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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ActionResult.Failed(
                "UNSUPPORTED",
                "Android 10 及以上不允许应用直接开关 Wi‑Fi，请到系统设置中手动操作",
            )
        }
        val wifiManager =
            ctx.appContext.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                ?: return ActionResult.Failed("NO_WIFI", "无法获取 Wi‑Fi 服务")
        return try {
            val ok = wifiManager.setWifiEnabled(enabled)
            if (ok) {
                ActionResult.Success
            } else {
                ActionResult.Failed("UNSUPPORTED", "系统拒绝切换 Wi‑Fi，请到系统设置中操作")
            }
        } catch (e: SecurityException) {
            ActionResult.Failed("NO_PERMISSION", "没有 Wi‑Fi 相关权限")
        } catch (e: Exception) {
            ActionResult.Failed("UNSUPPORTED", e.message ?: "切换 Wi‑Fi 失败")
        }
    }
}
