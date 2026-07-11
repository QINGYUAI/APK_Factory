// Author: PKQ32
// Created: 2026-07-11
// Description: 主 Activity：加载导航，解析快捷方式/deep link，冷启动执行 hostOpened

package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.di.ServiceLocator
import com.example.myapplication.domain.model.TriggerTypes
import com.example.myapplication.ui.navigation.AppNav
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 桌面快捷方式 / deep link：extras 或 miniapp://run/{id}
        val deepLinkMiniAppId = resolveMiniAppId(intent)
        maybeRunHostOpened(savedInstanceState)
        setContent {
            MyApplicationTheme {
                AppNav(deepLinkMiniAppId = deepLinkMiniAppId)
            }
        }
    }

    /**
     * 进程冷启动时执行一次 hostOpened（避免配置变更重建导致重复）。
     */
    private fun maybeRunHostOpened(savedInstanceState: Bundle?) {
        if (savedInstanceState != null || hostOpenHandled) return
        hostOpenHandled = true
        lifecycleScope.launch {
            val app = ServiceLocator.miniApps.getHostOpenApp() ?: return@launch
            ServiceLocator.runner.run(app.id, TriggerTypes.HOST_OPENED)
        }
    }

    companion object {
        const val EXTRA_MINI_APP_ID = "miniAppId"

        /** 进程级标志：同一进程内仅尝试一次 hostOpened */
        @Volatile
        private var hostOpenHandled: Boolean = false

        /**
         * 从 Intent extras（miniAppId）或 data URI（miniapp://run/{id}）解析小应用 ID。
         */
        fun resolveMiniAppId(intent: Intent?): String? {
            if (intent == null) return null
            intent.getStringExtra(EXTRA_MINI_APP_ID)?.takeIf { it.isNotBlank() }?.let { return it }
            val data = intent.data ?: return null
            if (data.scheme != "miniapp" || data.host != "run") return null
            return data.pathSegments.firstOrNull()?.takeIf { it.isNotBlank() }
        }
    }
}
