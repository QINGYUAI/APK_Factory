// Author: PKQ32
// Created: 2026-11-16
// Description: 在线更新数据模型与 UI 状态

package com.example.myapplication.update

/** GitHub Release 中的可用更新。 */
data class UpdateInfo(
    /** 去掉前缀 v 的版本号，例如 1.1.0 */
    val version: String,
    val tagName: String,
    val notes: String,
    val pageUrl: String,
    val apkUrl: String,
    val apkSize: Long,
)

data class UpdateUiState(
    val currentVersion: String = "",
    val currentVersionCode: Long = 0,
    val autoCheck: Boolean = true,
    val checking: Boolean = false,
    val info: UpdateInfo? = null,
    val showDialog: Boolean = false,
    val error: String? = null,
    val downloading: Boolean = false,
    /** 0–100；未知总大小时保持 0 */
    val progress: Int = 0,
    /** 一次性提示（由根界面消费后弹 Toast） */
    val message: String? = null,
)
