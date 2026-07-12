// Author: PKQ32
// Created: 2026-07-11
// Description: 触发器表单：赛博面板 + 中英标签

package com.example.myapplication.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
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
import com.example.myapplication.ui.theme.CyberCyan
import com.example.myapplication.ui.theme.CyberPanel

@Composable
fun TriggerForm(
    trigger: Trigger,
    index: Int,
    onUpdate: (Trigger) -> Unit,
    onDelete: () -> Unit,
) {
    CyberPanel {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = triggerIcon(trigger.type),
                contentDescription = triggerTitleZh(trigger.type),
                modifier = Modifier.size(28.dp),
                tint = CyberCyan,
            )
            TypeLabelColumn(
                titleZh = "触发器 ${index + 1} · ${triggerTitleZh(trigger.type)}",
                typeKey = trigger.type,
            )
        }
        when (trigger.type) {
            TriggerTypes.SCHEDULE -> {
                OutlinedTextField(
                    value = trigger.params["intervalMinutes"].orEmpty(),
                    onValueChange = {
                        onUpdate(trigger.copy(params = trigger.params + ("intervalMinutes" to it)))
                    },
                    label = { Text("间隔分钟（最短 15）") },
                    supportingText = { Text("intervalMinutes", style = MaterialTheme.typography.labelSmall) },
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
                    label = { Text("系统事件（一期支持充电连接）") },
                    supportingText = { Text("action", style = MaterialTheme.typography.labelSmall) },
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
                    label = { Text("目标应用包名") },
                    supportingText = { Text("packageName", style = MaterialTheme.typography.labelSmall) },
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

fun triggerLabel(type: String): String = triggerTitleZh(type)

fun defaultParamsForTrigger(type: String): Map<String, String> = when (type) {
    TriggerTypes.SCHEDULE -> mapOf("intervalMinutes" to "15")
    TriggerTypes.EVENT -> mapOf("action" to "android.intent.action.ACTION_POWER_CONNECTED")
    TriggerTypes.APP_OPENED -> mapOf("packageName" to "")
    else -> emptyMap()
}
