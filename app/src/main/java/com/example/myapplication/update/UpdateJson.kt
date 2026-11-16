// Author: PKQ32
// Created: 2026-11-16
// Description: 解析 GitHub releases/latest JSON，挑选 APK 资产

package com.example.myapplication.update

import org.json.JSONObject

object UpdateJson {

    /**
     * 解析失败或没有 APK 资产时返回 null。
     * `releases/latest` 不返回草稿与预发布版本。
     */
    fun parseRelease(json: String): UpdateInfo? {
        val obj = runCatching { JSONObject(json) }.getOrNull() ?: return null
        val tag = obj.optString("tag_name").trim()
        if (tag.isBlank()) return null
        val version = VersionComparator.parse(tag)
            ?.joinToString(".")
            ?: return null

        val assets = obj.optJSONArray("assets") ?: return null
        var apk: JSONObject? = null
        var fallback: JSONObject? = null
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            val name = asset.optString("name")
            if (!name.endsWith(".apk", ignoreCase = true)) continue
            if (fallback == null) fallback = asset
            if (name.contains("AppFactory", ignoreCase = true)) {
                apk = asset
                break
            }
        }
        val chosen = apk ?: fallback ?: return null
        val url = chosen.optString("browser_download_url").trim()
        if (url.isBlank()) return null

        return UpdateInfo(
            version = version,
            tagName = tag,
            notes = obj.optString("body").trim(),
            pageUrl = obj.optString("html_url").trim(),
            apkUrl = url,
            apkSize = chosen.optLong("size", 0L),
        )
    }
}
