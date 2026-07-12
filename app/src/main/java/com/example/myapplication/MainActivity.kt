// Author: PKQ32
// Created: 2026-07-11
// Description: 主 Activity：导航、deep link；冷启动与回到前台时触发 hostOpened

package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.myapplication.di.ServiceLocator
import com.example.myapplication.domain.model.TriggerTypes
import com.example.myapplication.ui.navigation.AppNav
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val deepLinkMiniAppId = resolveMiniAppId(intent)
        setContent {
            MyApplicationTheme {
                AppNav(deepLinkMiniAppId = deepLinkMiniAppId)
            }
        }
        // 冷启动与回到前台（防抖由 MiniAppRunner 处理）
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                maybeRunHostOpened()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val id = resolveMiniAppId(intent) ?: return
        setContent {
            MyApplicationTheme {
                AppNav(deepLinkMiniAppId = id)
            }
        }
    }

    private suspend fun maybeRunHostOpened() {
        val app = ServiceLocator.miniApps.getHostOpenApp() ?: return
        ServiceLocator.runner.run(app.id, TriggerTypes.HOST_OPENED)
    }

    companion object {
        const val EXTRA_MINI_APP_ID = "miniAppId"

        fun resolveMiniAppId(intent: Intent?): String? {
            if (intent == null) return null
            intent.getStringExtra(EXTRA_MINI_APP_ID)?.takeIf { it.isNotBlank() }?.let { return it }
            val data = intent.data ?: return null
            if (data.scheme != "miniapp") return null
            if (data.host != "app" && data.host != "run") return null
            return data.pathSegments.firstOrNull()?.takeIf { it.isNotBlank() }
        }
    }
}
