// Author: PKQ32
// Created: 2026-07-11
// Description: 小应用编辑器：基本信息、动作步骤与触发器配置并保存

package com.example.myapplication.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.example.myapplication.domain.FailurePolicy
import com.example.myapplication.domain.model.ActionStep
import com.example.myapplication.domain.model.MiniApp
import com.example.myapplication.domain.model.Trigger
import com.example.myapplication.domain.model.TriggerTypes
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniAppEditorScreen(
    miniAppId: String,
    onBack: () -> Unit,
) {
    var app by remember { mutableStateOf<MiniApp?>(null) }
    var loadError by remember { mutableStateOf(false) }
    var addStepMenuOpen by remember { mutableStateOf(false) }
    var addTriggerMenuOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
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
        topBar = {
            TopAppBar(
                title = { Text("编辑小应用") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    val current = app
                    Button(
                        onClick = {
                            if (current == null) return@Button
                            scope.launch {
                                val ordered = current.steps
                                    .mapIndexed { i, step -> step.copy(order = i) }
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = current.name,
                        onValueChange = { app = current.copy(name = it) },
                        label = { Text("名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("启用")
                        Switch(
                            checked = current.enabled,
                            onCheckedChange = { app = current.copy(enabled = it) },
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("打开本应用时自动运行")
                        Switch(
                            checked = current.runOnHostOpen,
                            onCheckedChange = { app = current.copy(runOnHostOpen = it) },
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("动作步骤", style = MaterialTheme.typography.titleMedium)
                    current.steps.forEachIndexed { index, step ->
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
                                app = current.copy(steps = current.steps.filterIndexed { i, _ -> i != index })
                            },
                        )
                        if (index < current.steps.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                    Box {
                        OutlinedButton(onClick = { addStepMenuOpen = true }) {
                            Text("添加动作")
                        }
                        DropdownMenu(
                            expanded = addStepMenuOpen,
                            onDismissRequest = { addStepMenuOpen = false },
                        ) {
                            actionTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
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

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("触发器", style = MaterialTheme.typography.titleMedium)
                    current.triggers.forEachIndexed { index, trigger ->
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
                        if (index < current.triggers.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                    Box {
                        OutlinedButton(onClick = { addTriggerMenuOpen = true }) {
                            Text("添加触发器")
                        }
                        DropdownMenu(
                            expanded = addTriggerMenuOpen,
                            onDismissRequest = { addTriggerMenuOpen = false },
                        ) {
                            listOf(
                                TriggerTypes.SCHEDULE,
                                TriggerTypes.EVENT,
                                TriggerTypes.APP_OPENED,
                            ).forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(triggerLabel(type)) },
                                    onClick = {
                                        addTriggerMenuOpen = false
                                        val trigger = Trigger(
                                            id = UUID.randomUUID().toString(),
                                            type = type,
                                            params = defaultParamsForTrigger(type),
                                            enabled = true,
                                        )
                                        app = current.copy(triggers = current.triggers + trigger)
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
