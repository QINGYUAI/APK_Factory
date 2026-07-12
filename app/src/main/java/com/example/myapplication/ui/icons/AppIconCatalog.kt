// Author: PKQ32
// Created: 2026-07-12
// Description: 小应用预设图标目录（工厂选择 / Runner 展示共用）

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
import androidx.compose.ui.graphics.vector.ImageVector

data class AppIconSpec(
    val key: String,
    val title: String,
    val icon: ImageVector,
)

object AppIconCatalog {
    val all: List<AppIconSpec> = listOf(
        AppIconSpec("default", "默认", Icons.Filled.Apps),
        AppIconSpec("bolt", "闪电", Icons.Filled.Bolt),
        AppIconSpec("rocket", "火箭", Icons.Filled.RocketLaunch),
        AppIconSpec("star", "星星", Icons.Filled.Star),
        AppIconSpec("sun", "日光", Icons.Filled.WbSunny),
        AppIconSpec("music", "音乐", Icons.Filled.MusicNote),
        AppIconSpec("notify", "通知", Icons.Filled.Notifications),
        AppIconSpec("home", "家居", Icons.Filled.Home),
        AppIconSpec("heart", "喜欢", Icons.Filled.Favorite),
        AppIconSpec("clock", "定时", Icons.Filled.Schedule),
        AppIconSpec("lab", "实验", Icons.Filled.Science),
    )

    fun specOf(key: String): AppIconSpec =
        all.firstOrNull { it.key == key } ?: all.first()

    fun iconOf(key: String): ImageVector = specOf(key).icon
}
