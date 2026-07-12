// Author: PKQ32
// Created: 2026-07-12
// Description: 预设桌面图标包：iconKey → Runner 模板 mipmap 资源名

package com.example.myapplication.export

/**
 * 与 runner 中 `mipmap/ic_app_*` 资源对应。
 * 导出时按 iconKey 改写 manifest 的 android:icon / roundIcon。
 */
object LauncherIconPack {

    /** 返回要设置的 mipmap 名（不含类型）。 */
    fun mipmapName(iconKey: String): String {
        val key = iconKey.trim().ifBlank { "default" }
        val safe = if (key in KNOWN_KEYS) key else "default"
        return "ic_app_$safe"
    }

    val KNOWN_KEYS: Set<String> = setOf(
        "default",
        "bolt",
        "rocket",
        "star",
        "sun",
        "music",
        "notify",
        "home",
        "heart",
        "clock",
        "lab",
    )
}
