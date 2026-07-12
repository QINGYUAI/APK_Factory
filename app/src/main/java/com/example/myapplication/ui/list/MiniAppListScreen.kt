// Author: PKQ32
// Created: 2026-07-11
// Description: 小应用列表：增删、运行；「添加到桌面」即生成可安装软件

package com.example.myapplication.ui.list

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myapplication.di.ServiceLocator
import com.example.myapplication.domain.model.MiniApp
import com.example.myapplication.export.ApkExporter
import com.example.myapplication.export.ApkInstaller
import com.example.myapplication.export.ConfigSyncPusher
import com.example.myapplication.export.ExportPreflight
import com.example.myapplication.export.ExportVersionHelper
import com.example.myapplication.ui.icons.AppIconCatalog
import com.example.myapplication.ui.theme.CyberCyan
import com.example.myapplication.ui.theme.CyberHeroHeader
import com.example.myapplication.ui.theme.CyberPanel
import com.example.myapplication.ui.theme.CyberStatusChip
import com.example.myapplication.ui.theme.CyberTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniAppListScreen(
    onOpenEditor: (String) -> Unit,
    onCreate: () -> Unit = {},
    onRun: (String) -> Unit,
    onOpenStandalone: (String) -> Unit = {},
    onOpenHistory: (String) -> Unit = {},
    onOpenPermissions: () -> Unit = {},
) {
    val context = LocalContext.current
    val apps by ServiceLocator.miniApps.observeAll().collectAsState(initial = emptyList())
    val runningId by ServiceLocator.runner.runningId.collectAsState()
    val scope = rememberCoroutineScope()
    var exportingId by remember { mutableStateOf<String?>(null) }
    var syncingId by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<MiniApp?>(null) }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除小应用") },
            text = {
                Text("确定删除「${target.name}」？删除后无法恢复，相关定时触发也会取消。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            ServiceLocator.miniApps.delete(target.id)
                            ServiceLocator.triggerManager.resync()
                            pendingDelete = null
                            Toast.makeText(context, "已删除「${target.name}」", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("取消")
                }
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CyberTopBar(
                title = "应用工厂",
                actions = {
                    IconButton(onClick = onOpenPermissions) {
                        Icon(
                            Icons.Filled.Security,
                            contentDescription = "权限中心",
                            tint = CyberCyan,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreate,
                containerColor = CyberCyan,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "新建小应用")
            }
        },
    ) { padding ->
        if (apps.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CyberHeroHeader(
                    title = "自动化控制台",
                    subtitle = "创建小应用，编排手机动作；点「添加到桌面」生成独立安装包",
                )
                CyberPanel {
                    Text(
                        text = "暂无小应用",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "本应用是「应用工厂」。点右下角「+」按向导创建（选模板与触发器），再用「添加到桌面」安装",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    CyberHeroHeader(
                        title = "自动化控制台",
                    subtitle = "共 ${apps.size} 个小应用 · 已安装可用「同步配置」热更新，或「更新应用」重装",
                    )
                }
                    items(apps, key = { it.id }) { app ->
                    val installed = remember(app.id, app.updatedAt, exportingId) {
                        ExportVersionHelper.isInstalled(context, app)
                    }
                    MiniAppListItem(
                        app = app,
                        exporting = exportingId == app.id,
                        syncing = syncingId == app.id,
                        installed = installed,
                        isRunning = runningId == app.id,
                        onToggleEnabled = { enabled ->
                            scope.launch {
                                ServiceLocator.miniApps.save(
                                    app.copy(enabled = enabled, updatedAt = System.currentTimeMillis()),
                                )
                                ServiceLocator.triggerManager.resync()
                            }
                        },
                        onRun = { onRun(app.id) },
                        onStop = { ServiceLocator.runner.stop() },
                        onOpenStandalone = { onOpenStandalone(app.id) },
                        onSyncConfig = {
                            if (syncingId != null || exportingId != null) return@MiniAppListItem
                            scope.launch {
                                syncingId = app.id
                                try {
                                    val preflight = withContext(Dispatchers.IO) {
                                        ExportPreflight.check(context, app)
                                    }
                                    if (!preflight.canExport) {
                                        Toast.makeText(
                                            context,
                                            preflight.errors.joinToString("\n"),
                                            Toast.LENGTH_LONG,
                                        ).show()
                                        return@launch
                                    }
                                    val result = withContext(Dispatchers.IO) {
                                        ConfigSyncPusher.sync(context, app)
                                    }
                                    when (result) {
                                        ConfigSyncPusher.Result.Success -> {
                                            Toast.makeText(
                                                context,
                                                "正在同步「${app.name}」配置（无需重装）",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        }
                                        is ConfigSyncPusher.Result.Failed -> {
                                            Toast.makeText(
                                                context,
                                                result.message,
                                                Toast.LENGTH_LONG,
                                            ).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "同步失败：${e.message ?: "未知错误"}",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                } finally {
                                    syncingId = null
                                }
                            }
                        },
                        onAddToDesktop = {
                            // 「添加到桌面」= 生成可安装 APK 并调起系统安装
                            if (exportingId != null) return@MiniAppListItem
                            scope.launch {
                                exportingId = app.id
                                try {
                                    if (!ApkInstaller.canRequestPackageInstalls(context)) {
                                        Toast.makeText(
                                            context,
                                            "请先允许「安装未知应用」，返回后再点「添加到桌面」",
                                            Toast.LENGTH_LONG,
                                        ).show()
                                        ApkInstaller.openUnknownSourcesSettings(context)
                                        return@launch
                                    }
                                    val preflight = withContext(Dispatchers.IO) {
                                        ExportPreflight.check(context, app)
                                    }
                                    if (!preflight.canExport) {
                                        Toast.makeText(
                                            context,
                                            preflight.errors.joinToString("\n"),
                                            Toast.LENGTH_LONG,
                                        ).show()
                                        return@launch
                                    }
                                    if (preflight.warnings.isNotEmpty()) {
                                        Toast.makeText(
                                            context,
                                            preflight.warnings.joinToString("\n"),
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    }
                                    val apk = withContext(Dispatchers.IO) {
                                        ApkExporter.export(context, app)
                                    }
                                    val updating = ExportVersionHelper.isInstalled(context, app)
                                    Toast.makeText(
                                        context,
                                        if (updating) {
                                            "正在更新「${app.name}」（覆盖原安装包，不会新增图标）"
                                        } else {
                                            "已生成「${app.name}」安装包，请完成安装"
                                        },
                                        Toast.LENGTH_LONG,
                                    ).show()
                                    ApkInstaller.install(context, apk)
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "生成失败：${e.message ?: "未知错误"}",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                } finally {
                                    exportingId = null
                                }
                            }
                        },
                        onDelete = { pendingDelete = app },
                        onOpenEditor = { onOpenEditor(app.id) },
                        onOpenHistory = { onOpenHistory(app.id) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MiniAppListItem(
    app: MiniApp,
    exporting: Boolean,
    syncing: Boolean,
    installed: Boolean,
    isRunning: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onRun: () -> Unit,
    onStop: () -> Unit,
    onOpenStandalone: () -> Unit,
    onSyncConfig: () -> Unit,
    onAddToDesktop: () -> Unit,
    onDelete: () -> Unit,
    onOpenEditor: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    CyberPanel {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenEditor),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = AppIconCatalog.iconOf(app.iconKey),
                contentDescription = null,
                tint = CyberCyan,
                modifier = Modifier.size(28.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "${app.steps.size} 个动作 · ${app.triggers.size} 个触发器",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isRunning) {
                CyberStatusChip(text = "运行中", active = true)
            }
            CyberStatusChip(
                text = if (app.enabled) "已启用" else "已停用",
                active = app.enabled,
            )
            Switch(
                checked = app.enabled,
                onCheckedChange = onToggleEnabled,
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isRunning) {
                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(
                        Icons.Filled.Stop,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Text("停止")
                }
            } else {
                Button(onClick = onRun) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Text("运行")
                }
            }
            OutlinedButton(onClick = onOpenStandalone) {
                Text("控制台")
            }
            if (installed) {
                OutlinedButton(
                    onClick = onSyncConfig,
                    enabled = !exporting && !syncing,
                ) {
                    Text(if (syncing) "同步中…" else "同步配置")
                }
            }
            OutlinedButton(
                onClick = onAddToDesktop,
                enabled = !exporting && !syncing,
            ) {
                Text(
                    when {
                        exporting && installed -> "更新中…"
                        exporting -> "生成中…"
                        installed -> "更新应用"
                        else -> "添加到桌面"
                    },
                )
            }
            OutlinedButton(onClick = onOpenEditor) {
                Text("编辑")
            }
            OutlinedButton(onClick = onOpenHistory) {
                Text("历史")
            }
            OutlinedButton(
                onClick = onDelete,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("删除")
            }
        }
    }
}
