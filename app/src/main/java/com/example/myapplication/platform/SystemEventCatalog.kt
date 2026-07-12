// Author: PKQ32
// Created: 2026-07-12
// Description: 系统事件触发器目录（常用子集 + 动态注册标记）

package com.example.myapplication.platform

import android.content.Intent
import android.os.Build

data class SystemEventSpec(
    val action: String,
    val titleZh: String,
    val hint: String,
    /** true：必须在运行时动态注册（无法写进 Manifest） */
    val dynamicOnly: Boolean = false,
)

object SystemEventCatalog {

    val all: List<SystemEventSpec> = listOf(
        SystemEventSpec(
            action = Intent.ACTION_POWER_CONNECTED,
            titleZh = "插入充电器",
            hint = "开始充电时触发",
        ),
        SystemEventSpec(
            action = Intent.ACTION_POWER_DISCONNECTED,
            titleZh = "拔出充电器",
            hint = "断开电源时触发",
        ),
        SystemEventSpec(
            action = Intent.ACTION_BOOT_COMPLETED,
            titleZh = "开机完成",
            hint = "手机启动完成后触发（需开机权限）",
        ),
        SystemEventSpec(
            action = Intent.ACTION_USER_PRESENT,
            titleZh = "解锁屏幕",
            hint = "用户解锁后触发",
        ),
        SystemEventSpec(
            action = Intent.ACTION_HEADSET_PLUG,
            titleZh = "插入耳机",
            hint = "有线耳机插入时触发",
        ),
        SystemEventSpec(
            action = Intent.ACTION_SCREEN_ON,
            titleZh = "屏幕点亮",
            hint = "亮屏时触发（应用需保持运行）",
            dynamicOnly = true,
        ),
        SystemEventSpec(
            action = Intent.ACTION_SCREEN_OFF,
            titleZh = "屏幕熄灭",
            hint = "灭屏时触发（应用需保持运行）",
            dynamicOnly = true,
        ),
        SystemEventSpec(
            action = Intent.ACTION_AIRPLANE_MODE_CHANGED,
            titleZh = "飞行模式变化",
            hint = "开关飞行模式时触发",
        ),
        SystemEventSpec(
            action = if (Build.VERSION.SDK_INT >= 33) {
                "android.intent.action.BATTERY_LOW"
            } else {
                Intent.ACTION_BATTERY_LOW
            },
            titleZh = "电量过低",
            hint = "系统提示低电量时触发",
        ),
        SystemEventSpec(
            action = Intent.ACTION_BATTERY_OKAY,
            titleZh = "电量恢复",
            hint = "电量从低恢复正常时触发",
        ),
    )

    fun of(action: String): SystemEventSpec? = all.firstOrNull { it.action == action }

    fun titleOf(action: String): String = of(action)?.titleZh ?: action.substringAfterLast('.')
}
