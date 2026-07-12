// Author: PKQ32
// Created: 2026-07-12
// Description: 日程触发参数校验与下次触发时间文案

package com.example.myapplication.platform

import com.example.myapplication.domain.model.Trigger
import com.example.myapplication.domain.model.TriggerTypes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ScheduleValidation {

    private val displayFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun validate(trigger: Trigger): String? {
        if (!TriggerTypes.isCalendarType(trigger.type)) return null
        val p = trigger.params
        return when (trigger.type) {
            TriggerTypes.ONCE_AT -> {
                val y = p["year"]?.toIntOrNull() ?: return "请填写有效年份"
                val mo = p["month"]?.toIntOrNull() ?: return "请填写有效月份"
                val d = p["day"]?.toIntOrNull() ?: return "请填写有效日期"
                if (mo !in 1..12) return "月份须为 1–12"
                if (d !in 1..31) return "日期须为 1–31"
                hourMinuteError(p["hour"], p["minute"])
                    ?: excludeDatesError(p["excludeDates"])
                    ?: if (y < 1970) "年份过小" else null
            }
            TriggerTypes.DAILY_AT, TriggerTypes.WEEKLY -> {
                hourMinuteError(p["hour"], p["minute"])
                    ?: daysOfWeekError(p["daysOfWeek"], requireAtLeastOne = trigger.type == TriggerTypes.WEEKLY)
                    ?: excludeDatesError(p["excludeDates"])
            }
            TriggerTypes.WINDOW_REPEAT -> {
                hourMinuteError(p["startHour"], p["startMinute"], "开始")
                    ?: hourMinuteError(p["endHour"], p["endMinute"], "结束")
                    ?: run {
                        val interval = p["intervalMinutes"]?.toIntOrNull()
                            ?: return@run "请填写时段内间隔分钟"
                        if (interval !in 1..720) return@run "间隔须为 1–720 分钟"
                        null
                    }
                    ?: daysOfWeekError(p["daysOfWeek"], requireAtLeastOne = false)
                    ?: excludeDatesError(p["excludeDates"])
            }
            else -> null
        }
    }

    fun nextFirePreview(trigger: Trigger): String {
        if (!trigger.enabled) return "已停用，不会调度"
        validate(trigger)?.let { return "配置有误：$it" }
        val next = ScheduleTimeMath.nextFireMillis(trigger)
            ?: return "无法计算下次触发（可能已过期或全被例外日期跳过）"
        val formatted = synchronized(displayFmt) { displayFmt.format(Date(next)) }
        return "下次预计：$formatted"
    }

    private fun hourMinuteError(
        hourRaw: String?,
        minuteRaw: String?,
        label: String = "",
    ): String? {
        val prefix = if (label.isBlank()) "" else "${label}"
        val h = hourRaw?.toIntOrNull() ?: return "请填写${prefix}小时 (0–23)"
        val m = minuteRaw?.toIntOrNull() ?: return "请填写${prefix}分钟 (0–59)"
        if (h !in 0..23) return "${prefix}小时须为 0–23"
        if (m !in 0..59) return "${prefix}分钟须为 0–59"
        return null
    }

    private fun daysOfWeekError(raw: String?, requireAtLeastOne: Boolean): String? {
        val parts = raw.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.isEmpty()) {
            return if (requireAtLeastOne) "每周重复请至少选择一天" else null
        }
        if (parts.any { it.toIntOrNull() !in 1..7 }) {
            return "星期取值须为 1–7（日=1 … 六=7）"
        }
        return null
    }

    private fun excludeDatesError(raw: String?): String? {
        val parts = raw.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val bad = parts.filter { !it.matches(Regex("""\d{4}-\d{2}-\d{2}""")) }
        return if (bad.isEmpty()) null else "例外日期格式错误：${bad.joinToString()}"
    }
}
