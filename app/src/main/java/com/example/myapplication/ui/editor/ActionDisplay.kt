// Author: PKQ32
// Created: 2026-07-11
// Description: 动作/触发器中文标签、英文引文与图标映射

package com.example.myapplication.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.myapplication.domain.model.ActionTypes
import com.example.myapplication.domain.model.TriggerTypes

/**
 * 动作类型中文名；未知类型回退为原 type。
 */
fun actionTitleZh(type: String): String = when (type) {
    ActionTypes.VIBRATE -> "震动"
    ActionTypes.DELAY -> "延时等待"
    ActionTypes.PLAY_MUSIC -> "播放音乐"
    ActionTypes.PLAY_VIDEO -> "播放视频"
    ActionTypes.SET_VOLUME -> "调节音量"
    ActionTypes.FLASHLIGHT -> "手电筒"
    ActionTypes.SET_BRIGHTNESS -> "调节亮度"
    ActionTypes.OPEN_APP -> "打开应用"
    ActionTypes.NOTIFY -> "发送通知"
    ActionTypes.WIFI -> "Wi‑Fi"
    ActionTypes.BLUETOOTH -> "蓝牙"
    ActionTypes.SET_RINGER_MODE -> "响铃模式"
    ActionTypes.SCREENSHOT -> "截图"
    else -> type
}

/**
 * 触发器类型中文名。
 */
fun triggerTitleZh(type: String): String = when (type) {
    TriggerTypes.SCHEDULE -> "定时"
    TriggerTypes.EVENT -> "系统事件"
    TriggerTypes.APP_OPENED -> "打开第三方应用"
    TriggerTypes.MANUAL -> "手动"
    TriggerTypes.HOST_OPENED -> "打开本应用"
    else -> type
}

/**
 * 动作图标。
 */
fun actionIcon(type: String): ImageVector = when (type) {
    ActionTypes.VIBRATE -> Icons.Filled.Vibration
    ActionTypes.DELAY -> Icons.Filled.Timer
    ActionTypes.PLAY_MUSIC -> Icons.Filled.MusicNote
    ActionTypes.PLAY_VIDEO -> Icons.Filled.Movie
    ActionTypes.SET_VOLUME -> Icons.Filled.VolumeUp
    ActionTypes.FLASHLIGHT -> Icons.Filled.FlashlightOn
    ActionTypes.SET_BRIGHTNESS -> Icons.Filled.Brightness6
    ActionTypes.OPEN_APP -> Icons.Filled.OpenInNew
    ActionTypes.NOTIFY -> Icons.Filled.Notifications
    ActionTypes.WIFI -> Icons.Filled.Wifi
    ActionTypes.BLUETOOTH -> Icons.Filled.Bluetooth
    ActionTypes.SET_RINGER_MODE -> Icons.Filled.PhoneInTalk
    ActionTypes.SCREENSHOT -> Icons.Filled.Screenshot
    else -> Icons.Filled.Extension
}

/**
 * 触发器图标。
 */
fun triggerIcon(type: String): ImageVector = when (type) {
    TriggerTypes.SCHEDULE -> Icons.Filled.Schedule
    TriggerTypes.EVENT -> Icons.Filled.Power
    TriggerTypes.APP_OPENED -> Icons.Filled.Apps
    TriggerTypes.HOST_OPENED -> Icons.Filled.OpenInNew
    else -> Icons.Filled.Extension
}

/**
 * 中文主文案 + 英文引文（等宽灰色小字）。
 */
@Composable
fun TypeLabelColumn(
    titleZh: String,
    typeKey: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = titleZh,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = typeKey,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 图标 + 中英文标签，用于下拉项与步骤标题。
 */
@Composable
fun LabeledTypeRow(
    titleZh: String,
    typeKey: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = titleZh,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        TypeLabelColumn(titleZh = titleZh, typeKey = typeKey)
    }
}
