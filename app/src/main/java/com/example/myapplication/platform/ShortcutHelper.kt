// Author: PKQ32
// Created: 2026-07-11
// Description: 请求将小应用固定为桌面快捷方式，点击后经 deep link / extras 打开运行页

package com.example.myapplication.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.myapplication.MainActivity
import com.example.myapplication.R

/**
 * 桌面快捷方式：通过 [ShortcutManagerCompat.requestPinShortcut] 请求固定，
 * Intent 携带 [MainActivity.EXTRA_MINI_APP_ID] 与 `miniapp://run/{id}`。
 */
object ShortcutHelper {

    private const val SHORTCUT_ID_PREFIX = "miniapp-"

    /**
     * 请求将指定小应用添加到桌面。
     * @return 是否已向系统发起固定请求（不代表用户已确认）
     */
    fun requestPinShortcut(context: Context, miniAppId: String, label: String): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            return false
        }
        val shortcutIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("miniapp://run/$miniAppId")
            putExtra(MainActivity.EXTRA_MINI_APP_ID, miniAppId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val displayLabel = label.ifBlank { "小应用" }.take(40)
        val shortcut = ShortcutInfoCompat.Builder(context, SHORTCUT_ID_PREFIX + miniAppId)
            .setShortLabel(displayLabel)
            .setLongLabel(displayLabel)
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(shortcutIntent)
            .build()
        return ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }
}
