// Author: PKQ32
// Created: 2026-11-16
// Description: 新建向导状态层：三步表单、模板/触发器选择与保存

package com.example.myapplication.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.di.ServiceLocator
import com.example.myapplication.domain.model.MiniApp
import com.example.myapplication.domain.model.Trigger
import com.example.myapplication.domain.model.TriggerTypes
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class CreateWizardUiState(
    val step: Int = 0,
    val name: String = "",
    val iconKey: String = "default",
    val templateKey: String = "acceptance",
    val triggerKey: String = "manual",
    val saving: Boolean = false,
)

data class CreatedMiniApp(
    val id: String,
    val runNow: Boolean,
)

class CreateMiniAppViewModel : ViewModel() {

    private val _state = MutableStateFlow(CreateWizardUiState())
    val state: StateFlow<CreateWizardUiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages

    private val _created = MutableSharedFlow<CreatedMiniApp>(extraBufferCapacity = 1)
    val created: SharedFlow<CreatedMiniApp> = _created

    fun setName(value: String) = _state.update { it.copy(name = value) }

    fun setIconKey(value: String) = _state.update { it.copy(iconKey = value) }

    fun setTemplateKey(value: String) = _state.update { it.copy(templateKey = value) }

    fun setTriggerKey(value: String) = _state.update { it.copy(triggerKey = value) }

    fun nextStep() {
        val current = _state.value
        if (current.step == 0 && current.name.isBlank()) {
            _messages.tryEmit("请填写应用名称")
            return
        }
        _state.update { it.copy(step = (it.step + 1).coerceAtMost(2)) }
    }

    /** @return false 表示已在第一步，调用方应执行返回导航 */
    fun previousStep(): Boolean {
        val current = _state.value
        if (current.step == 0) return false
        _state.update { it.copy(step = it.step - 1) }
        return true
    }

    fun save(runNow: Boolean) {
        if (_state.value.saving) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            try {
                val id = persist()
                runCatching { ServiceLocator.triggerManager.resync() }
                _created.tryEmit(CreatedMiniApp(id = id, runNow = runNow))
            } catch (e: Exception) {
                _messages.tryEmit("保存失败：${e.message ?: "未知错误"}")
            } finally {
                _state.update { it.copy(saving = false) }
            }
        }
    }

    private suspend fun persist(): String {
        val current = _state.value
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val template = ActionTemplates.all.first { it.key == current.templateKey }
        val option = TriggerOptions.wizardChoices.first { it.key == current.triggerKey }
        val triggers = mutableListOf<Trigger>()
        var runOnHostOpen = false
        when (option.kind) {
            TriggerOption.Kind.Manual -> Unit
            TriggerOption.Kind.HostOpen -> runOnHostOpen = true
            TriggerOption.Kind.TriggerType -> {
                TriggerOptions.buildTrigger(current.triggerKey)?.let { triggers.add(it) }
            }
        }
        if (runOnHostOpen) {
            triggers.add(
                Trigger(
                    id = UUID.randomUUID().toString(),
                    type = TriggerTypes.HOST_OPENED,
                    enabled = true,
                ),
            )
        }
        val app = MiniApp(
            id = id,
            name = current.name.trim().ifBlank { "未命名小应用" },
            iconKey = current.iconKey,
            enabled = true,
            runOnHostOpen = runOnHostOpen,
            createdAt = now,
            updatedAt = now,
            steps = template.buildSteps(),
            triggers = triggers,
        )
        ServiceLocator.miniApps.save(app)
        return id
    }
}
