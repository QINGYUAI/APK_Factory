// Author: PKQ32
// Created: 2026-11-16
// Description: 独立小应用的触发器调度：定时/每日/每周/时段/间隔 + 事件与开机恢复

package com.example.minirunner

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.myapplication.domain.model.Trigger
import com.example.myapplication.domain.model.TriggerTypes
import com.example.myapplication.platform.MiniAppConfigStore
import com.example.myapplication.platform.ScheduleTimeMath
import java.util.concurrent.TimeUnit

/**
 * 独立包内没有应用工厂的数据库与 ServiceLocator：
 * 直接读取嵌入/同步的 miniapp.json，把启用的触发器落到 AlarmManager，
 * 触发时用 WorkManager 执行动作链（避免广播接收器 10 秒限制）。
 */
object StandaloneTriggerScheduler {

    const val ACTION_FIRE = "com.apkfactory.action.STANDALONE_TRIGGER"
    const val EXTRA_TRIGGER_ID = "triggerId"

    private const val PREFS = "standalone_triggers"
    private const val KEY_ALARMS = "alarm_keys"
    private const val KEY_FIRED_ONCE = "fired_once"
    private const val WORK_NAME = "standalone-trigger-run"

    /** 与配置对齐：取消已删除/停用触发器的闹钟，重新布防全部启用触发器。 */
    fun resync(context: Context, resetOnceFired: Boolean = false) {
        val app = runCatching { MiniAppConfigStore.load(context) }.getOrNull() ?: return
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (resetOnceFired) prefs.edit().remove(KEY_FIRED_ONCE).apply()
        val firedOnce = prefs.getStringSet(KEY_FIRED_ONCE, emptySet()).orEmpty()
        val desired = linkedSetOf<String>()

        if (app.enabled) {
            for (trigger in app.triggers) {
                if (!trigger.enabled) continue
                if (trigger.type == TriggerTypes.ONCE_AT && trigger.id in firedOnce) continue
                val next = nextFireAt(trigger) ?: continue
                desired.add(trigger.id)
                setAlarm(context, am, trigger.id, next)
            }
        }

        val previous = prefs.getStringSet(KEY_ALARMS, emptySet()).orEmpty()
        for (orphan in previous - desired) {
            cancel(context, am, orphan)
        }
        prefs.edit().putStringSet(KEY_ALARMS, HashSet(desired)).apply()
    }

    /** 触发后计算下一次并重新布防；一次性触发器会自然取消。 */
    fun scheduleNext(context: Context, trigger: Trigger) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val next = nextFireAt(trigger)
        if (next == null) {
            cancel(context, am, trigger.id)
            val keys = prefs.getStringSet(KEY_ALARMS, emptySet()).orEmpty().toMutableSet()
            keys.remove(trigger.id)
            prefs.edit().putStringSet(KEY_ALARMS, HashSet(keys)).apply()
            return
        }
        setAlarm(context, am, trigger.id, next)
        val keys = prefs.getStringSet(KEY_ALARMS, emptySet()).orEmpty().toMutableSet()
        keys.add(trigger.id)
        prefs.edit().putStringSet(KEY_ALARMS, HashSet(keys)).apply()
    }

    fun cancel(context: Context, triggerId: String) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        cancel(context, am, triggerId)
    }

    fun markOnceFired(context: Context, triggerId: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val fired = prefs.getStringSet(KEY_FIRED_ONCE, emptySet()).orEmpty().toMutableSet()
        fired.add(triggerId)
        val keys = prefs.getStringSet(KEY_ALARMS, emptySet()).orEmpty().toMutableSet()
        keys.remove(triggerId)
        prefs.edit()
            .putStringSet(KEY_FIRED_ONCE, HashSet(fired))
            .putStringSet(KEY_ALARMS, HashSet(keys))
            .apply()
    }

    /** 用 WorkManager 串行执行动作链；同一时刻多个触发器排队而不是丢弃。 */
    fun enqueueRun(context: Context) {
        runCatching {
            val request = OneTimeWorkRequestBuilder<StandaloneTriggerWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request,
            )
        }
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(AlarmManager::class.java) ?: return false
        return am.canScheduleExactAlarms()
    }

    private fun nextFireAt(trigger: Trigger): Long? = when {
        TriggerTypes.isCalendarType(trigger.type) -> ScheduleTimeMath.nextFireMillis(trigger)
        trigger.type == TriggerTypes.SCHEDULE -> {
            // 间隔触发按固定时间片对齐，反复启动/重同步不会不断顺延
            val minutes = (trigger.params["intervalMinutes"]?.toLongOrNull() ?: 15L)
                .coerceIn(1L, 24L * 60L)
            val intervalMs = TimeUnit.MINUTES.toMillis(minutes)
            val now = System.currentTimeMillis()
            (now / intervalMs + 1) * intervalMs
        }
        else -> null
    }

    private fun setAlarm(context: Context, am: AlarmManager, triggerId: String, atMillis: Long) {
        val pi = pendingIntent(context, triggerId)
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

    private fun cancel(context: Context, am: AlarmManager, triggerId: String) {
        am.cancel(pendingIntent(context, triggerId))
    }

    private fun pendingIntent(context: Context, triggerId: String): PendingIntent {
        val intent = Intent(context, StandaloneAlarmReceiver::class.java).apply {
            action = ACTION_FIRE
            putExtra(EXTRA_TRIGGER_ID, triggerId)
        }
        return PendingIntent.getBroadcast(
            context,
            triggerId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
