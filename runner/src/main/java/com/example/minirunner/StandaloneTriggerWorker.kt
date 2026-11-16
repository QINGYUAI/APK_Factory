// Author: PKQ32
// Created: 2026-11-16
// Description: 独立包后台执行器：WorkManager 中运行嵌入配置的动作链

package com.example.minirunner

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.platform.MiniAppConfigStore

class StandaloneTriggerWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = runCatching { MiniAppConfigStore.load(applicationContext) }.getOrNull()
            ?: return Result.failure()
        if (!app.enabled || app.steps.isEmpty()) return Result.success()
        return try {
            val report = RunnerApplication.instance.executor.run(app.steps)
            Log.i(TAG, "background run: success=${report.success}, steps=${report.stepOutcomes.size}")
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "background run failed", e)
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "StandaloneTrigger"
    }
}
