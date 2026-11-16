// Author: PKQ32
// Created: 2026-11-16
// Description: 独立入口状态层：加载单个小应用、启停、启停用切换

package com.example.myapplication.ui.standalone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.di.ServiceLocator
import com.example.myapplication.domain.model.MiniApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StandaloneUiState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    val app: MiniApp? = null,
    val error: String? = null,
)

class MiniAppStandaloneViewModel : ViewModel() {

    private val _state = MutableStateFlow(StandaloneUiState())
    val state: StateFlow<StandaloneUiState> = _state.asStateFlow()

    val runningId: StateFlow<String?> = ServiceLocator.runner.runningId

    private var loadedId: String? = null

    fun load(miniAppId: String) {
        if (loadedId == miniAppId) return
        loadedId = miniAppId
        _state.value = StandaloneUiState()
        viewModelScope.launch {
            val result = runCatching { ServiceLocator.miniApps.get(miniAppId) }
            _state.value = result.fold(
                onSuccess = { app ->
                    if (app == null) {
                        StandaloneUiState(loading = false, missing = true)
                    } else {
                        StandaloneUiState(loading = false, app = app)
                    }
                },
                onFailure = { e ->
                    StandaloneUiState(
                        loading = false,
                        error = "加载失败：${e.message ?: e.javaClass.simpleName}",
                    )
                },
            )
        }
    }

    fun setEnabled(enabled: Boolean) {
        val current = _state.value.app ?: return
        val updated = current.copy(enabled = enabled, updatedAt = System.currentTimeMillis())
        _state.update { it.copy(app = updated) }
        viewModelScope.launch {
            ServiceLocator.miniApps.save(updated)
            runCatching { ServiceLocator.triggerManager.resync() }
        }
    }

    fun stop() = ServiceLocator.runner.stop()
}
