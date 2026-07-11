// Author: PKQ32
// Created: 2026-07-11
// Description: 小应用运行会话：单飞、防抖、执行并落库运行日志

package com.example.myapplication.engine

import com.example.myapplication.data.MiniAppRepository
import com.example.myapplication.data.RunLogRepository
import com.example.myapplication.domain.ActionResult
import com.example.myapplication.domain.model.RunLog
import kotlinx.coroutines.sync.Mutex
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class MiniAppRunner(
    private val miniApps: MiniAppRepository,
    private val runLogs: RunLogRepository,
    private val executor: ActionExecutor,
    private val debouncer: Debouncer,
) {
    private val mutex = Mutex()
    @Volatile var runningId: String? = null
        private set

    /**
     * 单飞运行：已有任务在执行时立即返回 null（不排队等待），保留防抖。
     */
    suspend fun run(miniAppId: String, source: String): RunReport? {
        // 已在运行或锁被占用：忽略本次触发，避免排队
        if (runningId != null || !mutex.tryLock()) return null
        try {
            if (!debouncer.shouldRun("$source:$miniAppId")) return null
            val app = miniApps.get(miniAppId) ?: return RunReport(
                success = false,
                stepOutcomes = listOf(
                    StepOutcome("-", "-", ActionResult.Failed("NOT_FOUND", "MiniApp not found"))
                )
            )
            if (!app.enabled) return RunReport(
                success = false,
                stepOutcomes = listOf(
                    StepOutcome("-", "-", ActionResult.Failed("DISABLED", "MiniApp disabled"))
                )
            )
            runningId = miniAppId
            val started = System.currentTimeMillis()
            return try {
                val report = executor.run(app.steps)
                val arr = JSONArray()
                report.stepOutcomes.forEach { o ->
                    arr.put(JSONObject().apply {
                        put("stepId", o.stepId)
                        put("type", o.type)
                        put("ok", o.result is ActionResult.Success)
                        if (o.result is ActionResult.Failed) {
                            put("code", o.result.code)
                            put("message", o.result.message)
                        }
                    })
                }
                runLogs.append(
                    RunLog(
                        id = UUID.randomUUID().toString(),
                        miniAppId = miniAppId,
                        startedAt = started,
                        endedAt = System.currentTimeMillis(),
                        stepResultsJson = arr.toString(),
                        success = report.success,
                    )
                )
                report
            } finally {
                runningId = null
            }
        } finally {
            mutex.unlock()
        }
    }
}
