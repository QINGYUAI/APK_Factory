// Author: PKQ32
// Created: 2026-07-11
// Description: 触发器表单：定时/事件/打开应用参数编辑与启用删除

package com.example.myapplication.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.domain.model.Trigger
import com.example.myapplication.domain.model.TriggerTypes

/**
 * 单个触发器的编辑区块。
 */
@Composable
fun TriggerForm(
    trigger: Trigger,
    index: Int,
    onUpdate: (Trigger) -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "触发器 ${index + 1} · ${triggerLabel(trigger.type)}",
            style = MaterialTheme.typography.titleSmall,
        )
        when (trigger.type) {
            TriggerTypes.SCHEDULE -> {
                OutlinedTextField(
                    value = trigger.params["intervalMinutes"].orEmpty(),
                    onValueChange = {
                        onUpdate(trigger.copy(params = trigger.params + ("intervalMinutes" to it)))
                    },
                    label = { Text("间隔分钟 (intervalMinutes)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            TriggerTypes.EVENT -> {
                OutlinedTextField(
                    value = trigger.params["action"].orEmpty(),
                    onValueChange = {
                        onUpdate(trigger.copy(params = trigger.params + ("action" to it)))
                    },
                    label = { Text("广播 Action") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            TriggerTypes.APP_OPENED -> {
                OutlinedTextField(
                    value = trigger.params["packageName"].orEmpty(),
                    onValueChange = {
                        onUpdate(trigger.copy(params = trigger.params + ("packageName" to it)))
                    },
                    label = { Text("包名 (packageName)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            else -> {
                Text(
                    text = "类型：${trigger.type}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("启用")
            Switch(
                checked = trigger.enabled,
                onCheckedChange = { onUpdate(trigger.copy(enabled = it)) },
            )
            TextButton(onClick = onDelete) {
                Text("删除")
            }
        }
    }
}

fun triggerLabel(type: String): String = when (type) {
    TriggerTypes.SCHEDULE -> "定时"
    TriggerTypes.EVENT -> "系统事件"
    TriggerTypes.APP_OPENED -> "打开应用"
    TriggerTypes.MANUAL -> "手动"
    TriggerTypes.HOST_OPENED -> "打开本应用"
    else -> type
}

fun defaultParamsForTrigger(type: String): Map<String, String> = when (type) {
    TriggerTypes.SCHEDULE -> mapOf("intervalMinutes" to "15")
    TriggerTypes.EVENT -> mapOf("action" to "android.intent.action.ACTION_POWER_CONNECTED")
    TriggerTypes.APP_OPENED -> mapOf("packageName" to "")
    else -> emptyMap()
}
