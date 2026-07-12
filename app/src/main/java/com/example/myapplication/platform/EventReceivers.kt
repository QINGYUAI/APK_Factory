// Author: PKQ32
// Created: 2026-07-12
// Description: 系统事件广播：匹配 event 触发器并运行小应用

package com.example.myapplication.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.example.myapplication.di.ServiceLocator
import com.example.myapplication.domain.model.TriggerTypes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manifest 静态注册的系统事件接收器（充电/开机/解锁等）。
 */
class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        dispatchEvent(context, action, goAsync())
    }

    companion object {
        fun dispatchEvent(
            context: Context,
            action: String,
            pendingResult: PendingResult? = null,
        ) {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scope.launch {
                try {
                    if (!serviceLocatorReady()) return@launch
                    if (action == Intent.ACTION_BOOT_COMPLETED) {
                        runCatching { ServiceLocator.triggerManager.resync() }
                    }
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
                    pendingResult?.finish()
                }
            }
        }

        private fun serviceLocatorReady(): Boolean = try {
            ServiceLocator.miniApps
            true
        } catch (_: Throwable) {
            false
        }
    }
}

/**
 * 兼容旧 Manifest 类名。
 */
class PowerConnectedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        SystemEventReceiver().onReceive(context, intent)
    }
}

/**
 * 动态注册 SCREEN_ON / SCREEN_OFF 等无法静态声明的事件。
 */
class DynamicScreenEventMonitor(
    private val appContext: Context,
) {
    private var receiver: BroadcastReceiver? = null

    fun sync(neededActions: Set<String>) {
        val dynamic = neededActions.filter { action ->
            SystemEventCatalog.of(action)?.dynamicOnly == true
        }.toSet()
        unregister()
        if (dynamic.isEmpty()) return
        val filter = IntentFilter().apply {
            dynamic.forEach { addAction(it) }
        }
        val r = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                val action = intent?.action ?: return
                SystemEventReceiver.dispatchEvent(context, action)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(r, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            appContext.registerReceiver(r, filter)
        }
        receiver = r
    }

    fun unregister() {
        val r = receiver ?: return
        runCatching { appContext.unregisterReceiver(r) }
        receiver = null
    }
}
