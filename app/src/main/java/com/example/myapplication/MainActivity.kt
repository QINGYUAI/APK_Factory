// Author: PKQ32
// Created: 2026-07-11
// Description: 主 Activity：加载导航，冷启动时执行 hostOpened 小应用

package com.example.myapplication

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
        // 桌面快捷方式 / 外部唤起可携带 miniAppId，后续导航到运行页
        val deepLinkMiniAppId = intent?.getStringExtra(EXTRA_MINI_APP_ID)
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
    }
}
