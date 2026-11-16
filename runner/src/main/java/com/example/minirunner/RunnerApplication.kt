// Author: PKQ32
// Created: 2026-07-12
// Description: Runner 进程入口：注册动作处理器

package com.example.minirunner

import android.app.Application
import android.content.pm.PackageManager
import com.example.myapplication.engine.ActionExecutor
import com.example.myapplication.engine.ActionRegistry
import com.example.myapplication.platform.MiniAppConfigStore
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
import com.example.myapplication.platform.media.MediaPlaybackController

class RunnerApplication : Application() {
    lateinit var registry: ActionRegistry
        private set
    lateinit var executor: ActionExecutor
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        MediaPlaybackController.init(this)
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
        discardStaleSyncedConfig()
        // 进程启动/应用更新后恢复后台触发（AlarmManager 会随重启丢失）
        runCatching { StandaloneTriggerScheduler.resync(this) }
    }

    /**
     * 更新安装包后，包内已嵌入工厂最新配置；若旧的「同步配置」早于本次安装，
     * 应丢弃，否则新配置会被旧覆盖文件压住。不依赖 MY_PACKAGE_REPLACED 广播是否送达。
     */
    private fun discardStaleSyncedConfig() {
        val override = MiniAppConfigStore.overrideJsonFile(this)
        if (!override.exists()) return
        val installedAt = runCatching {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                .lastUpdateTime
        }.getOrDefault(0L)
        if (installedAt > override.lastModified()) {
            runCatching { MiniAppConfigStore.clearOverride(this) }
        }
    }

    companion object {
        lateinit var instance: RunnerApplication
            private set
    }
}
