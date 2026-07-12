// Author: PKQ32
// Created: 2026-07-12
// Description: 日程时间计算：一次/每周/每日/时段循环与例外日期

package com.example.myapplication.platform

import com.example.myapplication.domain.model.Trigger
import com.example.myapplication.domain.model.TriggerTypes
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

object ScheduleTimeMath {

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun parseDaysOfWeek(raw: String?): Set<Int> {
        val parsed = raw.orEmpty()
            .split(',')
            .mapNotNull { it.trim().toIntOrNull()?.takeIf { d -> d in 1..7 } }
            .toSet()
        return parsed.ifEmpty { (1..7).toSet() }
    }

    fun parseExcludeDates(raw: String?): Set<String> =
        raw.orEmpty()
            .split(',')
            .map { it.trim() }
            .filter { it.matches(Regex("""\d{4}-\d{2}-\d{2}""")) }
            .toSet()

    fun dateKey(cal: Calendar): String = synchronized(dateFmt) { dateFmt.format(cal.time) }

    fun isExcluded(cal: Calendar, excludeDates: Set<String>): Boolean =
        dateKey(cal) in excludeDates

    fun nextFireMillis(trigger: Trigger, fromMillis: Long = System.currentTimeMillis()): Long? {
        return when (trigger.type) {
            TriggerTypes.ONCE_AT -> nextOnce(trigger, fromMillis)
            TriggerTypes.DAILY_AT, TriggerTypes.WEEKLY -> nextWeeklyLike(trigger, fromMillis)
            TriggerTypes.WINDOW_REPEAT -> nextWindowRepeat(trigger, fromMillis)
            else -> null
        }
    }

