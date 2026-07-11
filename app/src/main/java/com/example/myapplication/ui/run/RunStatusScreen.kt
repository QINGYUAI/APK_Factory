// Author: PKQ32
// Created: 2026-07-11
// Description: 运行状态页：手动触发执行并展示逐步结果

package com.example.myapplication.ui.run

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myapplication.di.ServiceLocator
import com.example.myapplication.domain.ActionResult
import com.example.myapplication.engine.RunReport
import com.example.myapplication.engine.StepOutcome
import com.example.myapplication.platform.PermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunStatusScreen(
    miniAppId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var report by remember { mutableStateOf<RunReport?>(null) }
    var running by remember { mutableStateOf(true) }
    var skipped by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* 结果由 PermissionHelper 状态反映；执行不依赖本次回调 */ }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("运行状态") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = onBack) {
                        Text("返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            when {
                running -> {
                    Text(
                        text = "正在执行…",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
                errorMessage != null -> {
                    Text(
                        text = "执行异常：$errorMessage",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
                skipped -> {
                    Text(
                        text = "已跳过（防抖：短时间内重复触发）",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
                report != null -> {
                    val r = report!!
                    Text(
                        text = if (r.success) "整体结果：成功" else "整体结果：失败",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (r.success) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(r.stepOutcomes, key = { index, o -> "${o.stepId}-$index" }) { index, outcome ->
                            StepOutcomeRow(index = index + 1, outcome = outcome)
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepOutcomeRow(
    index: Int,
    outcome: StepOutcome,
) {
    val (okLabel, message) = when (val result = outcome.result) {
        is ActionResult.Success -> "成功" to "ok"
        is ActionResult.Failed -> "失败" to "${result.code}: ${result.message}"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "$index. ${outcome.type} — $okLabel",
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = if (outcome.result is ActionResult.Failed) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
