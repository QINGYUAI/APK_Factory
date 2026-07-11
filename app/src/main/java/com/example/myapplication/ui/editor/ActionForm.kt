// Author: PKQ32
// Created: 2026-07-11
// Description: 动作步骤表单：按类型编辑参数、失败策略与排序删除

package com.example.myapplication.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.domain.FailurePolicy
import com.example.myapplication.domain.model.ActionStep
import com.example.myapplication.domain.model.ActionTypes

/**
 * 单个动作步骤的编辑区块。
 */
@Composable
fun ActionForm(
    step: ActionStep,
    index: Int,
    total: Int,
    onUpdate: (ActionStep) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "步骤 ${index + 1} · ${step.type}",
            style = MaterialTheme.typography.titleSmall,
        )
        ActionParamsFields(
            type = step.type,
            params = step.params,
            onParamsChange = { onUpdate(step.copy(params = it)) },
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("失败时")
            FilterChip(
                selected = step.onFailure == FailurePolicy.SKIP,
                onClick = { onUpdate(step.copy(onFailure = FailurePolicy.SKIP)) },
                label = { Text("跳过") },
            )
            FilterChip(
                selected = step.onFailure == FailurePolicy.ABORT,
                onClick = { onUpdate(step.copy(onFailure = FailurePolicy.ABORT)) },
                label = { Text("中止") },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onMoveUp, enabled = index > 0) {
                Text("上移")
            }
            TextButton(onClick = onMoveDown, enabled = index < total - 1) {
                Text("下移")
            }
            TextButton(onClick = onDelete) {
                Text("删除")
            }
        }
    }
}

@Composable
private fun ActionParamsFields(
    type: String,
    params: Map<String, String>,
    onParamsChange: (Map<String, String>) -> Unit,
) {
    fun update(key: String, value: String) {
        onParamsChange(params + (key to value))
    }

    when (type) {
        ActionTypes.VIBRATE, ActionTypes.DELAY -> {
            OutlinedTextField(
                value = params["millis"].orEmpty(),
                onValueChange = { update("millis", it) },
                label = { Text("毫秒 (millis)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        ActionTypes.SET_VOLUME -> {
            OutlinedTextField(
                value = params["stream"].orEmpty(),
                onValueChange = { update("stream", it) },
                label = { Text("声道 (music/ring)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = params["percent"].orEmpty(),
                onValueChange = { update("percent", it) },
                label = { Text("音量百分比 (0-100)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        ActionTypes.NOTIFY -> {
            OutlinedTextField(
                value = params["title"].orEmpty(),
                onValueChange = { update("title", it) },
                label = { Text("标题 (title)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = params["text"].orEmpty(),
                onValueChange = { update("text", it) },
                label = { Text("内容 (text)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        ActionTypes.OPEN_APP -> {
            OutlinedTextField(
                value = params["packageName"].orEmpty(),
                onValueChange = { update("packageName", it) },
                label = { Text("包名 (packageName)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        ActionTypes.FLASHLIGHT, ActionTypes.WIFI, ActionTypes.BLUETOOTH -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("开启 (enabled)")
                Switch(
                    checked = params["enabled"] == "true",
                    onCheckedChange = { update("enabled", it.toString()) },
                )
            }
        }
        ActionTypes.SET_BRIGHTNESS -> {
            OutlinedTextField(
                value = params["percent"].orEmpty(),
                onValueChange = { update("percent", it) },
                label = { Text("亮度百分比 (0-100)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        ActionTypes.PLAY_MUSIC -> {
            OutlinedTextField(
                value = params["uri"].orEmpty(),
                onValueChange = { update("uri", it) },
                label = { Text("音频 URI") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        ActionTypes.SET_RINGER_MODE -> {
            OutlinedTextField(
                value = params["mode"].orEmpty(),
                onValueChange = { update("mode", it) },
                label = { Text("模式 (silent/vibrate/normal)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        ActionTypes.SCREENSHOT -> {
            Text(
                text = "截图无需参数",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        else -> {
            OutlinedTextField(
                value = params.entries.joinToString("\n") { "${it.key}=${it.value}" },
                onValueChange = { raw ->
                    val map = raw.lines()
                        .mapNotNull { line ->
                            val idx = line.indexOf('=')
                            if (idx <= 0) null
                            else line.substring(0, idx).trim() to line.substring(idx + 1).trim()
                        }
                        .toMap()
                    onParamsChange(map)
                },
                label = { Text("参数 (key=value，每行一条)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
        }
    }
}

/**
 * 按动作类型给出合理默认参数。
 */
fun defaultParamsForType(type: String): Map<String, String> = when (type) {
    ActionTypes.VIBRATE -> mapOf("millis" to "200")
    ActionTypes.DELAY -> mapOf("millis" to "1000")
    ActionTypes.SET_VOLUME -> mapOf("stream" to "music", "percent" to "50")
    ActionTypes.NOTIFY -> mapOf("title" to "通知", "text" to "小应用执行完成")
    ActionTypes.OPEN_APP -> mapOf("packageName" to "")
    ActionTypes.FLASHLIGHT, ActionTypes.WIFI, ActionTypes.BLUETOOTH -> mapOf("enabled" to "true")
    ActionTypes.SET_BRIGHTNESS -> mapOf("percent" to "50")
    ActionTypes.PLAY_MUSIC -> mapOf("uri" to "")
    ActionTypes.SET_RINGER_MODE -> mapOf("mode" to "vibrate")
    else -> emptyMap()
}
