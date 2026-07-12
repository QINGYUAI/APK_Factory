// Author: PKQ32
// Created: 2026-07-11
// Description: 本地通知动作处理器，向 miniapp_actions 渠道发送通知

package com.example.myapplication.platform.handlers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.myapplication.domain.ActionHandler
import com.example.myapplication.domain.ActionResult
import com.example.myapplication.domain.model.ActionTypes
import com.example.myapplication.platform.PlatformActionContext

class NotifyHandler(
    private val ctx: PlatformActionContext,
) : ActionHandler {
    override val type = ActionTypes.NOTIFY

    override fun validate(params: Map<String, String>): String? {
        if (params["title"].isNullOrBlank()) return "需要填写通知标题"
        if (params["text"].isNullOrBlank()) return "需要填写通知内容"
        return null
    }

    override suspend fun execute(params: Map<String, String>): ActionResult {
        val title = params.getValue("title")
        val text = params.getValue("text")
        ensureChannel()
        val notification = NotificationCompat.Builder(ctx.appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        return try {
            NotificationManagerCompat.from(ctx.appContext)
                .notify(System.currentTimeMillis().toInt(), notification)
            ActionResult.Success
        } catch (e: SecurityException) {
            ActionResult.Failed("NO_PERMISSION", "需要「通知权限」，请在权限中心授权")
        }
    }

    private fun ensureChannel() {
        val manager =
            ctx.appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "小应用动作通知",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "miniapp_actions"
    }
}
