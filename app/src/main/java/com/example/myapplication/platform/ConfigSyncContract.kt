// Author: PKQ32
// Created: 2026-07-12
// Description: 工厂与独立小应用之间同步配置的约定常量（action 不可带模板包名前缀，避免导出改写）

package com.example.myapplication.platform

object ConfigSyncContract {
    /** 固定 action，禁止使用 com.example.minirunner.* 前缀（导出时会被改写包名） */
    const val ACTION_APPLY_CONFIG = "com.apkfactory.action.APPLY_MINIAPP_CONFIG"
    const val RECEIVER_ACTIVITY = "com.example.minirunner.ConfigSyncActivity"
    const val EXTRA_MINI_APP_ID = "miniAppId"
    const val EXTRA_MINI_APP_NAME = "miniAppName"
    const val MIME_ZIP = "application/zip"
}
