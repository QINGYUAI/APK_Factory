// Author: PKQ32
// Created: 2026-11-16
// Description: 日程触发参数校验单测

package com.example.myapplication.platform

import com.example.myapplication.domain.model.Trigger
import com.example.myapplication.domain.model.TriggerTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScheduleValidationTest {

    private fun trigger(type: String, params: Map<String, String>) = Trigger(
        id = "t",
        type = type,
        params = params,
    )

    @Test
    fun daily_valid() {
        assertNull(
            ScheduleValidation.validate(
                trigger(TriggerTypes.DAILY_AT, mapOf("hour" to "9", "minute" to "30")),
            ),
        )
    }

    @Test
    fun daily_hourOutOfRange() {
        assertEquals(
            "小时须为 0–23",
            ScheduleValidation.validate(
                trigger(TriggerTypes.DAILY_AT, mapOf("hour" to "24", "minute" to "0")),
            ),
        )
    }

    @Test
    fun weekly_requiresAtLeastOneDay() {
        assertEquals(
            "每周重复请至少选择一天",
            ScheduleValidation.validate(
                trigger(TriggerTypes.WEEKLY, mapOf("hour" to "9", "minute" to "0", "daysOfWeek" to "")),
            ),
        )
    }

    @Test
    fun window_rejectsZeroInterval() {
        assertEquals(
            "间隔须为 1–720 分钟",
            ScheduleValidation.validate(
                trigger(
                    TriggerTypes.WINDOW_REPEAT,
                    mapOf(
                        "startHour" to "9", "startMinute" to "0",
                        "endHour" to "18", "endMinute" to "0",
                        "intervalMinutes" to "0",
                    ),
                ),
            ),
        )
    }

    @Test
    fun once_rejectsInvalidMonth() {
        assertEquals(
            "月份须为 1–12",
            ScheduleValidation.validate(
                trigger(
                    TriggerTypes.ONCE_AT,
                    mapOf("year" to "2026", "month" to "13", "day" to "1"),
                ),
            ),
        )
    }

    @Test
    fun excludeDates_rejectsBadFormat() {
        assertEquals(
            "例外日期格式错误：2026/11/16",
            ScheduleValidation.validate(
                trigger(
                    TriggerTypes.DAILY_AT,
                    mapOf("hour" to "9", "minute" to "0", "excludeDates" to "2026/11/16"),
                ),
            ),
        )
    }
}
