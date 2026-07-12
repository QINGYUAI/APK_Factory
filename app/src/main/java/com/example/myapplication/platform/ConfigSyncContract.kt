// Author: PKQ32
// Created: 2026-07-12
// Description: 工厂与独立小应用之间同步配置的约定常量

package com.example.myapplication.platform

object ConfigSyncContract {
    const val ACTION_APPLY_CONFIG = "com.example.minirunner.action.APPLY_CONFIG"
    const val EXTRA_MINI_APP_ID = "miniAppId"
    const val EXTRA_MINI_APP_NAME = "miniAppName"
    const val MIME_ZIP = "application/zip"
}
