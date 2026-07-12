// Author: PKQ32
// Created: 2026-07-11
// Description: 小应用核心领域模型与动作/触发器类型常量

package com.example.myapplication.domain.model

import com.example.myapplication.domain.FailurePolicy

data class MiniApp(
    val id: String,
    val name: String,
    val iconKey: String = "default",
    val enabled: Boolean = true,
    val runOnHostOpen: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val steps: List<ActionStep> = emptyList(),
    val triggers: List<Trigger> = emptyList(),
)

data class ActionStep(
    val id: String,
    val type: String,
    val params: Map<String, String> = emptyMap(),
    val order: Int,
    val onFailure: FailurePolicy = FailurePolicy.ABORT,
)

data class Trigger(
    val id: String,
    val type: String, // manual | schedule | event | appOpened | hostOpened
    val params: Map<String, String> = emptyMap(),
    val enabled: Boolean = true,
)

data class RunLog(
    val id: String,
    val miniAppId: String,
    val startedAt: Long,
    val endedAt: Long?,
    val stepResultsJson: String,
    val success: Boolean,
)

object ActionTypes {
    const val VIBRATE = "vibrate"
    const val PLAY_MUSIC = "playMusic"
    const val PLAY_VIDEO = "playVideo"
    const val SET_VOLUME = "setVolume"
    const val FLASHLIGHT = "flashlight"
    const val SET_BRIGHTNESS = "setBrightness"
    const val OPEN_APP = "openApp"
    const val DELAY = "delay"
    const val WIFI = "wifi"
    const val BLUETOOTH = "bluetooth"
    const val SET_RINGER_MODE = "setRingerMode"
    const val SCREENSHOT = "screenshot"
    const val NOTIFY = "notify"
}

object TriggerTypes {
    const val MANUAL = "manual"
    const val SCHEDULE = "schedule"
    const val EVENT = "event"
    const val APP_OPENED = "appOpened"
    const val HOST_OPENED = "hostOpened"
    /** 每天固定时刻，params: hour / minute / daysOfWeek / excludeDates */
    const val DAILY_AT = "dailyAt"
    /** 一次性：year,month,day,hour,minute */
    const val ONCE_AT = "onceAt"
    /** 每周：hour,minute,daysOfWeek(1=周日…7=周六),excludeDates */
    const val WEEKLY = "weekly"
    /** 时段内循环：startHour/startMinute/endHour/endMinute/intervalMinutes/daysOfWeek/excludeDates */
    const val WINDOW_REPEAT = "windowRepeat"

    fun isCalendarType(type: String): Boolean = type == DAILY_AT ||
        type == ONCE_AT ||
        type == WEEKLY ||
        type == WINDOW_REPEAT
}
