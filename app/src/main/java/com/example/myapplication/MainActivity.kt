// Author: PKQ32
// Created: 2026-07-11
// Description: 主 Activity：加载导航，解析 miniAppId deep link 额外参数

package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.myapplication.ui.navigation.AppNav
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 桌面快捷方式 / 外部唤起可携带 miniAppId，后续导航到运行页
        val deepLinkMiniAppId = intent?.getStringExtra(EXTRA_MINI_APP_ID)
        setContent {
            MyApplicationTheme {
                AppNav(deepLinkMiniAppId = deepLinkMiniAppId)
            }
        }
    }

    companion object {
        const val EXTRA_MINI_APP_ID = "miniAppId"
    }
}
