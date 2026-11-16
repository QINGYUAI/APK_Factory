// Author: PKQ32
// Created: 2026-07-11
// Description: WorkManager 周期任务：按 miniAppId 触发定时执行

package com.example.myapplication.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.di.ServiceLocator
import com.example.myapplication.domain.model.TriggerTypes

/**
 * 读取 inputData 中的 miniAppId，以 schedule 来源运行小应用。
 */
class ScheduleMiniAppWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val miniAppId = inputData.getString(KEY_MINI_APP_ID) ?: return Result.failure()
        ServiceLocator.runner.runQueued(miniAppId, TriggerTypes.SCHEDULE)
        return Result.success()
    }

    companion object {
        const val KEY_MINI_APP_ID = "miniAppId"
    }
}
