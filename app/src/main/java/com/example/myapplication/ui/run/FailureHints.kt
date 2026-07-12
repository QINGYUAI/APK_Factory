// Author: PKQ32
// Created: 2026-07-12
// Description: 将动作失败码映射为中文说明与系统授权跳转

package com.example.myapplication.ui.run

import android.content.Context
import com.example.myapplication.domain.ActionResult
import com.example.myapplication.domain.model.ActionTypes
import com.example.myapplication.engine.StepOutcome
import com.example.myapplication.platform.PermissionHelper

data class FailureActionHint(
    val buttonLabel: String,
    val onClick: (Context) -> Unit,
)

/**
 * 根据失败步骤给出中文详情与可选「去授权」操作。
 */
fun failureDetailZh(outcome: StepOutcome): String {
    val failed = outcome.result as? ActionResult.Failed ?: return "执行完成"
    // 已是中文则直接展示；兼容旧英文文案做兜底
    val msg = failed.message
    if (msg.any { it.code > 127 }) return msg
    return when (failed.code) {
        "NO_PERMISSION" -> when (outcome.type) {
            ActionTypes.SET_RINGER_MODE -> "需要「勿扰权限」才能切换响铃模式"
            ActionTypes.NOTIFY -> "需要「通知权限」才能发送通知"
            ActionTypes.PLAY_MUSIC -> "需要「读取音频」权限才能播放音乐"
            ActionTypes.PLAY_VIDEO -> "无法读取所选视频，请重新选择文件"
            ActionTypes.BLUETOOTH -> "需要「附近的设备 / 蓝牙」权限"
            ActionTypes.FLASHLIGHT -> "手电筒权限被拒绝"
            ActionTypes.WIFI -> "Wi‑Fi 权限被拒绝"
            else -> "缺少所需权限"
        }
        "NEEDS_SETTINGS" -> "需要「修改系统设置」权限（如调节亮度）"
        "NEEDS_UI" -> "该动作需要额外用户授权界面（一期暂未完成）"
        "UNSUPPORTED" -> "当前系统不支持此操作，请到系统设置中手动处理"
        "NOT_INSTALLED" -> "目标应用未安装"
        "FILE_NOT_FOUND" -> "找不到音频文件"
        else -> msg
    }
}

fun failureActionHint(outcome: StepOutcome): FailureActionHint? {
    val failed = outcome.result as? ActionResult.Failed ?: return null
    return when {
        failed.code == "NEEDS_SETTINGS" ||
            (outcome.type == ActionTypes.SET_BRIGHTNESS &&
                failed.code in setOf("NEEDS_SETTINGS", "NO_PERMISSION")) ->
            FailureActionHint("去授权修改系统设置") { PermissionHelper.openWriteSettings(it) }

        outcome.type == ActionTypes.SET_RINGER_MODE && failed.code == "NO_PERMISSION" ->
            FailureActionHint("去授权勿扰权限") {
                PermissionHelper.openNotificationPolicySettings(it)
            }

        outcome.type == ActionTypes.NOTIFY && failed.code == "NO_PERMISSION" ->
            FailureActionHint("去授权通知权限") {
                PermissionHelper.openAppNotificationSettings(it)
            }

        outcome.type == ActionTypes.PLAY_MUSIC && failed.code == "NO_PERMISSION" ->
            FailureActionHint("去应用权限设置") {
                PermissionHelper.openAppDetailsSettings(it)
            }

        outcome.type == ActionTypes.BLUETOOTH && failed.code == "NO_PERMISSION" ->
            FailureActionHint("去应用权限设置") {
                PermissionHelper.openAppDetailsSettings(it)
            }

        failed.code == "UNSUPPORTED" &&
            outcome.type in setOf(ActionTypes.WIFI, ActionTypes.BLUETOOTH) ->
            FailureActionHint("打开系统设置") {
                PermissionHelper.openAppDetailsSettings(it)
            }

        else -> null
    }
}