    fun shouldSkipFire(trigger: Trigger, atMillis: Long = System.currentTimeMillis()): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = atMillis }
        val excludes = parseExcludeDates(trigger.params["excludeDates"])
        if (isExcluded(cal, excludes)) return true
        when (trigger.type) {
            TriggerTypes.DAILY_AT, TriggerTypes.WEEKLY, TriggerTypes.WINDOW_REPEAT -> {
                val days = parseDaysOfWeek(trigger.params["daysOfWeek"])
                if (cal.get(Calendar.DAY_OF_WEEK) !in days) return true
            }
        }
        if (trigger.type == TriggerTypes.WINDOW_REPEAT && !isInWindow(cal, trigger)) return true
        return false
    }

    private fun nextOnce(trigger: Trigger, fromMillis: Long): Long? {
        val y = trigger.params["year"]?.toIntOrNull() ?: return null
        val mo = trigger.params["month"]?.toIntOrNull()?.coerceIn(1, 12) ?: return null
        val d = trigger.params["day"]?.toIntOrNull()?.coerceIn(1, 31) ?: return null
        val h = trigger.params["hour"]?.toIntOrNull()?.coerceIn(0, 23) ?: 0
        val mi = trigger.params["minute"]?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val cal = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.YEAR, y)
            set(Calendar.MONTH, mo - 1)
            set(Calendar.DAY_OF_MONTH, d)
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, mi)
        }
        val excludes = parseExcludeDates(trigger.params["excludeDates"])
        if (isExcluded(cal, excludes)) return null
        return cal.timeInMillis.takeIf { it > fromMillis }
    }

    private fun nextWeeklyLike(trigger: Trigger, fromMillis: Long): Long? {
        val hour = trigger.params["hour"]?.toIntOrNull()?.coerceIn(0, 23) ?: 9
        val minute = trigger.params["minute"]?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val days = parseDaysOfWeek(trigger.params["daysOfWeek"])
        val excludes = parseExcludeDates(trigger.params["excludeDates"])
        val cal = Calendar.getInstance().apply {
            timeInMillis = fromMillis
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        if (cal.timeInMillis <= fromMillis) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
        }
        repeat(400) {
            if (cal.get(Calendar.DAY_OF_WEEK) in days && !isExcluded(cal, excludes)) {
                return cal.timeInMillis
            }
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
        }
        return null
    }

    fun isInWindow(cal: Calendar, trigger: Trigger): Boolean {
        val start = minutesOfDay(
            trigger.params["startHour"]?.toIntOrNull() ?: 9,
            trigger.params["startMinute"]?.toIntOrNull() ?: 0,
        )
        val end = minutesOfDay(
            trigger.params["endHour"]?.toIntOrNull() ?: 18,
            trigger.params["endMinute"]?.toIntOrNull() ?: 0,
        )
        val now = minutesOfDay(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        return if (start == end) {
            true
        } else if (start < end) {
            now in start until end
        } else {
            now >= start || now < end
        }
    }

    private fun minutesOfDay(hour: Int, minute: Int): Int =
        hour.coerceIn(0, 23) * 60 + minute.coerceIn(0, 59)

    private fun nextWindowRepeat(trigger: Trigger, fromMillis: Long): Long? {
        val intervalMin = (trigger.params["intervalMinutes"]?.toLongOrNull() ?: 30L)
            .coerceIn(1L, 12L * 60L)
        val intervalMs = TimeUnit.MINUTES.toMillis(intervalMin)
        val days = parseDaysOfWeek(trigger.params["daysOfWeek"])
        val excludes = parseExcludeDates(trigger.params["excludeDates"])
        val startH = trigger.params["startHour"]?.toIntOrNull()?.coerceIn(0, 23) ?: 9
        val startM = trigger.params["startMinute"]?.toIntOrNull()?.coerceIn(0, 59) ?: 0

        var probe = fromMillis + 500L
        repeat(1500) {
            val cal = Calendar.getInstance().apply { timeInMillis = probe }
            if (cal.get(Calendar.DAY_OF_WEEK) !in days || isExcluded(cal, excludes)) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, startH)
                cal.set(Calendar.MINUTE, startM)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                probe = cal.timeInMillis
                return@repeat
            }
            if (!isInWindow(cal, trigger)) {
                val startToday = anchorWindowStart(cal, startH, startM)
                val startMin = minutesOfDay(startH, startM)
                val nowMin = minutesOfDay(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                val endMin = minutesOfDay(
                    trigger.params["endHour"]?.toIntOrNull() ?: 18,
                    trigger.params["endMinute"]?.toIntOrNull() ?: 0,
                )
                probe = when {
                    startMin < endMin && nowMin >= endMin -> {
                        startToday.add(Calendar.DAY_OF_YEAR, 1)
                        startToday.timeInMillis
                    }
                    startMin < endMin -> maxOf(startToday.timeInMillis, fromMillis + 1)
                    nowMin in endMin until startMin -> startToday.timeInMillis
                    else -> {
                        startToday.add(Calendar.DAY_OF_YEAR, 1)
                        startToday.timeInMillis
                    }
                }
                return@repeat
            }
            val windowStart = anchorWindowStart(cal, startH, startM)
            // 跨夜且当前在后半段：起点在昨天
            val startMin = minutesOfDay(startH, startM)
            val endMin = minutesOfDay(
                trigger.params["endHour"]?.toIntOrNull() ?: 18,
                trigger.params["endMinute"]?.toIntOrNull() ?: 0,
            )
            val nowMin = minutesOfDay(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
            if (startMin > endMin && nowMin < endMin) {
                windowStart.add(Calendar.DAY_OF_YEAR, -1)
            }
            var tick = windowStart.timeInMillis
            while (tick <= fromMillis) tick += intervalMs
            val tickCal = Calendar.getInstance().apply { timeInMillis = tick }
            if (isInWindow(tickCal, trigger) &&
                tickCal.get(Calendar.DAY_OF_WEEK) in days &&
                !isExcluded(tickCal, excludes)
            ) {
                return tick
            }
            probe = tick + 1
        }
        return null
    }

    private fun anchorWindowStart(cal: Calendar, startH: Int, startM: Int): Calendar {
        return Calendar.getInstance().apply {
            timeInMillis = cal.timeInMillis
            set(Calendar.HOUR_OF_DAY, startH)
            set(Calendar.MINUTE, startM)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}
