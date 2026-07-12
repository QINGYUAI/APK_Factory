// Author: PKQ32
// Created: 2026-07-12
// Description: Runner 私有目录覆盖配置：优先于 APK assets 中的 miniapp.json

package com.example.myapplication.platform

import android.content.Context
import com.example.myapplication.data.JsonMappers
import com.example.myapplication.domain.model.MiniApp
import java.io.File

object MiniAppConfigStore {

    const val DIR_NAME = "synced_config"
    const val JSON_NAME = "miniapp.json"
    const val ASSET_NAME = "miniapp.json"

    fun configDir(context: Context): File =
        File(context.filesDir, DIR_NAME)

    fun overrideJsonFile(context: Context): File =
        File(configDir(context), JSON_NAME)

    fun hasOverride(context: Context): Boolean {
        val f = overrideJsonFile(context)
        return f.exists() && f.isFile && f.length() > 0L
    }

    fun load(context: Context): MiniApp {
        val override = overrideJsonFile(context)
        if (override.exists() && override.isFile && override.length() > 0L) {
            return JsonMappers.miniAppFromJson(override.readText(Charsets.UTF_8))
        }
        return context.assets.open(ASSET_NAME).bufferedReader().use { reader ->
            JsonMappers.miniAppFromJson(reader.readText())
        }
    }

    /** 独立小应用内一键关闭：本地覆盖为停用，避免下次自动执行。 */
    fun disableLocally(context: Context, app: MiniApp) {
        val updated = app.copy(
            enabled = false,
            updatedAt = System.currentTimeMillis(),
        )
        configDir(context).mkdirs()
        overrideJsonFile(context).writeText(JsonMappers.miniAppToJson(updated), Charsets.UTF_8)
    }

    fun clearOverride(context: Context) {
        configDir(context).deleteRecursively()
    }
}
