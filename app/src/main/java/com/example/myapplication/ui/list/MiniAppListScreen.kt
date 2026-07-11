// Author: PKQ32
// Created: 2026-07-11
// Description: 小应用列表页：启用开关、运行、进入编辑，FAB 创建新应用

package com.example.myapplication.ui.list

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.di.ServiceLocator
import com.example.myapplication.domain.model.ActionStep
import com.example.myapplication.domain.model.ActionTypes
import com.example.myapplication.domain.model.MiniApp
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniAppListScreen(
    onOpenEditor: (String) -> Unit,
    onRun: (String) -> Unit,
    onOpenPermissions: () -> Unit = {},
) {
    val apps by ServiceLocator.miniApps.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("小应用列表") },
                actions = {
                    TextButton(onClick = onOpenPermissions) {
                        Text("权限")
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
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "暂无小应用",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "点击右下角 + 创建",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(apps, key = { it.id }) { app ->
                    MiniAppListItem(
                        app = app,
                        onToggleEnabled = { enabled ->
                            scope.launch {
                                ServiceLocator.miniApps.save(
                                    app.copy(enabled = enabled, updatedAt = System.currentTimeMillis()),
                                )
                            }
                        },
                        onRun = { onRun(app.id) },
                        onOpenEditor = { onOpenEditor(app.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniAppListItem(
    app: MiniApp,
    onToggleEnabled: (Boolean) -> Unit,
    onRun: () -> Unit,
    onOpenEditor: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenEditor)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.name,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (app.enabled) "已启用" else "已禁用",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(
            checked = app.enabled,
            onCheckedChange = onToggleEnabled,
        )
        Button(onClick = onRun) {
            Text("运行")
        }
    }
}
