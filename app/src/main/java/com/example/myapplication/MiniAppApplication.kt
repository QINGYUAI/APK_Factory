// Author: PKQ32
// Created: 2026-07-11
// Description: Application 入口，初始化 ServiceLocator 并同步触发器

package com.example.myapplication

import android.app.Application
import com.example.myapplication.di.ServiceLocator
import com.example.myapplication.platform.media.MediaPlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MiniAppApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        MediaPlaybackController.init(this)
        ServiceLocator.init(this)
        applicationScope.launch {
            ServiceLocator.triggerManager.resync()
        }
    }
}
