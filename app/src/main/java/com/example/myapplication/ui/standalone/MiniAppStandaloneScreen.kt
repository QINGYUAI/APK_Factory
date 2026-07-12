// Author: PKQ32
// Created: 2026-07-12
// Description: 桌面快捷方式独立入口：可运行、可配置、可进权限中心

package com.example.myapplication.ui.standalone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.di.ServiceLocator
import com.example.myapplication.domain.model.MiniApp
import com.example.myapplication.ui.editor.actionIcon
import com.example.myapplication.ui.editor.actionTitleZh
import com.example.myapplication.ui.editor.triggerIcon
import com.example.myapplication.ui.editor.triggerTitleZh
import com.example.myapplication.ui.theme.CyberCyan
import com.example.myapplication.ui.theme.CyberHeroHeader
import com.example.myapplication.ui.theme.CyberPanel
import com.example.myapplication.ui.theme.CyberSectionTitle
import com.example.myapplication.ui.theme.CyberStatusChip
import com.example.myapplication.ui.theme.CyberTopBar
import kotlinx.coroutines.launch

/**
 * 通过桌面快捷方式 / deep link 打开单个小应用时的控制台：
 * 可立即运行、进入配置编辑、管理权限。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniAppStandaloneScreen(
    miniAppId: String,
    onRun: () -> Unit,
    onConfigure: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenFactory: () -> Unit,
) {
    var app by remember { mutableStateOf<MiniApp?>(null) }
    var missing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(miniAppId) {
        val loaded = ServiceLocator.miniApps.get(miniAppId)
        if (loaded == null) {
            missing = true
        } else {
            app = loaded
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CyberTopBar(
                title = app?.name ?: "小应用控制台",
                navigationIcon = {
                    IconButton(onClick = onOpenFactory) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回工厂",
                            tint = CyberCyan,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenPermissions) {
                        Icon(Icons.Filled.Settings, contentDescription = "权限中心", tint = CyberCyan)
                    }
                },
            )
        },
    ) { padding ->
        when {
            missing -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("未找到该小应用", style = MaterialTheme.typography.titleMedium)
                    Button(onClick = onOpenFactory) { Text("返回应用工厂") }
                }
            }
            app == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("加载中…")
                }
            }
            else -> {
                val current = app!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item {
                        CyberHeroHeader(
                            title = current.name,
                            subtitle = "独立入口 · 可运行也可配置动作与触发器",
                        )
                    }
                    item {
                        CyberPanel {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("启用状态", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = if (current.enabled) "触发器与手动运行均可用" else "已停用，不会被触发执行",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                CyberStatusChip(
                                    text = if (current.enabled) "已启用" else "已停用",
                                    active = current.enabled,
                                )
                                Switch(
                                    checked = current.enabled,
                                    onCheckedChange = { enabled ->
                                        scope.launch {
                                            val updated = current.copy(
                                                enabled = enabled,
                                                updatedAt = System.currentTimeMillis(),
                                            )
                                            ServiceLocator.miniApps.save(updated)
                                            ServiceLocator.triggerManager.resync()
                                            app = updated
                                        }
                                    },
                                )
                            }
                        }
                    }
                    item {
                        CyberPanel {
                            Text("快捷操作", style = MaterialTheme.typography.titleSmall)
                            Button(
                                onClick = onRun,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = current.enabled,
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Text("立即运行", modifier = Modifier.padding(start = 6.dp))
                            }
                            OutlinedButton(
                                onClick = onConfigure,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Filled.Tune, contentDescription = null, tint = CyberCyan)
                                Text("配置动作与触发器", modifier = Modifier.padding(start = 6.dp))
                            }
                            OutlinedButton(
                                onClick = onOpenPermissions,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("打开权限中心")
                            }
                            OutlinedButton(
                                onClick = onOpenFactory,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("返回应用工厂首页")
                            }
                        }
                    }
                    item { CyberSectionTitle("动作预览") }
                    item {
                        if (current.steps.isEmpty()) {
                            CyberPanel {
                                Text("暂无动作，请先进入配置添加")
                            }
                        } else {
                            current.steps.sortedBy { it.order }.forEachIndexed { index, step ->
                                CyberPanel(modifier = Modifier.padding(bottom = 8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Icon(
                                            actionIcon(step.type),
                                            contentDescription = null,
                                            tint = CyberCyan,
                                        )
                                        Column {
                                            Text(
                                                text = "${index + 1}. ${actionTitleZh(step.type)}",
                                                style = MaterialTheme.typography.titleSmall,
                                            )
                                            Text(
                                                text = step.type,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { CyberSectionTitle("触发器预览") }
                    item {
                        if (current.triggers.isEmpty()) {
                            CyberPanel {
                                Text("暂无触发器（仍可手动运行）")
                            }
                        } else {
                            current.triggers.forEachIndexed { index, trigger ->
                                CyberPanel(modifier = Modifier.padding(bottom = 8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Icon(
                                            triggerIcon(trigger.type),
                                            contentDescription = null,
                                            tint = CyberCyan,
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${index + 1}. ${triggerTitleZh(trigger.type)}",
                                                style = MaterialTheme.typography.titleSmall,
                                            )
                                            Text(
                                                text = trigger.type,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        CyberStatusChip(
                                            text = if (trigger.enabled) "启用" else "关闭",
                                            active = trigger.enabled,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
