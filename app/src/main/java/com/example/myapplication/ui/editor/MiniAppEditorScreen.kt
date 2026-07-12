// Author: PKQ32
// Created: 2026-07-11
// Description: 小应用编辑器：赛博分区、图标选择、Lazy 列表提升滑动流畅度

package com.example.myapplication.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.example.myapplication.di.ServiceLocator
import com.example.myapplication.domain.FailurePolicy
import com.example.myapplication.domain.model.ActionStep
import com.example.myapplication.domain.model.MiniApp
import com.example.myapplication.domain.model.Trigger
import com.example.myapplication.domain.model.TriggerTypes
import com.example.myapplication.platform.ScheduleValidation
import com.example.myapplication.ui.icons.AppIconPicker
import com.example.myapplication.ui.theme.CyberCyan
import com.example.myapplication.ui.theme.CyberPanel
import com.example.myapplication.ui.theme.CyberSectionTitle
import com.example.myapplication.ui.theme.CyberTopBar
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniAppEditorScreen(
    miniAppId: String,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit = {},
) {
    var app by remember { mutableStateOf<MiniApp?>(null) }
    var loadError by remember { mutableStateOf(false) }
    var addStepMenuOpen by remember { mutableStateOf(false) }
    var addTriggerMenuOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val actionTypes = remember { ServiceLocator.registry.allTypes().toList().sorted() }

    LaunchedEffect(miniAppId) {
        val loaded = ServiceLocator.miniApps.get(miniAppId)
        if (loaded == null) {
            loadError = true
        } else {
            app = loaded
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CyberTopBar(
                title = "编辑小应用",
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
                    val current = app
                    OutlinedButton(
                        onClick = onOpenHistory,
                        enabled = current != null,
                        modifier = Modifier.padding(end = 4.dp),
                    ) {
                        Text("历史")
                    }
                    Button(
                        onClick = {
                            if (current == null) return@Button
                            val calendarErrors = current.triggers
                                .filter { it.enabled && TriggerTypes.isCalendarType(it.type) }
                                .mapNotNull { t -> ScheduleValidation.validate(t)?.let { "${triggerTitleZh(t.type)}：$it" } }
                            if (calendarErrors.isNotEmpty()) {
                                Toast.makeText(
                                    context,
                                    "请先修正日程：\n${calendarErrors.joinToString("\n")}",
                                    Toast.LENGTH_LONG,
                                ).show()
                                return@Button
                            }
                            scope.launch {
                                val ordered = current.steps
                                    .mapIndexed { i, step -> step.copy(order = i) }
                                if (current.runOnHostOpen) {
                                    Toast.makeText(
                                        context,
                                        "已开启「打开本应用时自动运行」（其它小应用的同名开关将关闭）",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                                ServiceLocator.miniApps.save(
                                    current.copy(
                                        steps = ordered,
                                        updatedAt = System.currentTimeMillis(),
                                    ),
                                )
                                ServiceLocator.triggerManager.resync()
                                onBack()
                            }
                        },
                        enabled = current != null,
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text("保存")
                    }
                },
            )
        },
    ) { padding ->
        when {
            loadError -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("未找到该小应用")
                    Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                        Text("返回")
                    }
                }
            }
            app == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
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
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item(key = "section-basic") {
                        CyberSectionTitle("基本信息")
                    }
                    item(key = "panel-basic") {
                        CyberPanel {
                            OutlinedTextField(
                                value = current.name,
                                onValueChange = { app = current.copy(name = it) },
                                label = { Text("名称") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                            Text("图标", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "选择后将显示在列表与导出应用界面",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            AppIconPicker(
                                selectedKey = current.iconKey,
                                onSelect = { app = current.copy(iconKey = it) },
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("启用")
                                Switch(
                                    checked = current.enabled,
                                    onCheckedChange = { app = current.copy(enabled = it) },
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text("打开本应用时自动运行")
                                    Text(
                                        text = "同时只能有一个小应用开启；保存时会自动互斥",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(
                                    checked = current.runOnHostOpen,
                                    onCheckedChange = { app = current.copy(runOnHostOpen = it) },
                                )
                            }
                        }
                    }

                    item(key = "section-steps") {
                        CyberSectionTitle("动作步骤")
                    }
                    if (current.steps.isEmpty()) {
                        item(key = "steps-empty") {
                            CyberPanel {
                                Text(
                                    text = "尚未添加动作",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = "点击下方按钮选择震动、音量、通知等模块",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    itemsIndexed(
                        current.steps,
                        key = { _, step -> step.id },
                    ) { index, step ->
                        ActionForm(
                            step = step,
                            index = index,
                            total = current.steps.size,
                            onUpdate = { updated ->
                                val list = current.steps.toMutableList()
                                list[index] = updated
                                app = current.copy(steps = list)
                            },
                            onMoveUp = {
                                if (index <= 0) return@ActionForm
                                val list = current.steps.toMutableList()
                                val tmp = list[index - 1]
                                list[index - 1] = list[index]
                                list[index] = tmp
                                app = current.copy(steps = list)
                            },
                            onMoveDown = {
                                if (index >= current.steps.lastIndex) return@ActionForm
                                val list = current.steps.toMutableList()
                                val tmp = list[index + 1]
                                list[index + 1] = list[index]
                                list[index] = tmp
                                app = current.copy(steps = list)
                            },
                            onDelete = {
                                app = current.copy(
                                    steps = current.steps.filterIndexed { i, _ -> i != index },
                                )
                            },
                        )
                    }
                    item(key = "add-step") {
                        Box {
                            OutlinedButton(onClick = { addStepMenuOpen = true }) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = CyberCyan)
                                Text("添加动作", modifier = Modifier.padding(start = 6.dp))
                            }
                            DropdownMenu(
                                expanded = addStepMenuOpen,
                                onDismissRequest = { addStepMenuOpen = false },
                            ) {
                                actionTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = {
                                            LabeledTypeRow(
                                                titleZh = actionTitleZh(type),
                                                typeKey = type,
                                                icon = actionIcon(type),
                                            )
                                        },
                                        onClick = {
                                            addStepMenuOpen = false
                                            val step = ActionStep(
                                                id = UUID.randomUUID().toString(),
                                                type = type,
                                                params = defaultParamsForType(type),
                                                order = current.steps.size,
                                                onFailure = FailurePolicy.ABORT,
                                            )
                                            app = current.copy(steps = current.steps + step)
                                        },
                                    )
                                }
                            }
                        }
                    }

                    item(key = "section-triggers") {
                        CyberSectionTitle("触发器")
                    }
                    if (current.triggers.isEmpty()) {
                        item(key = "triggers-empty") {
                            CyberPanel {
                                Text("可选触发方式", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "支持周期、一次/每日/每周/时段、充电解锁等事件、打开指定应用",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    itemsIndexed(
                        current.triggers,
                        key = { _, trigger -> trigger.id },
                    ) { index, trigger ->
                        TriggerForm(
                            trigger = trigger,
                            index = index,
                            onUpdate = { updated ->
                                val list = current.triggers.toMutableList()
                                list[index] = updated
                                app = current.copy(triggers = list)
                            },
                            onDelete = {
                                app = current.copy(
                                    triggers = current.triggers.filterIndexed { i, _ -> i != index },
                                )
                            },
                        )
                    }
                    item(key = "add-trigger") {
                        Box {
                            OutlinedButton(onClick = { addTriggerMenuOpen = true }) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = CyberCyan)
                                Text("添加触发器", modifier = Modifier.padding(start = 6.dp))
                            }
                            DropdownMenu(
                                expanded = addTriggerMenuOpen,
                                onDismissRequest = { addTriggerMenuOpen = false },
                            ) {
                                editableTriggerTypes().forEach { type ->
                                    DropdownMenuItem(
                                        text = {
                                            LabeledTypeRow(
                                                titleZh = triggerTitleZh(type),
                                                typeKey = type,
                                                icon = triggerIcon(type),
                                            )
                                        },
                                        onClick = {
                                            addTriggerMenuOpen = false
                                            val trigger = Trigger(
                                                id = UUID.randomUUID().toString(),
                                                type = type,
                                                params = defaultParamsForTrigger(type),
                                                enabled = true,
                                            )
                                            app = current.copy(
                                                triggers = current.triggers + trigger,
                                                runOnHostOpen = if (type == TriggerTypes.HOST_OPENED) {
                                                    true
                                                } else {
                                                    current.runOnHostOpen
                                                },
                                            )
                                        },
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
