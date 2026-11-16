// Author: PKQ32
// Created: 2026-07-11
// Description: 小应用运行会话：单飞、后台触发排队、防抖、可停止、执行并落库运行日志

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
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class MiniAppRunner(
    private val miniApps: MiniAppRepository,
    private val runLogs: RunLogRepository,
    private val executor: ActionExecutor,
    private val debouncer: Debouncer,
    /** 停止时附带关闭媒体播放；测试可注入空实现 */
    private val onStopMedia: () -> Unit = { MediaPlaybackController.stop() },
    /** 后台触发最多允许多少个同时排队/执行 */
    private val maxQueued: Int = DEFAULT_MAX_QUEUED,
) {
    private val mutex = Mutex()

    /** 已进入排队/执行的后台触发键（source:miniAppId），用于合并重复事件 */
    private val pendingKeys = ConcurrentHashMap.newKeySet<String>()
    private val waiting = AtomicInteger(0)

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
     * 手动/前台运行：已有任务在执行时立即返回 null（不等待），保留防抖。
     * 可通过 [stop] 中断当前执行。
     */
    suspend fun run(miniAppId: String, source: String): RunReport? {
        if (!mutex.tryLock()) return null
        return try {
            executeLocked(miniAppId, source)
        } finally {
            mutex.unlock()
        }
    }

    /**
     * 后台触发运行（定时 / 事件 / appOpened / hostOpened）：
     * 已有任务在执行时排队等待，而不是直接丢弃；排队超过 [maxQueued] 才放弃。
     * 同一 `source:miniAppId` 已在排队/执行时，重复事件直接忽略。
     */
    suspend fun runQueued(miniAppId: String, source: String): RunReport? {
        val key = "$source:$miniAppId"
        if (!pendingKeys.add(key)) return null
        try {
            if (waiting.incrementAndGet() > maxQueued) {
                waiting.decrementAndGet()
                return null
            }
            try {
                return mutex.withLock { executeLocked(miniAppId, source) }
            } finally {
                waiting.decrementAndGet()
            }
        } finally {
            pendingKeys.remove(key)
        }
    }

    /** 停止当前正在执行的动作链，并停止正在播放的媒体。 */
    fun stop() {
        runJob?.cancel()
        runCatching { onStopMedia() }
    }

    /** 已持有 [mutex]：防抖、取配置、执行并落库。 */
    private suspend fun executeLocked(miniAppId: String, source: String): RunReport? {
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

    companion object {
        const val DEFAULT_MAX_QUEUED = 16
    }
}
