// Author: PKQ32
// Created: 2026-07-11
// Description: 小应用列表：赛博面板布局与中文文案；支持生成安装包

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.example.myapplication.domain.model.ActionStep
import com.example.myapplication.domain.model.ActionTypes
import com.example.myapplication.domain.model.MiniApp
import com.example.myapplication.export.ApkExporter
import com.example.myapplication.export.ApkInstaller
import com.example.myapplication.ui.theme.CyberCyan
import com.example.myapplication.ui.theme.CyberHeroHeader
import com.example.myapplication.ui.theme.CyberPanel
import com.example.myapplication.ui.theme.CyberStatusChip
import com.example.myapplication.ui.theme.CyberTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniAppListScreen(
    onOpenEditor: (String) -> Unit,
    onRun: (String) -> Unit,
    onOpenStandalone: (String) -> Unit = {},
    onOpenPermissions: () -> Unit = {},
) {
    val context = LocalContext.current
    val apps by ServiceLocator.miniApps.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var exportingId by remember { mutableStateOf<String?>(null) }

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
                onClick = {
                    scope.launch {
                        val id = UUID.randomUUID().toString()
                        val now = System.currentTimeMillis()
                        val app = MiniApp(
                            id = id,
                            name = "未命名小应用",
                            enabled = true,
                            createdAt = now,
                            updatedAt = now,
                            steps = listOf(
                                ActionStep(
                                    id = UUID.randomUUID().toString(),
                                    type = ActionTypes.DELAY,
                                    params = mapOf("millis" to "100"),
                                    order = 0,
                                ),
                            ),
                        )
                        ServiceLocator.miniApps.save(app)
                        onOpenEditor(id)
                    }
                },
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
                    subtitle = "创建小应用，编排手机动作并一键执行",
                )
                CyberPanel {
                    Text(
                        text = "暂无小应用",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "点击右下角「+」开始配置震动、音量、通知等动作",
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
                        subtitle = "共 ${apps.size} 个小应用 · 可生成独立安装包",
                    )
                }
                items(apps, key = { it.id }) { app ->
                    MiniAppListItem(
                        app = app,
                        exporting = exportingId == app.id,
                        onToggleEnabled = { enabled ->
                            scope.launch {
                                ServiceLocator.miniApps.save(
                                    app.copy(enabled = enabled, updatedAt = System.currentTimeMillis()),
                                )
                                ServiceLocator.triggerManager.resync()
                            }
                        },
                        onRun = { onRun(app.id) },
                        onOpenStandalone = { onOpenStandalone(app.id) },
                        onExportApk = {
                            if (exportingId != null) return@MiniAppListItem
                            scope.launch {
                                exportingId = app.id
                                try {
                                    if (!ApkInstaller.canRequestPackageInstalls(context)) {
                                        Toast.makeText(
                                            context,
                                            "请先允许「安装未知应用」，返回后再点生成",
                                            Toast.LENGTH_LONG,
                                        ).show()
                                        ApkInstaller.openUnknownSourcesSettings(context)
                                        return@launch
                                    }
                                    val apk = withContext(Dispatchers.IO) {
                                        ApkExporter.export(context, app)
                                    }
                                    Toast.makeText(
                                        context,
                                        "已生成「${app.name}」安装包（桌面名：迷你应用，重复安装会覆盖）",
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
                        onOpenEditor = { onOpenEditor(app.id) },
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
    onToggleEnabled: (Boolean) -> Unit,
    onRun: () -> Unit,
    onOpenStandalone: () -> Unit,
    onExportApk: () -> Unit,
    onOpenEditor: () -> Unit,
) {
    CyberPanel {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenEditor),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
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
            Button(onClick = onRun) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp),
                )
                Text("运行")
            }
            OutlinedButton(onClick = onOpenStandalone) {
                Text("控制台")
            }
            OutlinedButton(
                onClick = onExportApk,
                enabled = !exporting,
            ) {
                Text(if (exporting) "生成中…" else "生成安装包")
            }
            OutlinedButton(onClick = onOpenEditor) {
                Text("编辑")
            }
        }
    }
}
