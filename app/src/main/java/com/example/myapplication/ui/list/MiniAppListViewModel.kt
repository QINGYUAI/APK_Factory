// Author: PKQ32
// Created: 2026-11-16
// Description: 列表页状态层：小应用流、运行状态、导出/同步与提示事件

package com.example.myapplication.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.di.ServiceLocator
import com.example.myapplication.domain.model.MiniApp
import com.example.myapplication.export.ApkExporter
import com.example.myapplication.export.ApkInstaller
import com.example.myapplication.export.ConfigSyncPusher
import com.example.myapplication.export.ExportPreflight
import com.example.myapplication.export.ExportVersionHelper
import com.example.myapplication.platform.ShortcutHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MiniAppListViewModel : ViewModel() {

    private val repo get() = ServiceLocator.miniApps
    private val context get() = ServiceLocator.application

    val apps: StateFlow<List<MiniApp>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val runningId: StateFlow<String?> = ServiceLocator.runner.runningId

    private val _exportingId = MutableStateFlow<String?>(null)
    val exportingId: StateFlow<String?> = _exportingId.asStateFlow()

    private val _syncingId = MutableStateFlow<String?>(null)
    val syncingId: StateFlow<String?> = _syncingId.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages

    fun setEnabled(app: MiniApp, enabled: Boolean) {
        viewModelScope.launch {
            repo.save(app.copy(enabled = enabled, updatedAt = System.currentTimeMillis()))
            runCatching { ServiceLocator.triggerManager.resync() }
        }
    }

    fun delete(app: MiniApp) {
        viewModelScope.launch {
            repo.delete(app.id)
            // 一并清理该小应用的运行历史与桌面快捷方式
            runCatching { ServiceLocator.runLogs.clearForApp(app.id) }
            runCatching { ShortcutHelper.removeShortcut(context, app.id) }
            runCatching { ServiceLocator.triggerManager.resync() }
            post("已删除「${app.name}」")
        }
    }

    fun stop() = ServiceLocator.runner.stop()

    fun syncConfig(app: MiniApp) {
        if (_syncingId.value != null || _exportingId.value != null) return
        viewModelScope.launch {
            _syncingId.value = app.id
            try {
                val preflight = withContext(Dispatchers.IO) { ExportPreflight.check(context, app) }
                if (!preflight.canExport) {
                    post(preflight.errors.joinToString("\n"))
                    return@launch
                }
                val result = withContext(Dispatchers.IO) { ConfigSyncPusher.sync(context, app) }
                when (result) {
                    ConfigSyncPusher.Result.Success ->
                        post("正在同步「${app.name}」配置（无需重装）")
                    is ConfigSyncPusher.Result.Failed -> post(result.message)
                }
            } catch (e: Exception) {
                post("同步失败：${e.message ?: "未知错误"}")
            } finally {
                _syncingId.value = null
            }
        }
    }

    fun exportAndInstall(app: MiniApp) {
        if (_exportingId.value != null) return
        viewModelScope.launch {
            _exportingId.value = app.id
            try {
                if (!ApkInstaller.canRequestPackageInstalls(context)) {
                    post("请先允许「安装未知应用」，返回后再点「添加到桌面」")
                    ApkInstaller.openUnknownSourcesSettings(context)
                    return@launch
                }
                val preflight = withContext(Dispatchers.IO) { ExportPreflight.check(context, app) }
                if (!preflight.canExport) {
                    post(preflight.errors.joinToString("\n"))
                    return@launch
                }
                if (preflight.warnings.isNotEmpty()) {
                    post(preflight.warnings.joinToString("\n"))
                }
                val updating = ExportVersionHelper.isInstalled(context, app)
                val apk = withContext(Dispatchers.IO) { ApkExporter.export(context, app) }
                post(
                    if (updating) {
                        "正在更新「${app.name}」（覆盖原安装包，不会新增图标）"
                    } else {
                        "已生成「${app.name}」安装包，请完成安装"
                    },
                )
                ApkInstaller.install(context, apk)
            } catch (e: Exception) {
                post("生成失败：${e.message ?: "未知错误"}")
            } finally {
                _exportingId.value = null
            }
        }
    }

    private fun post(message: String) {
        _messages.tryEmit(message)
    }
}
