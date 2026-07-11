// Author: PKQ32
// Created: 2026-07-11
// Description: 系统事件广播接收：充电连接时匹配 event 触发器并运行小应用

package com.example.myapplication.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.myapplication.di.ServiceLocator
import com.example.myapplication.domain.model.TriggerTypes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 监听 [Intent.ACTION_POWER_CONNECTED]，查找启用中且 triggers 含匹配 event 的小应用并执行。
 */
class PowerConnectedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_POWER_CONNECTED) return
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val action = Intent.ACTION_POWER_CONNECTED
                val apps = ServiceLocator.miniApps.getAll()
                for (app in apps) {
                    if (!app.enabled) continue
                    val matched = app.triggers.any { trigger ->
                        trigger.enabled &&
                            trigger.type == TriggerTypes.EVENT &&
                            trigger.params["action"] == action
                    }
                    if (matched) {
                        ServiceLocator.runner.run(app.id, TriggerTypes.EVENT)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
