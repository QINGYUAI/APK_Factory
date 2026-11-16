// Author: PKQ32
// Created: 2026-07-12
// Description: 完整日程调度：一次/每日/每周/时段循环 + AlarmManager

package com.example.myapplication.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.myapplication.di.ServiceLocator
import com.example.myapplication.domain.model.MiniApp
import com.example.myapplication.domain.model.Trigger
import com.example.myapplication.domain.model.TriggerTypes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object CalendarScheduler {

    fun resync(context: Context, apps: List<MiniApp>) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previous = prefs.getStringSet(KEY_IDS, emptySet()).orEmpty().toSet()
        val desired = linkedSetOf<String>()

        for (app in apps) {
            if (!app.enabled) continue
            for (trigger in app.triggers) {
                if (!trigger.enabled) continue
                if (!TriggerTypes.isCalendarType(trigger.type)) continue
                val key = alarmKey(app.id, trigger.id)
                desired.add(key)
                val next = ScheduleTimeMath.nextFireMillis(trigger) ?: continue
                setAlarm(context, am, key, app.id, trigger.id, next)
            }
        }
        for (orphan in previous - desired) {
            cancel(context, am, orphan)
        }
        prefs.edit().putStringSet(KEY_IDS, HashSet(desired)).apply()
    }

    fun scheduleNext(context: Context, miniAppId: String, trigger: Trigger) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val key = alarmKey(miniAppId, trigger.id)
        val next = ScheduleTimeMath.nextFireMillis(trigger) ?: run {
            cancel(context, am, key)
            return
        }
        setAlarm(context, am, key, miniAppId, trigger.id, next)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY_IDS, emptySet()).orEmpty().toMutableSet()
        set.add(key)
        prefs.edit().putStringSet(KEY_IDS, HashSet(set)).apply()
    }

    private fun setAlarm(
        context: Context,
        am: AlarmManager,
        key: String,
        miniAppId: String,
        triggerId: String,
        atMillis: Long,
    ) {
        val pi = pendingIntent(context, key, miniAppId, triggerId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        } else {
            runCatching {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
            }.onFailure {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
            }
        }
    }

    private fun cancel(context: Context, am: AlarmManager, key: String) {
        val parts = key.split("|")
        if (parts.size != 2) return
        am.cancel(pendingIntent(context, key, parts[0], parts[1]))
    }

    private fun pendingIntent(
        context: Context,
        key: String,
        miniAppId: String,
        triggerId: String,
    ): PendingIntent {
        val intent = Intent(context, CalendarAlarmReceiver::class.java).apply {
            action = ACTION_FIRE
            putExtra(EXTRA_MINI_APP_ID, miniAppId)
            putExtra(EXTRA_TRIGGER_ID, triggerId)
            putExtra(EXTRA_KEY, key)
        }
        return PendingIntent.getBroadcast(
            context,
            key.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun alarmKey(miniAppId: String, triggerId: String): String = "$miniAppId|$triggerId"

    const val ACTION_FIRE = "com.example.myapplication.action.CALENDAR_FIRE"
    const val ACTION_DAILY = "com.example.myapplication.action.DAILY_AT" // 兼容旧广播
    const val EXTRA_MINI_APP_ID = "miniAppId"
    const val EXTRA_TRIGGER_ID = "triggerId"
    const val EXTRA_KEY = "key"
    private const val PREFS = "calendar_scheduler"
    private const val KEY_IDS = "alarm_keys"
}

class CalendarAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != CalendarScheduler.ACTION_FIRE && action != CalendarScheduler.ACTION_DAILY) return
        val miniAppId = intent.getStringExtra(CalendarScheduler.EXTRA_MINI_APP_ID) ?: return
        val triggerId = intent.getStringExtra(CalendarScheduler.EXTRA_TRIGGER_ID)
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                handleFire(context, miniAppId, triggerId)
            } finally {
                pending?.finish()
            }
        }
    }

    private suspend fun handleFire(context: Context, miniAppId: String, triggerId: String?) {
        val app = ServiceLocator.miniApps.get(miniAppId) ?: return
        if (!app.enabled) return
        val trigger = app.triggers.firstOrNull { it.id == triggerId && it.enabled }
            ?: app.triggers.firstOrNull {
                it.enabled && TriggerTypes.isCalendarType(it.type)
            }
            ?: return

        if (ScheduleTimeMath.shouldSkipFire(trigger)) {
            CalendarScheduler.scheduleNext(context, miniAppId, trigger)
            return
        }

        ServiceLocator.runner.runQueued(miniAppId, trigger.type)

        if (trigger.type == TriggerTypes.ONCE_AT) {
            val updated = app.copy(
                triggers = app.triggers.map {
                    if (it.id == trigger.id) it.copy(enabled = false) else it
                },
                updatedAt = System.currentTimeMillis(),
            )
            ServiceLocator.miniApps.save(updated)
            ServiceLocator.triggerManager.resync()
        } else {
            CalendarScheduler.scheduleNext(context, miniAppId, trigger)
        }
    }
}

/** 兼容旧入口：TriggerManager 统一走 CalendarScheduler。 */
object DailyAtScheduler {
    fun resync(context: Context, apps: List<MiniApp>) {
        CalendarScheduler.resync(context, apps)
    }
}
