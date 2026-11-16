// Author: PKQ32
// Created: 2026-07-12
// Description: 导出前预检：媒体文件、触发器能力说明

package com.example.myapplication.export

import android.content.Context
import com.example.myapplication.domain.model.ActionTypes
import com.example.myapplication.domain.model.MiniApp
import com.example.myapplication.domain.model.TriggerTypes
import com.example.myapplication.platform.MediaUriHelper
import com.example.myapplication.platform.ScheduleValidation
import com.example.myapplication.platform.SystemEventCatalog
import com.example.myapplication.ui.editor.actionTitleZh
import java.io.File

data class ExportPreflightResult(
    val errors: List<String>,
    val warnings: List<String>,
) {
    val canExport: Boolean get() = errors.isEmpty()
}

object ExportPreflight {

    fun check(context: Context, miniApp: MiniApp): ExportPreflightResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (miniApp.name.isBlank()) {
            errors += "小应用名称不能为空"
        }
        if (miniApp.steps.isEmpty()) {
            errors += "至少需要一个动作步骤才能导出"
        }

        miniApp.steps.forEachIndexed { index, step ->
            val label = "第 ${index + 1} 步「${actionTitleZh(step.type)}」"
            when (step.type) {
                ActionTypes.PLAY_MUSIC, ActionTypes.PLAY_VIDEO -> {
                    if (!hasExportableMedia(context, step.params)) {
                        errors += "$label 缺少可导出的媒体文件，请重新选择后再导出"
                    }
                }
                ActionTypes.SCREENSHOT -> {
                    warnings += "$label 截图动作在独立包中仍不可用（一期未完成）"
                }
                ActionTypes.WIFI -> {
                    warnings += "$label 仅会打开系统 Wi‑Fi 面板，不能在后台静默开关"
                }
                ActionTypes.OPEN_APP -> {
                    if (step.params["packageName"].isNullOrBlank()) {
                        errors += "$label 未填写目标包名"
                    }
                }
            }
        }

        miniApp.triggers.filter { it.enabled }.forEach { trigger ->
            if (TriggerTypes.isCalendarType(trigger.type)) {
                ScheduleValidation.validate(trigger)?.let { err ->
                    errors += "触发器「${trigger.type}」：$err"
                }
            }
        }

        val factoryOnlyTriggers = miniApp.triggers.filter { trigger ->
            if (!trigger.enabled) return@filter false
            when (trigger.type) {
                TriggerTypes.APP_OPENED -> true
                TriggerTypes.EVENT -> {
                    val action = trigger.params["action"].orEmpty()
                    action.isNotBlank() && SystemEventCatalog.of(action)?.dynamicOnly == true
                }
                else -> false
            }
        }
        if (factoryOnlyTriggers.isNotEmpty()) {
            warnings += "「打开指定 App」「亮屏/灭屏」触发仅在应用工厂内生效；独立包已支持定时与开关机、充电等系统事件触发"
        }
        val hasBackgroundTriggers = miniApp.triggers.any { trigger ->
            trigger.enabled && when (trigger.type) {
                TriggerTypes.SCHEDULE, TriggerTypes.EVENT -> true
                else -> TriggerTypes.isCalendarType(trigger.type)
            }
        }
        if (hasBackgroundTriggers) {
            warnings += "独立包安装后请先打开一次，系统才会允许其后台触发；定时触发需允许「闹钟和提醒」，未授权时使用近似时间，个别机型还需允许自启动/后台运行；后台播放音视频、打开其他 App 可能被系统限制"
        }
        if (miniApp.runOnHostOpen) {
            warnings += "「打开本应用时自动运行」在工厂内按配置执行；独立包每次打开都会自动运行一次动作链"
        }
        warnings += "独立包使用本机专属签名：卸载「应用工厂」或清除其数据后，将无法覆盖更新已安装的小应用"

        return ExportPreflightResult(errors = errors, warnings = warnings)
    }

    private fun hasExportableMedia(context: Context, params: Map<String, String>): Boolean {
        val path = params["path"]?.takeIf { it.isNotBlank() }
        if (path != null && !MediaUriHelper.isAssetPath(path)) {
            val file = File(path)
            if (file.exists() && file.isFile && file.length() > 0L) return true
        }
        if (MediaUriHelper.isAssetPath(path.orEmpty())) return true
        val uri = params["uri"]?.takeIf { it.isNotBlank() } ?: return false
        return runCatching {
            val file = MediaUriHelper.copyToPrivateMedia(context, android.net.Uri.parse(uri))
            file.exists() && file.length() > 0L
        }.getOrDefault(false)
    }
}
