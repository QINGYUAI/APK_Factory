// Author: PKQ32
// Created: 2026-07-11
// Description: 触发器表单：事件、日程（一次/每日/每周/时段）与例外日期

package com.example.myapplication.ui.editor

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myapplication.domain.model.Trigger
import com.example.myapplication.domain.model.TriggerTypes
import com.example.myapplication.platform.PermissionHelper
import com.example.myapplication.platform.ScheduleTimeMath
import com.example.myapplication.platform.ScheduleValidation
import com.example.myapplication.platform.SystemEventCatalog
import com.example.myapplication.ui.theme.CyberCyan
import com.example.myapplication.ui.theme.CyberPanel
import java.util.Calendar

@Composable
fun TriggerForm(
    trigger: Trigger,
    index: Int,
    onUpdate: (Trigger) -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
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
                    supportingText = {
                        Text("系统 PeriodicWork 下限 15 分钟", style = MaterialTheme.typography.labelSmall)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            TriggerTypes.ONCE_AT -> {
                OnceAtFields(trigger = trigger, onUpdate = onUpdate)
                ExcludeDatesField(trigger = trigger, onUpdate = onUpdate)
                ScheduleStatusBlock(trigger = trigger)
                ScheduleHint()
            }
            TriggerTypes.DAILY_AT, TriggerTypes.WEEKLY -> {
                HourMinuteRow(trigger = trigger, onUpdate = onUpdate)
                DaysOfWeekPicker(trigger = trigger, onUpdate = onUpdate)
                ExcludeDatesField(trigger = trigger, onUpdate = onUpdate)
                ScheduleStatusBlock(trigger = trigger)
                ScheduleHint()
            }
            TriggerTypes.WINDOW_REPEAT -> {
                WindowRepeatFields(trigger = trigger, onUpdate = onUpdate)
                DaysOfWeekPicker(trigger = trigger, onUpdate = onUpdate)
                ExcludeDatesField(trigger = trigger, onUpdate = onUpdate)
                ScheduleStatusBlock(trigger = trigger)
                ScheduleHint()
            }
            TriggerTypes.EVENT -> {
                var menuOpen by remember { mutableStateOf(false) }
                val currentAction = trigger.params["action"].orEmpty()
                Box {
                    OutlinedButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = SystemEventCatalog.titleOf(currentAction).ifBlank { "选择系统事件" },
                            modifier = Modifier.weight(1f),
                        )
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        SystemEventCatalog.all.forEach { spec ->
                            DropdownMenuItem(
                                text = {
                                    ColumnLike(title = spec.titleZh, subtitle = spec.hint)
                                },
                                onClick = {
                                    menuOpen = false
                                    onUpdate(
                                        trigger.copy(params = trigger.params + ("action" to spec.action)),
                                    )
                                },
                            )
                        }
                    }
                }
            }
            TriggerTypes.APP_OPENED -> {
                var menuOpen by remember { mutableStateOf(false) }
                val apps = remember {
                    val pm = context.packageManager
                    pm.getInstalledApplications(PackageManager.GET_META_DATA)
                        .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
                        .sortedBy { it.loadLabel(pm).toString() }
                        .take(80)
                }
                OutlinedTextField(
                    value = trigger.params["packageName"].orEmpty(),
                    onValueChange = {
                        onUpdate(trigger.copy(params = trigger.params + ("packageName" to it)))
                    },
                    label = { Text("目标应用包名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = "选择应用")
                        }
                    },
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    apps.forEach { info ->
                        val label = info.loadLabel(context.packageManager).toString()
                        DropdownMenuItem(
                            text = { Text("$label\n${info.packageName}") },
                            onClick = {
                                menuOpen = false
                                onUpdate(
                                    trigger.copy(
                                        params = trigger.params + ("packageName" to info.packageName),
                                    ),
                                )
                            },
                        )
                    }
                }
                Text(
                    text = "需在权限中心开启「使用情况访问」",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TriggerTypes.HOST_OPENED -> {
                Text(
                    text = "打开应用工厂时触发（与「打开本应用时自动运行」开关联动）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> {
                Text("类型：${trigger.type}", style = MaterialTheme.typography.bodySmall)
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
            TextButton(onClick = onDelete) { Text("删除") }
        }
    }
}

@Composable
private fun OnceAtFields(trigger: Trigger, onUpdate: (Trigger) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ParamField(trigger, "year", "年", onUpdate, Modifier.weight(1.2f))
        ParamField(trigger, "month", "月", onUpdate, Modifier.weight(1f))
        ParamField(trigger, "day", "日", onUpdate, Modifier.weight(1f))
    }
    HourMinuteRow(trigger = trigger, onUpdate = onUpdate)
}

