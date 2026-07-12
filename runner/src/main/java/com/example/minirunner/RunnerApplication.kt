// Author: PKQ32
// Created: 2026-07-12
// Description: Runner 进程入口：注册动作处理器

package com.example.minirunner

import android.app.Application
import com.example.myapplication.engine.ActionExecutor
import com.example.myapplication.engine.ActionRegistry
import com.example.myapplication.platform.PlatformActionContext
import com.example.myapplication.platform.handlers.BluetoothHandler
import com.example.myapplication.platform.handlers.DelayHandler
import com.example.myapplication.platform.handlers.FlashlightHandler
import com.example.myapplication.platform.handlers.NotifyHandler
import com.example.myapplication.platform.handlers.OpenAppHandler
import com.example.myapplication.platform.handlers.PlayMusicHandler
import com.example.myapplication.platform.handlers.PlayVideoHandler
import com.example.myapplication.platform.handlers.ScreenshotHandler
import com.example.myapplication.platform.handlers.SetBrightnessHandler
import com.example.myapplication.platform.handlers.SetRingerModeHandler
import com.example.myapplication.platform.handlers.SetVolumeHandler
import com.example.myapplication.platform.handlers.VibrateHandler
import com.example.myapplication.platform.handlers.WifiHandler

class RunnerApplication : Application() {
    lateinit var registry: ActionRegistry
        private set
    lateinit var executor: ActionExecutor
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        val ctx = PlatformActionContext(this)
        registry = ActionRegistry().apply {
            register(DelayHandler())
            register(VibrateHandler(ctx))
            register(SetVolumeHandler(ctx))
            register(NotifyHandler(ctx))
            register(PlayMusicHandler(ctx))
            register(PlayVideoHandler(ctx))
            register(FlashlightHandler(ctx))
            register(SetBrightnessHandler(ctx))
            register(OpenAppHandler(ctx))
            register(WifiHandler(ctx))
            register(BluetoothHandler(ctx))
            register(SetRingerModeHandler(ctx))
            register(ScreenshotHandler())
        }
        executor = ActionExecutor(registry)
    }

    companion object {
        lateinit var instance: RunnerApplication
            private set
    }
}
