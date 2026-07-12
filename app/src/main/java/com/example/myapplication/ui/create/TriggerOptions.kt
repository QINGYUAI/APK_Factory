// Author: PKQ32
// Created: 2026-07-12
// Description: 触发方式卡片目录（创建向导 / 编辑器共用）

package com.example.myapplication.ui.create

import com.example.myapplication.domain.model.Trigger
import com.example.myapplication.domain.model.TriggerTypes
import com.example.myapplication.platform.SystemEventCatalog
import com.example.myapplication.ui.editor.defaultParamsForTrigger
import java.util.UUID

data class TriggerOption(
    val key: String,
    val titleZh: String,
    val subtitle: String,
    val permissionHint: String?,
    /** null = 仅手动，不写入 triggers；host = runOnHostOpen */
    val kind: Kind,
) {
    enum class Kind {
        Manual,
        HostOpen,
        TriggerType,
    }
}

object TriggerOptions {

    val wizardChoices: List<TriggerOption> = listOf(
        TriggerOption(
            key = "manual",
            titleZh = "仅手动运行",
            subtitle = "在工厂或导出应用里点运行",
            permissionHint = null,
            kind = TriggerOption.Kind.Manual,
        ),
        TriggerOption(
            key = TriggerTypes.SCHEDULE,
            titleZh = "周期定时",
            subtitle = "每隔 N 分钟执行（系统最短 15 分钟）",
            permissionHint = "部分手机需关闭电池优化",
            kind = TriggerOption.Kind.TriggerType,
        ),
        TriggerOption(
            key = TriggerTypes.ONCE_AT,
            titleZh = "一次执行",
            subtitle = "指定日期时刻执行一次后自动关闭",
            permissionHint = "Android 12+ 可能需「闹钟和提醒」权限",
            kind = TriggerOption.Kind.TriggerType,
        ),
        TriggerOption(
            key = TriggerTypes.DAILY_AT,
            titleZh = "每日定点",
            subtitle = "每天（或指定星期）固定时刻执行",
            permissionHint = "Android 12+ 可能需「闹钟和提醒」权限",
            kind = TriggerOption.Kind.TriggerType,
        ),
        TriggerOption(
            key = TriggerTypes.WEEKLY,
            titleZh = "每周重复",
            subtitle = "按星期几在固定时刻执行",
            permissionHint = "Android 12+ 可能需「闹钟和提醒」权限",
            kind = TriggerOption.Kind.TriggerType,
        ),
        TriggerOption(
            key = TriggerTypes.WINDOW_REPEAT,
            titleZh = "时段循环",
            subtitle = "在起止时段内按间隔反复执行（可跨午夜）",
            permissionHint = "Android 12+ 可能需「闹钟和提醒」权限",
            kind = TriggerOption.Kind.TriggerType,
        ),
        TriggerOption(
            key = "event_power",
            titleZh = "插入充电器",
            subtitle = "开始充电时自动执行",
            permissionHint = null,
            kind = TriggerOption.Kind.TriggerType,
        ),
        TriggerOption(
            key = "event_unplug",
            titleZh = "拔出充电器",
            subtitle = "断开电源时自动执行（新）",
            permissionHint = null,
            kind = TriggerOption.Kind.TriggerType,
        ),
        TriggerOption(
            key = "event_unlock",
            titleZh = "解锁屏幕",
            subtitle = "解锁后自动执行（新）",
            permissionHint = null,
            kind = TriggerOption.Kind.TriggerType,
        ),
        TriggerOption(
            key = "event_screen_on",
            titleZh = "屏幕点亮",
            subtitle = "亮屏时触发（新，需保持工厂进程）",
            permissionHint = null,
            kind = TriggerOption.Kind.TriggerType,
        ),
        TriggerOption(
            key = "event_boot",
            titleZh = "开机完成",
            subtitle = "开机后自动执行（新）",
            permissionHint = "需授予开机启动相关权限",
            kind = TriggerOption.Kind.TriggerType,
        ),
        TriggerOption(
            key = TriggerTypes.APP_OPENED,
            titleZh = "打开指定应用",
            subtitle = "监测到目标 App 切换到前台时执行",
            permissionHint = "需「使用情况访问」权限",
            kind = TriggerOption.Kind.TriggerType,
        ),
        TriggerOption(
            key = TriggerTypes.HOST_OPENED,
            titleZh = "打开应用工厂",
            subtitle = "打开本工厂 App 时自动执行",
            permissionHint = null,
            kind = TriggerOption.Kind.HostOpen,
        ),
    )

    fun buildTrigger(optionKey: String): Trigger? {
        return when (optionKey) {
            "manual", TriggerTypes.HOST_OPENED -> null
            "event_power" -> eventTrigger(SystemEventCatalog.all[0].action)
            "event_unplug" -> eventTrigger(
                SystemEventCatalog.all.first { it.titleZh == "拔出充电器" }.action,
            )
            "event_unlock" -> eventTrigger(
                SystemEventCatalog.all.first { it.titleZh == "解锁屏幕" }.action,
            )
            "event_screen_on" -> eventTrigger(
                SystemEventCatalog.all.first { it.titleZh == "屏幕点亮" }.action,
            )
            "event_boot" -> eventTrigger(
                SystemEventCatalog.all.first { it.titleZh == "开机完成" }.action,
            )
            TriggerTypes.SCHEDULE,
            TriggerTypes.ONCE_AT,
            TriggerTypes.DAILY_AT,
            TriggerTypes.WEEKLY,
            TriggerTypes.WINDOW_REPEAT,
            TriggerTypes.APP_OPENED,
            TriggerTypes.EVENT,
            -> Trigger(
                id = UUID.randomUUID().toString(),
                type = optionKey,
                params = defaultParamsForTrigger(optionKey),
                enabled = true,
            )
            else -> null
        }
    }

    private fun eventTrigger(action: String) = Trigger(
        id = UUID.randomUUID().toString(),
        type = TriggerTypes.EVENT,
        params = mapOf("action" to action),
        enabled = true,
    )
}
