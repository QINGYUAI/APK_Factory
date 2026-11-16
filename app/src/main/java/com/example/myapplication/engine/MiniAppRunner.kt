// Author: PKQ32
// Created: 2026-07-11
// Description: 小应用运行会话：单飞、防抖、可停止、执行并落库运行日志

package com.example.myapplication.engine

import com.example.myapplication.data.MiniAppRepository
import com.example.myapplication.data.RunLogRepository
import com.example.myapplication.domain.ActionResult
import com.example.myapplication.domain.model.RunLog
import com.example.myapplication.platform.media.MediaPlaybackController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class MiniAppRunner(
    private val miniApps: MiniAppRepository,
    private val runLogs: RunLogRepository,
    private val executor: ActionExecutor,
    private val debouncer: Debouncer,
    /** 停止时附带关闭媒体播放；测试可注入空实现 */
    private val onStopMedia: () -> Unit = { MediaPlaybackController.stop() },
) {
    private val mutex = Mutex()

    /**
     * 动作链运行在 Runner 自己的作用域里：
     * [stop] 只取消本次动作链，不会连带取消调用方（ViewModel / 轮询 / 广播接收器）的协程。
     */
    private val runScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var runJob: Job? = null

    private val _runningId = MutableStateFlow<String?>(null)
    /** 当前正在运行的小应用 id；null 表示空闲。 */
    val runningId: StateFlow<String?> = _runningId.asStateFlow()

    /**
     * 单飞运行：已有任务在执行时立即返回 null（不排队等待），保留防抖。
     * 可通过 [stop] 中断当前执行。
     */
    suspend fun run(miniAppId: String, source: String): RunReport? {
        // 已在运行或锁被占用：忽略本次触发，避免排队
        if (_runningId.value != null || !mutex.tryLock()) return null
        try {
            if (!debouncer.shouldRun("$source:$miniAppId")) return null
            val app = miniApps.get(miniAppId) ?: return RunReport(
                success = false,
                stepOutcomes = listOf(
                    StepOutcome("-", "-", ActionResult.Failed("NOT_FOUND", "MiniApp not found")),
                ),
            )
            if (!app.enabled) return RunReport(
                success = false,
                stepOutcomes = listOf(
                    StepOutcome("-", "-", ActionResult.Failed("DISABLED", "MiniApp disabled")),
                ),
            )
            _runningId.value = miniAppId
            val started = System.currentTimeMillis()
            return try {
                val job = runScope.async {
                    val report = executor.run(app.steps)
                    persistLog(miniAppId, started, report)
                    report
                }
                runJob = job
                try {
                    job.await()
                } catch (e: CancellationException) {
                    // 主动停止：返回可展示的停止结果；调用方协程不受影响
                    if (job.isCancelled) {
                        RunReport(success = false, stopped = true, stepOutcomes = emptyList())
                    } else {
                        // 调用方被取消：连带取消动作链，避免遗留孤儿任务
                        job.cancel()
                        throw e
                    }
                }
            } finally {
                runJob = null
                _runningId.value = null
            }
        } finally {
            mutex.unlock()
        }
    }

    /** 停止当前正在执行的动作链，并停止正在播放的媒体。 */
    fun stop() {
        runJob?.cancel()
        runCatching { onStopMedia() }
    }

    private suspend fun persistLog(miniAppId: String, started: Long, report: RunReport) {
        val arr = JSONArray()
        report.stepOutcomes.forEach { o ->
            arr.put(
                JSONObject().apply {
                    put("stepId", o.stepId)
                    put("type", o.type)
                    put("ok", o.result is ActionResult.Success)
                    if (o.result is ActionResult.Failed) {
                        put("code", o.result.code)
                        put("message", o.result.message)
                    }
                },
            )
        }
        runLogs.append(
            RunLog(
                id = UUID.randomUUID().toString(),
                miniAppId = miniAppId,
                startedAt = started,
                endedAt = System.currentTimeMillis(),
                stepResultsJson = arr.toString(),
                success = report.success,
            ),
        )
    }
}
