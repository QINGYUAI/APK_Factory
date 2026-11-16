// Author: PKQ32
// Created: 2026-07-11
// Description: 通过 UsageStats 轮询检测前台包名变化，匹配 appOpened 触发器后运行小应用

package com.example.myapplication.platform

import android.app.Application
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import com.example.myapplication.domain.model.MiniApp
import com.example.myapplication.domain.model.TriggerTypes
import com.example.myapplication.engine.MiniAppRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.coroutineContext

/**
 * 仅在已授予使用情况访问、且存在启用中的 appOpened 触发时轮询 [UsageStatsManager.queryEvents]。
 * 检测到前台包名变化并匹配触发器的 packageName 后调用 [MiniAppRunner.runQueued]。
 */
class AppOpenMonitor(
    private val app: Application,
    private val runner: MiniAppRunner,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private var pollJob: Job? = null

    /** packageName → 需触发的小应用 id 集合 */
    @Volatile
    private var packageToMiniAppIds: Map<String, Set<String>> = emptyMap()

    /**
     * 根据当前小应用列表启停轮询；无权限或不存在启用的 appOpened 时停止。
     */
    suspend fun sync(apps: List<MiniApp>) = mutex.withLock {
        val mapping = buildMapping(apps)
        packageToMiniAppIds = mapping
        val shouldPoll = mapping.isNotEmpty() && PermissionHelper.hasUsageAccess(app)
        if (shouldPoll) {
            if (pollJob?.isActive != true) {
                pollJob = scope.launch { pollLoop() }
            }
        } else {
            pollJob?.cancel()
            pollJob = null
        }
    }

    /** 显式停止轮询（例如进程清理）。 */
    suspend fun stop() = mutex.withLock {
        pollJob?.cancel()
        pollJob = null
        packageToMiniAppIds = emptyMap()
    }

    private fun buildMapping(apps: List<MiniApp>): Map<String, Set<String>> {
        val result = mutableMapOf<String, MutableSet<String>>()
        for (miniApp in apps) {
            if (!miniApp.enabled) continue
            for (trigger in miniApp.triggers) {
                if (!trigger.enabled) continue
                if (trigger.type != TriggerTypes.APP_OPENED) continue
                val packageName = trigger.params["packageName"]?.trim().orEmpty()
                if (packageName.isEmpty()) continue
                result.getOrPut(packageName) { mutableSetOf() }.add(miniApp.id)
            }
        }
        return result
    }

    private suspend fun pollLoop() {
        val usm = app.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return
        var windowStart = System.currentTimeMillis()
        var lastForegroundPackage: String? = null

        while (coroutineContext.isActive) {
            delay(POLL_INTERVAL_MS)
            if (!PermissionHelper.hasUsageAccess(app)) {
                // 权限被撤销：退出循环，等待下次 resync 再启动
                break
            }
            val mapping = packageToMiniAppIds
            if (mapping.isEmpty()) {
                break
            }

            val now = System.currentTimeMillis()
            val events = usm.queryEvents(windowStart, now)
            windowStart = now

            val event = UsageEvents.Event()
            var latestForeground: String? = null
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (isMoveToForeground(event)) {
                    latestForeground = event.packageName
                }
            }

            if (latestForeground == null || latestForeground == lastForegroundPackage) {
                continue
            }
            lastForegroundPackage = latestForeground

            val miniAppIds = mapping[latestForeground] ?: continue
            for (miniAppId in miniAppIds) {
                runner.runQueued(miniAppId, TriggerTypes.APP_OPENED)
            }
        }
    }

    private fun isMoveToForeground(event: UsageEvents.Event): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
        } else {
            @Suppress("DEPRECATION")
            event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
        }
    }

    companion object {
        /** 1～2 秒轮询间隔 */
        const val POLL_INTERVAL_MS = 1500L
    }
}
