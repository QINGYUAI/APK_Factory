// Author: PKQ32
// Created: 2026-11-16
// Description: 独立包闹钟接收器：校验触发器后重排下一次并入队执行

package com.example.minirunner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.myapplication.domain.model.TriggerTypes
import com.example.myapplication.platform.MiniAppConfigStore
import com.example.myapplication.platform.ScheduleTimeMath

class StandaloneAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != StandaloneTriggerScheduler.ACTION_FIRE) return
        val triggerId = intent.getStringExtra(StandaloneTriggerScheduler.EXTRA_TRIGGER_ID) ?: return
        val app = runCatching { MiniAppConfigStore.load(context) }.getOrNull()
        val trigger = app?.triggers?.firstOrNull { it.id == triggerId && it.enabled }
        if (app == null || !app.enabled || trigger == null) {
            StandaloneTriggerScheduler.cancel(context, triggerId)
            return
        }
        if (ScheduleTimeMath.shouldSkipFire(trigger)) {
            StandaloneTriggerScheduler.scheduleNext(context, trigger)
            return
        }
        if (trigger.type == TriggerTypes.ONCE_AT) {
            StandaloneTriggerScheduler.markOnceFired(context, triggerId)
        }
        StandaloneTriggerScheduler.scheduleNext(context, trigger)
        StandaloneTriggerScheduler.enqueueRun(context)
    }
}
