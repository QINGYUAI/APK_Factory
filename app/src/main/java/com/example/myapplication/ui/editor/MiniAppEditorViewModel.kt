// Author: PKQ32
// Created: 2026-11-16
// Description: 编辑器状态层：草稿加载、动作/触发器增删排序、校验与保存

package com.example.myapplication.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.di.ServiceLocator
import com.example.myapplication.domain.FailurePolicy
import com.example.myapplication.domain.model.ActionStep
import com.example.myapplication.domain.model.MiniApp
import com.example.myapplication.domain.model.Trigger
import com.example.myapplication.domain.model.TriggerTypes
import com.example.myapplication.platform.ScheduleValidation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class EditorUiState(
    val loading: Boolean = true,
    val notFound: Boolean = false,
    val app: MiniApp? = null,
    val error: String? = null,
)

class MiniAppEditorViewModel : ViewModel() {

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages

    private val _saved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saved: SharedFlow<Unit> = _saved

    private var loadedId: String? = null
    private var saving = false

    fun load(miniAppId: String) {
        if (loadedId == miniAppId) return
        loadedId = miniAppId
        _state.value = EditorUiState()
        viewModelScope.launch {
            val result = runCatching { ServiceLocator.miniApps.get(miniAppId) }
            _state.value = result.fold(
                onSuccess = { app ->
                    if (app == null) {
                        EditorUiState(loading = false, notFound = true)
                    } else {
                        EditorUiState(loading = false, app = app)
                    }
                },
                onFailure = { e ->
                    EditorUiState(
                        loading = false,
                        error = "加载失败：${e.message ?: e.javaClass.simpleName}",
                    )
                },
            )
        }
    }

    fun update(transform: (MiniApp) -> MiniApp) {
        _state.update { current ->
            current.copy(app = current.app?.let(transform))
        }
    }

    fun addStep(type: String) {
        update { app ->
            val step = ActionStep(
                id = UUID.randomUUID().toString(),
                type = type,
                params = defaultParamsForType(type),
                order = app.steps.size,
                onFailure = FailurePolicy.ABORT,
            )
            app.copy(steps = app.steps + step)
        }
    }

    fun replaceStep(index: Int, step: ActionStep) {
        update { app ->
            if (index !in app.steps.indices) return@update app
            val list = app.steps.toMutableList()
            list[index] = step
            app.copy(steps = list)
        }
    }

    fun moveStep(index: Int, delta: Int) {
        update { app ->
            val target = index + delta
            if (index !in app.steps.indices || target !in app.steps.indices) return@update app
            val list = app.steps.toMutableList()
            val moving = list.removeAt(index)
            list.add(target, moving)
            app.copy(steps = list)
        }
    }

    fun removeStep(index: Int) {
        update { app ->
            if (index !in app.steps.indices) return@update app
            app.copy(steps = app.steps.filterIndexed { i, _ -> i != index })
        }
    }

    fun addTrigger(type: String) {
        update { app ->
            val trigger = Trigger(
                id = UUID.randomUUID().toString(),
                type = type,
                params = defaultParamsForTrigger(type),
                enabled = true,
            )
            app.copy(
                triggers = app.triggers + trigger,
                runOnHostOpen = if (type == TriggerTypes.HOST_OPENED) true else app.runOnHostOpen,
            )
        }
    }

    fun replaceTrigger(index: Int, trigger: Trigger) {
        update { app ->
            if (index !in app.triggers.indices) return@update app
            val list = app.triggers.toMutableList()
            list[index] = trigger
            app.copy(triggers = list)
        }
    }

    fun removeTrigger(index: Int) {
        update { app ->
            if (index !in app.triggers.indices) return@update app
            app.copy(triggers = app.triggers.filterIndexed { i, _ -> i != index })
        }
    }

    fun save() {
        if (saving) return
        val current = _state.value.app ?: return
        val stepErrors = current.steps.mapIndexedNotNull { index, step ->
            ServiceLocator.registry.get(step.type)?.validate(step.params)?.let { error ->
                "第 ${index + 1} 步「${actionTitleZh(step.type)}」：$error"
            }
        }
        val calendarErrors = current.triggers
            .filter { it.enabled && TriggerTypes.isCalendarType(it.type) }
            .mapNotNull { trigger ->
                ScheduleValidation.validate(trigger)?.let { "${triggerTitleZh(trigger.type)}：$it" }
            }
        val problems = stepErrors + calendarErrors
        if (problems.isNotEmpty()) {
            _messages.tryEmit("保存前请先修正：\n${problems.joinToString("\n")}")
            return
        }
        saving = true
        viewModelScope.launch {
            try {
                val ordered = current.steps.mapIndexed { index, step -> step.copy(order = index) }
                ServiceLocator.miniApps.save(
                    current.copy(steps = ordered, updatedAt = System.currentTimeMillis()),
                )
                runCatching { ServiceLocator.triggerManager.resync() }
                _saved.tryEmit(Unit)
            } catch (e: Exception) {
                _messages.tryEmit("保存失败：${e.message ?: "未知错误"}")
            } finally {
                saving = false
            }
        }
    }
}
