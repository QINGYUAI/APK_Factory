// Author: PKQ32
// Created: 2026-07-12
// Description: 小应用运行历史：成功/失败摘要与步骤结果

package com.example.myapplication.ui.run

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.di.ServiceLocator
import com.example.myapplication.domain.model.RunLog
import com.example.myapplication.ui.editor.actionTitleZh
import com.example.myapplication.ui.theme.CyberCyan
import com.example.myapplication.ui.theme.CyberHeroHeader
import com.example.myapplication.ui.theme.CyberPanel
import com.example.myapplication.ui.theme.CyberStatusChip
import com.example.myapplication.ui.theme.CyberTopBar
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunHistoryScreen(
    miniAppId: String,
    appName: String,
    onBack: () -> Unit,
) {
    val logs by ServiceLocator.runLogs.observeForApp(miniAppId, limit = 50)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val timeFmt = remember {
        SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
    }
    val scope = rememberCoroutineScope()
    var confirmClear by remember { mutableStateOf(false) }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清空运行历史？") },
            text = { Text("将删除「${appName.ifBlank { "小应用" }}」的全部运行记录，且无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        scope.launch { ServiceLocator.runLogs.clearForApp(miniAppId) }
                    },
                ) {
                    Text("清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("取消") }
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CyberTopBar(
                title = "运行历史",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = CyberCyan,
                        )
                    }
                },
                actions = {
                    if (logs.isNotEmpty()) {
                        TextButton(onClick = { confirmClear = true }) {
                            Text("清空")
                        }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                CyberHeroHeader(
                    title = appName.ifBlank { "小应用" },
                    subtitle = if (logs.isEmpty()) "暂无运行记录" else "最近 ${logs.size} 次",
                )
            }
            if (logs.isEmpty()) {
                item {
                    CyberPanel {
                        Text("还没有运行记录", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "手动运行或触发器执行后会显示在这里",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(logs, key = { it.id }) { log ->
                RunLogCard(log = log, timeFmt = timeFmt)
            }
        }
    }
}

@Composable
private fun RunLogCard(log: RunLog, timeFmt: SimpleDateFormat) {
    val started = remember(log.startedAt) { timeFmt.format(Date(log.startedAt)) }
    val duration = remember(log.startedAt, log.endedAt) {
        val end = log.endedAt ?: return@remember "进行中"
        val ms = (end - log.startedAt).coerceAtLeast(0L)
        if (ms < 1000) "${ms}ms" else String.format(Locale.getDefault(), "%.1fs", ms / 1000.0)
    }
    val stepSummaries = remember(log.stepResultsJson) { parseStepSummaries(log.stepResultsJson) }

    CyberPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(started, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "耗时 $duration",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            CyberStatusChip(
                text = if (log.success) "成功" else "失败",
                active = log.success,
            )
        }
        if (stepSummaries.isEmpty()) {
            Text(
                text = "无步骤详情",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            stepSummaries.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun parseStepSummaries(json: String): List<String> {
    return runCatching {
        val arr = JSONArray(json)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val type = o.optString("type")
                val ok = o.optBoolean("ok", false)
                val title = actionTitleZh(type)
                if (ok) {
                    add("✓ $title")
                } else {
                    val msg = o.optString("message").ifBlank { o.optString("code") }
                    add("✗ $title · $msg")
                }
            }
        }
    }.getOrDefault(emptyList())
}
