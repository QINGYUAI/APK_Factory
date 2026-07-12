// Author: PKQ32
// Created: 2026-07-11
// Description: 打开第三方应用动作处理器，按 packageName 启动启动 Intent

package com.example.myapplication.platform.handlers

import android.content.Intent
import com.example.myapplication.domain.ActionHandler
import com.example.myapplication.domain.ActionResult
import com.example.myapplication.domain.model.ActionTypes
import com.example.myapplication.platform.PlatformActionContext

class OpenAppHandler(
    private val ctx: PlatformActionContext,
) : ActionHandler {
    override val type = ActionTypes.OPEN_APP

    override fun validate(params: Map<String, String>): String? {
        if (params["packageName"].isNullOrBlank()) return "需要填写应用包名"
        return null
    }

    override suspend fun execute(params: Map<String, String>): ActionResult {
        val packageName = params.getValue("packageName")
        val launchIntent =
            ctx.appContext.packageManager.getLaunchIntentForPackage(packageName)
                ?: return ActionResult.Failed("NOT_INSTALLED", "未安装应用：$packageName")
        return try {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.appContext.startActivity(launchIntent)
            ActionResult.Success
        } catch (e: Exception) {
            ActionResult.Failed("LAUNCH_FAILED", e.message ?: "无法启动应用：$packageName")
        }
    }
}
