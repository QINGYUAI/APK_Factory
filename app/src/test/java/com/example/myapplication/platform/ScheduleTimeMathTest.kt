// Author: PKQ32
// Created: 2026-11-16
// Description: 日程时间计算单测：一次/每日/每周/例外日期/时段循环

package com.example.myapplication.platform

import com.example.myapplication.domain.model.Trigger
import com.example.myapplication.domain.model.TriggerTypes
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ScheduleTimeMathTest {

    private lateinit var originalZone: TimeZone

    @Before
    fun setUp() {
        originalZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalZone)
    }

    @Test
    fun onceAt_returnsFutureTime() {
        val trigger = trigger(
            TriggerTypes.ONCE_AT,
            mapOf(
                "year" to "2026", "month" to "12", "day" to "1",
                "hour" to "8", "minute" to "30",
            ),
        )
        val from = at(2026, 11, 16, 10, 0)
        assertEquals(at(2026, 12, 1, 8, 30), ScheduleTimeMath.nextFireMillis(trigger, from))
    }

    @Test
    fun onceAt_pastTimeReturnsNull() {
        val trigger = trigger(
            TriggerTypes.ONCE_AT,
            mapOf(
                "year" to "2026", "month" to "11", "day" to "1",
                "hour" to "8", "minute" to "0",
            ),
        )
        assertNull(ScheduleTimeMath.nextFireMillis(trigger, at(2026, 11, 16, 10, 0)))
    }

    @Test
    fun dailyAt_usesTodayWhenAhead() {
        val trigger = trigger(TriggerTypes.DAILY_AT, mapOf("hour" to "7", "minute" to "0"))
        assertEquals(
            at(2026, 11, 16, 7, 0),
            ScheduleTimeMath.nextFireMillis(trigger, at(2026, 11, 16, 6, 0)),
        )
    }

    @Test
    fun dailyAt_rollsToTomorrowWhenPassed() {
        val trigger = trigger(TriggerTypes.DAILY_AT, mapOf("hour" to "7", "minute" to "0"))
        assertEquals(
            at(2026, 11, 17, 7, 0),
            ScheduleTimeMath.nextFireMillis(trigger, at(2026, 11, 16, 8, 0)),
        )
    }

    @Test
    fun weekly_skipsDisallowedDays() {
        // 2026-11-16 是周一，daysOfWeek=2 表示周一
        val trigger = trigger(
            TriggerTypes.WEEKLY,
            mapOf("hour" to "9", "minute" to "0", "daysOfWeek" to "2"),
        )
        assertEquals(
            at(2026, 11, 23, 9, 0),
            ScheduleTimeMath.nextFireMillis(trigger, at(2026, 11, 16, 10, 0)),
        )
    }

    @Test
    fun excludeDates_skipsToday() {
        val trigger = trigger(
            TriggerTypes.DAILY_AT,
            mapOf("hour" to "7", "minute" to "0", "excludeDates" to "2026-11-16"),
        )
        assertEquals(
            at(2026, 11, 17, 7, 0),
            ScheduleTimeMath.nextFireMillis(trigger, at(2026, 11, 16, 6, 0)),
        )
    }

    @Test
    fun windowRepeat_insideWindow_returnsNextTick() {
        val trigger = windowTrigger(startHour = "9", endHour = "18", interval = "60")
        assertEquals(
            at(2026, 11, 16, 11, 0),
            ScheduleTimeMath.nextFireMillis(trigger, at(2026, 11, 16, 10, 10)),
        )
    }

    @Test
    fun windowRepeat_beforeWindow_returnsWindowStart() {
        val trigger = windowTrigger(startHour = "9", endHour = "18", interval = "60")
        assertEquals(
            at(2026, 11, 16, 9, 0),
            ScheduleTimeMath.nextFireMillis(trigger, at(2026, 11, 16, 8, 0)),
        )
    }

    @Test
    fun windowRepeat_afterWindow_returnsTomorrowStart() {
        val trigger = windowTrigger(startHour = "9", endHour = "18", interval = "60")
        assertEquals(
            at(2026, 11, 17, 9, 0),
            ScheduleTimeMath.nextFireMillis(trigger, at(2026, 11, 16, 19, 0)),
        )
    }

    @Test
    fun windowRepeat_crossMidnight_afterMidnight() {
        // 22:00–02:00，00:10 之后的下一个刻度是 00:30
        val trigger = windowTrigger(startHour = "22", endHour = "2", interval = "30")
        assertEquals(
            at(2026, 11, 17, 0, 30),
            ScheduleTimeMath.nextFireMillis(trigger, at(2026, 11, 17, 0, 10)),
        )
    }

    @Test
    fun windowRepeat_crossMidnight_beforeEveningStart() {
        // 凌晨 03:00 已过窗口，下一次是当天 22:00
        val trigger = windowTrigger(startHour = "22", endHour = "2", interval = "30")
        assertEquals(
            at(2026, 11, 17, 22, 0),
            ScheduleTimeMath.nextFireMillis(trigger, at(2026, 11, 17, 3, 0)),
        )
    }

    @Test
    fun isInWindow_crossMidnight() {
        val trigger = windowTrigger(startHour = "22", endHour = "2", interval = "30")
        assertTrue(ScheduleTimeMath.isInWindow(calendarAt(2026, 11, 16, 23, 0), trigger))
        assertTrue(ScheduleTimeMath.isInWindow(calendarAt(2026, 11, 17, 1, 0), trigger))
        assertFalse(ScheduleTimeMath.isInWindow(calendarAt(2026, 11, 17, 12, 0), trigger))
    }

    @Test
    fun shouldSkipFire_onExcludedDay() {
        val trigger = trigger(
            TriggerTypes.WEEKLY,
            mapOf("hour" to "9", "minute" to "0", "daysOfWeek" to "2"),
        )
        // 2026-11-17 周二，不在周一计划内
        assertTrue(ScheduleTimeMath.shouldSkipFire(trigger, at(2026, 11, 17, 9, 0)))
        assertFalse(ScheduleTimeMath.shouldSkipFire(trigger, at(2026, 11, 23, 9, 0)))
    }

    private fun trigger(type: String, params: Map<String, String>) = Trigger(
        id = "trigger-$type",
        type = type,
        params = params,
    )

    private fun windowTrigger(startHour: String, endHour: String, interval: String) = trigger(
        TriggerTypes.WINDOW_REPEAT,
        mapOf(
            "startHour" to startHour,
            "startMinute" to "0",
            "endHour" to endHour,
            "endMinute" to "0",
            "intervalMinutes" to interval,
        ),
    )

    private fun calendarAt(year: Int, month: Int, day: Int, hour: Int, minute: Int): Calendar =
        Calendar.getInstance().apply {
            set(year, month - 1, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        calendarAt(year, month, day, hour, minute).timeInMillis
}
