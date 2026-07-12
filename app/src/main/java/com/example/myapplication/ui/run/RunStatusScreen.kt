// Author: PKQ32
// Created: 2026-07-11
// Description: 运行状态页：运行中可停止，完成后展示逐步结果

package com.example.myapplication.ui.run

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myapplication.di.ServiceLocator
import com.example.myapplication.domain.ActionResult
import com.example.myapplication.engine.RunReport
import com.example.myapplication.engine.StepOutcome
import com.example.myapplication.platform.PermissionHelper
import com.example.myapplication.ui.editor.actionIcon
import com.example.myapplication.ui.editor.actionTitleZh
import com.example.myapplication.ui.theme.CyberCyan
import com.example.myapplication.ui.theme.CyberPanel
import com.example.myapplication.ui.theme.CyberStatusChip
import com.example.myapplication.ui.theme.CyberSuccess
import com.example.myapplication.ui.theme.CyberTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunStatusScreen(
    miniAppId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activeRunningId by ServiceLocator.runner.runningId.collectAsState()
    var report by remember { mutableStateOf<RunReport?>(null) }
    var running by remember { mutableStateOf(true) }
    var skipped by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !PermissionHelper.hasNotificationsPermission(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(miniAppId) {
        running = true
        skipped = false
        errorMessage = null
        report = null
        try {
            val result = ServiceLocator.runner.run(miniAppId, "manual")
            if (result == null) {
                skipped = true
            } else {
                report = result
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "执行失败"
        } finally {
            running = false
        }
    }

    val isThisRunning = running || activeRunningId == miniAppId

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CyberTopBar(
                title = "运行监控",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = CyberCyan,
                        )
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
                when {
                    isThisRunning && report == null && !skipped && errorMessage == null -> {
                        CyberPanel {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("运行中", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "正在串行执行动作链，可随时停止",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                CyberStatusChip(text = "运行中", active = true)
                            }
                            Button(
                                onClick = { ServiceLocator.runner.stop() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("停止")
                            }
                        }
                    }
                    errorMessage != null -> {
                        CyberPanel {
                            Text("执行异常", style = MaterialTheme.typography.titleMedium)
                            Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    skipped -> {
                        CyberPanel {
                            Text("已跳过本次执行", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "已有任务在运行中，或短时间内重复触发被防抖拦截",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (activeRunningId != null) {
                                OutlinedButton(
                                    onClick = { ServiceLocator.runner.stop() },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("停止当前运行")
                                }
                            }
                        }
                    }
                    report != null -> {
                        val r = report!!
                        CyberPanel {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("整体结果", style = MaterialTheme.typography.titleMedium)
                                CyberStatusChip(
                                    text = when {
                                        r.stopped -> "已停止"
                                        r.success -> "成功"
                                        else -> "失败"
                                    },
                                    active = r.success,
                                )
                            }
                            Text(
                                text = if (r.stopped) {
                                    "已手动停止 · 完成 ${r.stepOutcomes.size.coerceAtLeast(0)} 条记录"
                                } else {
                                    "共 ${r.stepOutcomes.size} 个步骤"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedButton(
                                onClick = onBack,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("返回")
                            }
                        }
                    }
                }
            }
            if (report != null) {
                itemsIndexed(
                    report!!.stepOutcomes,
                    key = { index, o -> "${o.stepId}-$index" },
                ) { index, outcome ->
                    StepOutcomeCard(index = index + 1, outcome = outcome)
                }
            }
        }
    }
}

@Composable
private fun StepOutcomeCard(
    index: Int,
    outcome: StepOutcome,
) {
    val context = LocalContext.current
    val stopped = outcome.result is ActionResult.Failed &&
        (outcome.result as ActionResult.Failed).code == "STOPPED"
    val ok = outcome.result is ActionResult.Success
    val detail = when {
        ok -> "执行完成"
        stopped -> "用户已停止"
        else -> failureDetailZh(outcome)
    }
    val hint = if (ok || stopped) null else failureActionHint(outcome)

    CyberPanel {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = actionIcon(outcome.type),
                contentDescription = null,
                tint = when {
                    ok -> CyberSuccess
                    stopped -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.error
                },
                modifier = Modifier.size(26.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "步骤 $index · ${
                        if (stopped) "停止" else actionTitleZh(outcome.type)
                    }",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = outcome.type,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            CyberStatusChip(
                text = when {
                    ok -> "成功"
                    stopped -> "已停止"
                    else -> "失败"
                },
                active = ok,
            )
        }
        Text(
            text = detail,
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                ok -> MaterialTheme.colorScheme.onSurfaceVariant
                stopped -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.error
            },
        )
        if (hint != null) {
            Button(
                onClick = { hint.onClick(context) },
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text(hint.buttonLabel)
            }
        }
    }
}