@Composable
private fun WindowRepeatFields(trigger: Trigger, onUpdate: (Trigger) -> Unit) {
    Text("开始时刻", style = MaterialTheme.typography.labelMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ParamField(trigger, "startHour", "时", onUpdate, Modifier.weight(1f))
        ParamField(trigger, "startMinute", "分", onUpdate, Modifier.weight(1f))
    }
    Text("结束时刻（可跨午夜）", style = MaterialTheme.typography.labelMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ParamField(trigger, "endHour", "时", onUpdate, Modifier.weight(1f))
        ParamField(trigger, "endMinute", "分", onUpdate, Modifier.weight(1f))
    }
    OutlinedTextField(
        value = trigger.params["intervalMinutes"].orEmpty(),
        onValueChange = {
            onUpdate(trigger.copy(params = trigger.params + ("intervalMinutes" to it)))
        },
        label = { Text("时段内间隔（分钟）") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}

@Composable
private fun HourMinuteRow(trigger: Trigger, onUpdate: (Trigger) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ParamField(trigger, "hour", "时 (0-23)", onUpdate, Modifier.weight(1f))
        ParamField(trigger, "minute", "分 (0-59)", onUpdate, Modifier.weight(1f))
    }
}

@Composable
private fun ParamField(
    trigger: Trigger,
    key: String,
    label: String,
    onUpdate: (Trigger) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = trigger.params[key].orEmpty(),
        onValueChange = { onUpdate(trigger.copy(params = trigger.params + (key to it))) },
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DaysOfWeekPicker(trigger: Trigger, onUpdate: (Trigger) -> Unit) {
    val selected = remember(trigger.params["daysOfWeek"]) {
        ScheduleTimeMath.parseDaysOfWeek(trigger.params["daysOfWeek"])
    }
    val labels = listOf(
        1 to "日",
        2 to "一",
        3 to "二",
        4 to "三",
        5 to "四",
        6 to "五",
        7 to "六",
    )
    Text("重复星期", style = MaterialTheme.typography.labelMedium)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEach { (day, label) ->
            val on = day in selected
            FilterChip(
                selected = on,
                onClick = {
                    val next = selected.toMutableSet()
                    if (on) next.remove(day) else next.add(day)
                    val value = if (next.isEmpty() || next.size == 7) {
                        ""
                    } else {
                        next.sorted().joinToString(",")
                    }
                    onUpdate(trigger.copy(params = trigger.params + ("daysOfWeek" to value)))
                },
                label = { Text(label) },
            )
        }
    }
    Text(
        text = "未选或全选 = 每天；每周重复请至少选一天",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ExcludeDatesField(trigger: Trigger, onUpdate: (Trigger) -> Unit) {
    OutlinedTextField(
        value = trigger.params["excludeDates"].orEmpty(),
        onValueChange = {
            onUpdate(trigger.copy(params = trigger.params + ("excludeDates" to it)))
        },
        label = { Text("例外日期（跳过）") },
        supportingText = {
            Text("格式 yyyy-MM-dd，多个用逗号分隔", style = MaterialTheme.typography.labelSmall)
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}

@Composable
private fun ScheduleHint() {
    val context = LocalContext.current
    val exactOk = remember {
        PermissionHelper.canScheduleExactAlarms(context)
    }
    Text(
        text = if (exactOk) {
            "使用系统闹钟调度"
        } else {
            "未授予「闹钟和提醒」时可能不准时，请到权限中心授权"
        },
        style = MaterialTheme.typography.labelSmall,
        color = if (exactOk) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.error
        },
    )
}

@Composable
private fun ScheduleStatusBlock(trigger: Trigger) {
    val preview = remember(trigger) { ScheduleValidation.nextFirePreview(trigger) }
    val error = remember(trigger) { ScheduleValidation.validate(trigger) }
    if (error != null) {
        Text(
            text = error,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
    Text(
        text = preview,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun ColumnLike(title: String, subtitle: String) {
    Column {
        Text(title)
        Text(subtitle, style = MaterialTheme.typography.labelSmall)
    }
}

fun triggerLabel(type: String): String = triggerTitleZh(type)

fun defaultParamsForTrigger(type: String): Map<String, String> {
    val cal = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
    return when (type) {
        TriggerTypes.SCHEDULE -> mapOf("intervalMinutes" to "15")
        TriggerTypes.ONCE_AT -> mapOf(
            "year" to cal.get(Calendar.YEAR).toString(),
            "month" to (cal.get(Calendar.MONTH) + 1).toString(),
            "day" to cal.get(Calendar.DAY_OF_MONTH).toString(),
            "hour" to cal.get(Calendar.HOUR_OF_DAY).toString(),
            "minute" to cal.get(Calendar.MINUTE).toString(),
            "excludeDates" to "",
        )
        TriggerTypes.DAILY_AT -> mapOf(
            "hour" to "9",
            "minute" to "0",
            "daysOfWeek" to "",
            "excludeDates" to "",
        )
        TriggerTypes.WEEKLY -> mapOf(
            "hour" to "9",
            "minute" to "0",
            "daysOfWeek" to "2,3,4,5,6",
            "excludeDates" to "",
        )
        TriggerTypes.WINDOW_REPEAT -> mapOf(
            "startHour" to "9",
            "startMinute" to "0",
            "endHour" to "18",
            "endMinute" to "0",
            "intervalMinutes" to "30",
            "daysOfWeek" to "",
            "excludeDates" to "",
        )
        TriggerTypes.EVENT -> mapOf("action" to android.content.Intent.ACTION_POWER_CONNECTED)
        TriggerTypes.APP_OPENED -> mapOf("packageName" to "")
        else -> emptyMap()
    }
}

/** 编辑器「添加触发器」可选类型 */
fun editableTriggerTypes(): List<String> = listOf(
    TriggerTypes.SCHEDULE,
    TriggerTypes.ONCE_AT,
    TriggerTypes.DAILY_AT,
    TriggerTypes.WEEKLY,
    TriggerTypes.WINDOW_REPEAT,
    TriggerTypes.EVENT,
    TriggerTypes.APP_OPENED,
    TriggerTypes.HOST_OPENED,
)
