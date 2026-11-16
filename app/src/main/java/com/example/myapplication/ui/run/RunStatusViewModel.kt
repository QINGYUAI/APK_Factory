// Author: PKQ32
// Created: 2026-11-16
// Description: 运行监控状态层：手动执行、跳过原因、结果与停止

package com.example.myapplication.ui.run

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.di.ServiceLocator
import com.example.myapplication.engine.RunReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RunUiState(
    val running: Boolean = true,
    val skipped: Boolean = false,
    val report: RunReport? = null,
    val error: String? = null,
)

class RunStatusViewModel : ViewModel() {

    private val _state = MutableStateFlow(RunUiState())
    val state: StateFlow<RunUiState> = _state.asStateFlow()

    val runningId: StateFlow<String?> = ServiceLocator.runner.runningId

    private var startedFor: String? = null

    fun start(miniAppId: String) {
        if (startedFor == miniAppId) return
        startedFor = miniAppId
        _state.value = RunUiState(running = true)
        viewModelScope.launch {
            try {
                val result = ServiceLocator.runner.run(miniAppId, "manual")
                _state.update {
                    it.copy(running = false, skipped = result == null, report = result)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(running = false, error = e.message ?: "执行失败")
                }
            }
        }
    }

    fun stop() = ServiceLocator.runner.stop()

    /** 重新执行：清掉「已为本次进入启动过」标记后再启动。 */
    fun rerun(miniAppId: String) {
        startedFor = null
        start(miniAppId)
    }
}
