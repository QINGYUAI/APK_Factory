// Author: PKQ32
// Created: 2026-07-11
// Description: 动作步骤表单：图标化卡片、中英文标签、参数与失败策略

package com.example.myapplication.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myapplication.domain.FailurePolicy
import com.example.myapplication.domain.model.ActionStep
import com.example.myapplication.domain.model.ActionTypes
import com.example.myapplication.platform.MediaUriHelper
import com.example.myapplication.ui.theme.CyberCyan
import com.example.myapplication.ui.theme.CyberPanel
import android.net.Uri

/**
 * 单个动作步骤的编辑卡片（图标 + 中文主文案 + 英文引文）。
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
    CyberPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.border(1.dp, CyberCyan.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
            ) {
                Text(
                    text = "${index + 1}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = CyberCyan,
                )
            }
            Icon(
                imageVector = actionIcon(step.type),
                contentDescription = actionTitleZh(step.type),
                modifier = Modifier.size(28.dp),
                tint = CyberCyan,
            )
            TypeLabelColumn(
                titleZh = actionTitleZh(step.type),
                typeKey = step.type,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onMoveUp, enabled = index > 0) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "上移", tint = CyberCyan)
            }
            IconButton(onClick = onMoveDown, enabled = index < total - 1) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "下移", tint = CyberCyan)
            }
        }
        ActionParamsFields(
            type = step.type,
            params = step.params,
            onParamsChange = { onUpdate(step.copy(params = it)) },
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("失败时", style = MaterialTheme.typography.bodyMedium)
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
                label = { Text("毫秒 · millis") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        ActionTypes.SET_VOLUME -> {
            OutlinedTextField(
                value = params["stream"].orEmpty(),
                onValueChange = { update("stream", it) },
                label = { Text("声道 · music / ring") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = params["percent"].orEmpty(),
                onValueChange = { update("percent", it) },
                label = { Text("音量百分比 · 0–100") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        ActionTypes.NOTIFY -> {
            OutlinedTextField(
                value = params["title"].orEmpty(),
                onValueChange = { update("title", it) },
                label = { Text("标题 · title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = params["text"].orEmpty(),
                onValueChange = { update("text", it) },
                label = { Text("内容 · text") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        ActionTypes.OPEN_APP -> {
            OutlinedTextField(
                value = params["packageName"].orEmpty(),
                onValueChange = { update("packageName", it) },
                label = { Text("包名 · packageName") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        ActionTypes.FLASHLIGHT, ActionTypes.WIFI, ActionTypes.BLUETOOTH -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("开启 · enabled")
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
                label = { Text("亮度百分比 · 0–100") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        ActionTypes.PLAY_MUSIC -> {
            MediaFilePickerField(
                mimeTypes = arrayOf("audio/*"),
                pickButtonLabel = "从系统选择音乐",
                emptyHint = "尚未选择音频文件",
                params = params,
                onParamsChange = onParamsChange,
            )
        }
        ActionTypes.PLAY_VIDEO -> {
            MediaFilePickerField(
                mimeTypes = arrayOf("video/*"),
                pickButtonLabel = "从系统选择视频",
                emptyHint = "尚未选择视频文件",
                params = params,
                onParamsChange = onParamsChange,
            )
        }
        ActionTypes.SET_RINGER_MODE -> {
            OutlinedTextField(
                value = params["mode"].orEmpty(),
                onValueChange = { update("mode", it) },
                label = { Text("模式 · silent / vibrate / normal") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        ActionTypes.SCREENSHOT -> {
            Text(
                text = "截图暂无需参数（需系统授权）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
 * 通过系统文件选择器选取媒体，并写入 uri / displayName。
 */
@Composable
private fun MediaFilePickerField(
    mimeTypes: Array<String>,
    pickButtonLabel: String,
    emptyHint: String,
    params: Map<String, String>,
    onParamsChange: (Map<String, String>) -> Unit,
) {
    val context = LocalContext.current
    val displayName = params["displayName"].orEmpty().ifBlank {
        params["path"]?.takeIf { it.isNotBlank() && !MediaUriHelper.isAssetPath(it) }
            ?.let { java.io.File(it).name }
            ?: params["uri"]?.let { MediaUriHelper.displayName(context, it) }.orEmpty()
    }
    val hasMedia = params["path"].orEmpty().isNotBlank() || params["uri"].orEmpty().isNotBlank()
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        MediaUriHelper.takePersistableRead(context, uri)
        val name = MediaUriHelper.displayName(context, uri.toString()).orEmpty()
        // 复制到私有目录，工厂内可直接播；导出时再嵌入 APK（content URI 权限不跨应用）
        val copied = runCatching { MediaUriHelper.copyToPrivateMedia(context, uri) }.getOrElse { err ->
            Toast.makeText(
                context,
                err.message ?: "复制媒体失败，请换一个文件重试",
                Toast.LENGTH_LONG,
            ).show()
            return@rememberLauncherForActivityResult
        }
        onParamsChange(
            params + mapOf(
                "uri" to "",
                "displayName" to name.ifBlank { copied.name },
                "path" to copied.absolutePath,
            ),
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (displayName.isNotBlank()) "已选：$displayName" else emptyHint,
            style = MaterialTheme.typography.bodyMedium,
            color = if (displayName.isNotBlank()) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        if (params["path"].orEmpty().isNotBlank()) {
            Text(
                text = "已保存到应用内，导出时将一并打包",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        } else if (params["uri"].orEmpty().isNotBlank()) {
            Text(
                text = "旧版仅存链接，请重新选择文件后再导出",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { picker.launch(mimeTypes) }) {
                Icon(Icons.Filled.FolderOpen, contentDescription = null)
                Text(pickButtonLabel, modifier = Modifier.padding(start = 6.dp))
            }
            if (hasMedia) {
                OutlinedButton(
                    onClick = {
                        onParamsChange(params + mapOf("uri" to "", "displayName" to "", "path" to ""))
                    },
                ) {
                    Text("清除")
                }
            }
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
    ActionTypes.PLAY_MUSIC -> mapOf("uri" to "", "displayName" to "", "path" to "")
    ActionTypes.PLAY_VIDEO -> mapOf("uri" to "", "displayName" to "", "path" to "")
    ActionTypes.SET_RINGER_MODE -> mapOf("mode" to "vibrate")
    else -> emptyMap()
}
