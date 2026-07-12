// Author: PKQ32
// Created: 2026-07-11
// Description: 权限中心：赛博面板展示授权状态（全中文）

package com.example.myapplication.ui.permissions

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.myapplication.di.ServiceLocator
import com.example.myapplication.platform.PermissionHelper
import com.example.myapplication.ui.theme.CyberCyan
import com.example.myapplication.ui.theme.CyberHeroHeader
import com.example.myapplication.ui.theme.CyberPanel
import com.example.myapplication.ui.theme.CyberStatusChip
import com.example.myapplication.ui.theme.CyberTopBar
import kotlinx.coroutines.launch

private data class PermissionItem(
    val id: String,
    val title: String,
    val description: String,
    val granted: Boolean,
    val primaryActionLabel: String,
    val onPrimaryAction: () -> Unit,
    val secondaryActionLabel: String? = null,
    val onSecondaryAction: (() -> Unit)? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionCenterScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshKey++
                scope.launch {
                    ServiceLocator.triggerManager.resync()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        refreshKey++
    }

    val notificationsGranted = remember(refreshKey) {
        PermissionHelper.hasNotificationsPermission(context)
    }
    val usageGranted = remember(refreshKey) {
        PermissionHelper.hasUsageAccess(context)
    }
    val writeSettingsGranted = remember(refreshKey) {
        PermissionHelper.canWriteSettings(context)
    }
    val dndGranted = remember(refreshKey) {
        PermissionHelper.hasNotificationPolicyAccess(context)
    }

    val items = listOf(
        PermissionItem(
            id = "notifications",
            title = "通知权限",
            description = "用于「发送通知」动作。Android 13 及以上需手动授权。",
            granted = notificationsGranted,
            primaryActionLabel = when {
                notificationsGranted -> "打开通知设置"
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> "申请权限"
                else -> "去系统设置"
            },
            onPrimaryAction = {
                if (!notificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    PermissionHelper.openAppNotificationSettings(context)
                }
            },
            secondaryActionLabel = if (!notificationsGranted &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ) {
                "打开通知设置"
            } else {
                null
            },
            onSecondaryAction = if (!notificationsGranted &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ) {
                { PermissionHelper.openAppNotificationSettings(context) }
            } else {
                null
            },
        ),
        PermissionItem(
            id = "usage",
            title = "使用情况访问",
            description = "用于「打开第三方应用时自动触发」。请在系统设置中为本应用开启使用情况访问。",
            granted = usageGranted,
            primaryActionLabel = "去系统设置",
            onPrimaryAction = { PermissionHelper.openUsageAccessSettings(context) },
        ),
        PermissionItem(
            id = "write_settings",
            title = "修改系统设置",
            description = "用于「调节亮度」等需要写入系统设置的动作。",
            granted = writeSettingsGranted,
            primaryActionLabel = "去系统设置",
            onPrimaryAction = { PermissionHelper.openWriteSettings(context) },
        ),
        PermissionItem(
            id = "dnd",
            title = "勿扰与通知策略",
            description = "用于切换静音 / 震动 / 响铃模式。",
            granted = dndGranted,
            primaryActionLabel = "去系统设置",
            onPrimaryAction = { PermissionHelper.openNotificationPolicySettings(context) },
        ),
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CyberTopBar(
                title = "权限中心",
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
                CyberHeroHeader(
                    title = "能力授权",
                    subtitle = "未授权时对应动作会失败并记录原因，不会导致应用崩溃",
                )
            }
            items(items, key = { it.id }) { item ->
                CyberPanel {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        CyberStatusChip(
                            text = if (item.granted) "已授权" else "未授权",
                            active = item.granted,
                        )
                    }
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!item.granted) {
                            Button(onClick = item.onPrimaryAction) {
                                Text(item.primaryActionLabel)
                            }
                        } else {
                            OutlinedButton(onClick = item.onPrimaryAction) {
                                Text(item.primaryActionLabel)
                            }
                        }
                        if (item.secondaryActionLabel != null && item.onSecondaryAction != null) {
                            OutlinedButton(onClick = item.onSecondaryAction) {
                                Text(item.secondaryActionLabel)
                            }
                        }
                    }
                }
            }
        }
    }
}
