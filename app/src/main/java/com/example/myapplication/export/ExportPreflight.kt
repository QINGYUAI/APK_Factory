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

        val autoTriggers = miniApp.triggers.filter {
            it.enabled && it.type != TriggerTypes.MANUAL && it.type != TriggerTypes.HOST_OPENED
        }
        if (autoTriggers.isNotEmpty()) {
            warnings += "独立安装包目前以手动运行为主；定时/系统事件等触发请在「应用工厂」内使用。改动作可优先用「同步配置」，换图标/引擎再用「更新应用」"
        }
        if (miniApp.runOnHostOpen) {
            warnings += "「打开本应用时自动运行」仅对工厂生效，不会带到独立安装包"
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
