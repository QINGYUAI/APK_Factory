// Author: PKQ32
// Created: 2026-07-11
// Description: 触发器同步：为 schedule 注册/取消 WorkManager 周期任务

package com.example.myapplication.engine

import android.app.Application
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.myapplication.data.MiniAppRepository
import com.example.myapplication.domain.model.TriggerTypes
import com.example.myapplication.worker.ScheduleMiniAppWorker
import java.util.concurrent.TimeUnit

/**
 * 将启用中的定时触发器与 WorkManager 对齐；事件类由 Manifest 静态 Receiver 处理。
 *
 * PeriodicWork 最短间隔 15 分钟：若用户配置更短，按 15 分钟入队。
 */
class TriggerManager(
    private val app: Application,
    private val miniApps: MiniAppRepository,
) {
    private val workManager: WorkManager get() = WorkManager.getInstance(app)
    private val prefs by lazy {
        app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    suspend fun resync() {
        val apps = miniApps.getAll()
        val desired = linkedSetOf<String>()

        for (miniApp in apps) {
            if (!miniApp.enabled) continue
            for (trigger in miniApp.triggers) {
                if (!trigger.enabled) continue
                if (trigger.type != TriggerTypes.SCHEDULE) continue

                val workName = scheduleWorkName(miniApp.id, trigger.id)
                desired.add(workName)

                val requestedMinutes = trigger.params["intervalMinutes"]?.toLongOrNull() ?: 15L
                // PeriodicWorkRequest 系统下限 15 分钟
                val intervalMinutes = maxOf(requestedMinutes, MIN_PERIODIC_MINUTES)

                val request = PeriodicWorkRequestBuilder<ScheduleMiniAppWorker>(
                    intervalMinutes,
                    TimeUnit.MINUTES,
                )
                    .setInputData(
                        workDataOf(ScheduleMiniAppWorker.KEY_MINI_APP_ID to miniApp.id),
                    )
                    .build()

                workManager.enqueueUniquePeriodicWork(
                    workName,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request,
                )
            }
        }

        val previous = prefs.getStringSet(KEY_SCHEDULE_NAMES, emptySet()).orEmpty().toSet()
        for (orphan in previous - desired) {
            workManager.cancelUniqueWork(orphan)
        }
        prefs.edit().putStringSet(KEY_SCHEDULE_NAMES, HashSet(desired)).apply()
    }

    companion object {
        private const val PREFS_NAME = "trigger_manager"
        private const val KEY_SCHEDULE_NAMES = "schedule_work_names"
        const val MIN_PERIODIC_MINUTES = 15L

        fun scheduleWorkName(miniAppId: String, triggerId: String): String =
            "schedule-$miniAppId-$triggerId"
    }
}
