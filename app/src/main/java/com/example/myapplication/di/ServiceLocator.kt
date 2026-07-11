// Author: PKQ32
// Created: 2026-07-11
// Description: 简易服务定位器，装配 Room、仓库与执行引擎

package com.example.myapplication.di

import android.app.Application
import androidx.room.Room
import com.example.myapplication.data.MiniAppRepository
import com.example.myapplication.data.RunLogRepository
import com.example.myapplication.data.db.AppDatabase
import com.example.myapplication.engine.ActionExecutor
import com.example.myapplication.engine.ActionRegistry
import com.example.myapplication.engine.Debouncer
import com.example.myapplication.platform.PlatformActionContext
import com.example.myapplication.platform.handlers.BluetoothHandler
import com.example.myapplication.platform.handlers.DelayHandler
import com.example.myapplication.platform.handlers.FlashlightHandler
import com.example.myapplication.platform.handlers.NotifyHandler
import com.example.myapplication.platform.handlers.OpenAppHandler
import com.example.myapplication.platform.handlers.PlayMusicHandler
import com.example.myapplication.platform.handlers.ScreenshotHandler
import com.example.myapplication.platform.handlers.SetBrightnessHandler
import com.example.myapplication.platform.handlers.SetRingerModeHandler
import com.example.myapplication.platform.handlers.SetVolumeHandler
import com.example.myapplication.platform.handlers.VibrateHandler
import com.example.myapplication.platform.handlers.WifiHandler

object ServiceLocator {
    lateinit var db: AppDatabase
        private set
    lateinit var miniApps: MiniAppRepository
        private set
    lateinit var runLogs: RunLogRepository
        private set
    lateinit var registry: ActionRegistry
        private set
    lateinit var executor: ActionExecutor
        private set
    val debouncer = Debouncer()

    fun init(app: Application) {
        db = Room.databaseBuilder(app, AppDatabase::class.java, "miniapp.db").build()
        miniApps = MiniAppRepository(db.miniAppDao())
        runLogs = RunLogRepository(db.runLogDao())
        registry = ActionRegistry()
        val ctx = PlatformActionContext(app)
        registry.register(DelayHandler())
        registry.register(VibrateHandler(ctx))
        registry.register(SetVolumeHandler(ctx))
        registry.register(NotifyHandler(ctx))
        registry.register(PlayMusicHandler(ctx))
        registry.register(FlashlightHandler(ctx))
        registry.register(SetBrightnessHandler(ctx))
        registry.register(OpenAppHandler(ctx))
        registry.register(WifiHandler(ctx))
        registry.register(BluetoothHandler(ctx))
        registry.register(SetRingerModeHandler(ctx))
        registry.register(ScreenshotHandler())
        executor = ActionExecutor(registry)
    }
}
