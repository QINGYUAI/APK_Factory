// Author: PKQ32
// Created: 2026-11-16
// Description: 全局更新提示：新版本说明、下载进度与安装入口

package com.example.myapplication.ui.help

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.di.ServiceLocator
import com.example.myapplication.ui.theme.CyberError

/**
 * 挂在根导航上，任何页面都能弹出更新提示；同时消费一次性提示消息。
 */
@Composable
fun UpdatePrompt() {
    val context = LocalContext.current
    val manager = ServiceLocator.updateManager
    val state by manager.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        manager.consumeMessage()
    }

    val info = state.info
    if (!state.showDialog || info == null) return

    AlertDialog(
        onDismissRequest = {
            if (!state.downloading) manager.dismissDialog()
        },
        title = {
            Text("发现新版本 v${info.version}")
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (state.downloading) {
                    LinearProgressIndicator(
                        progress = { state.progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "正在下载更新包 ${state.progress}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = info.notes.ifBlank { "该版本未提供更新说明" },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "更新包会先校验包名、版本号与签名，再由系统安装器确认安装。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                state.error?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberError,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = manager::downloadAndInstall,
                enabled = !state.downloading,
            ) {
                Text(
                    if (state.downloading) {
                        "下载中 ${state.progress}%"
                    } else {
                        "立即更新"
                    },
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = manager::dismissDialog,
                enabled = !state.downloading,
            ) {
                Text("稍后")
            }
        },
    )
}
