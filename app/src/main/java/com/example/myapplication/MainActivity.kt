// Author: PKQ32
// Created: 2026-07-11
// Description: 主 Activity：导航、deep link；冷启动与回到前台时触发 hostOpened

package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.myapplication.di.ServiceLocator
import com.example.myapplication.domain.model.TriggerTypes
import com.example.myapplication.ui.navigation.AppNav
import com.example.myapplication.ui.navigation.DeepLinkTarget
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /** 用状态驱动导航，onNewIntent 时不再重建整棵 Compose 树。 */
    private var deepLinkTarget by mutableStateOf<DeepLinkTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 配置变更重建时交给导航自身恢复，避免重复压栈
        if (savedInstanceState == null) {
            deepLinkTarget = resolveDeepLinkTarget(intent)
        }
        setContent {
            MyApplicationTheme {
                AppNav(deepLinkTarget = deepLinkTarget)
            }
        }
        // 冷启动与回到前台（防抖由 MiniAppRunner 处理）
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                ServiceLocator.updateManager.maybeAutoCheck()
                maybeRunHostOpened()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // 同一个 id 再次点击桌面快捷方式也要能重新导航，因此带一次性 token
        deepLinkTarget = resolveDeepLinkTarget(intent) ?: deepLinkTarget
    }

    private fun resolveDeepLinkTarget(intent: Intent?): DeepLinkTarget? {
        val id = resolveMiniAppId(intent) ?: return null
        return DeepLinkTarget(miniAppId = id, token = System.nanoTime())
    }

    private suspend fun maybeRunHostOpened() {
        val app = ServiceLocator.miniApps.getHostOpenApp() ?: return
        ServiceLocator.runner.runQueued(app.id, TriggerTypes.HOST_OPENED)
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
