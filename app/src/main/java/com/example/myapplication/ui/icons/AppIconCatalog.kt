// Author: PKQ32
// Created: 2026-07-12
// Description: 小应用预设图标目录（工厂选择 / Runner 展示共用，与导出桌面图标对应）

package com.example.myapplication.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class AppIconSpec(
    val key: String,
    val title: String,
    val icon: ImageVector,
    /** 与导出桌面图标背景色一致，便于预览 */
    val tintBg: Color,
)

object AppIconCatalog {
    val all: List<AppIconSpec> = listOf(
        AppIconSpec("default", "默认", Icons.Filled.Apps, Color(0xFF0EA5E9)),
        AppIconSpec("bolt", "闪电", Icons.Filled.Bolt, Color(0xFFF59E0B)),
        AppIconSpec("rocket", "火箭", Icons.Filled.RocketLaunch, Color(0xFF6366F1)),
        AppIconSpec("star", "星星", Icons.Filled.Star, Color(0xFFEAB308)),
        AppIconSpec("sun", "日光", Icons.Filled.WbSunny, Color(0xFFF97316)),
        AppIconSpec("music", "音乐", Icons.Filled.MusicNote, Color(0xFF06B6D4)),
        AppIconSpec("notify", "通知", Icons.Filled.Notifications, Color(0xFF8B5CF6)),
        AppIconSpec("home", "家居", Icons.Filled.Home, Color(0xFF10B981)),
        AppIconSpec("heart", "喜欢", Icons.Filled.Favorite, Color(0xFFEF4444)),
        AppIconSpec("clock", "定时", Icons.Filled.Schedule, Color(0xFF3B82F6)),
        AppIconSpec("lab", "实验", Icons.Filled.Science, Color(0xFF14B8A6)),
    )

    fun specOf(key: String): AppIconSpec =
        all.firstOrNull { it.key == key } ?: all.first()

    fun iconOf(key: String): ImageVector = specOf(key).icon
}
