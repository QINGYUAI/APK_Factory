// Author: PKQ32
// Created: 2026-11-16
// Description: 独立包系统事件接收器：开机/对时后重排闹钟，并匹配静态系统事件触发

package com.example.minirunner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.myapplication.domain.model.TriggerTypes
import com.example.myapplication.platform.MiniAppConfigStore

class StandaloneSystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        // 重启、应用更新、手动改时间/时区后，AlarmManager 中的任务需要重排
        when (action) {
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                // 更新后的安装包已嵌入工厂最新配置；清掉旧的同步覆盖，避免新配置被旧文件压住
                runCatching { MiniAppConfigStore.clearOverride(context) }
                StandaloneTriggerScheduler.resync(context)
            }
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> StandaloneTriggerScheduler.resync(context)
        }

        val app = runCatching { MiniAppConfigStore.load(context) }.getOrNull() ?: return
        if (!app.enabled) return
        val matched = app.triggers.any { trigger ->
            trigger.enabled &&
                trigger.type == TriggerTypes.EVENT &&
                trigger.params["action"] == action
        }
        if (matched) StandaloneTriggerScheduler.enqueueRun(context)
    }
}
