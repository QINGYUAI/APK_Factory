// Author: PKQ32
// Created: 2026-07-12
// Description: 桌面快捷方式：动态快捷方式 + 固定请求 + 国产桌面 INSTALL_SHORTCUT 兜底

package com.example.myapplication.platform

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ShortcutManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.myapplication.MainActivity
import com.example.myapplication.R

/**
 * 将小应用固定到桌面。
 *
 * 常见失败原因与对策：
 * 1. Intent 带 NEW_TASK 等 flags → 部分启动器拒绝固定
 * 2. 未先注册动态快捷方式 → 部分机型 requestPin 无效
 * 3. 国产桌面关闭「创建快捷方式」权限 → 需引导用户到系统设置
 * 4. UI 忽略返回值 → 用户感觉「点了没反应」
 */
object ShortcutHelper {

    private const val SHORTCUT_ID_PREFIX = "miniapp-"
    private const val ACTION_PIN_RESULT = "com.example.myapplication.action.PIN_SHORTCUT_RESULT"

    enum class PinResult {
        /** 已弹出系统确认框（或已发起固定） */
        REQUESTED,

        /** 已通过旧版广播尝试安装（多见于国产桌面） */
        LEGACY_SENT,

        /** 启动器不支持固定快捷方式 */
        UNSUPPORTED,

        /** 调用失败 */
        FAILED,
    }

    /**
     * 请求将指定小应用添加到桌面，并用 Toast 给出中文反馈。
     */
    fun requestPinShortcut(context: Context, miniAppId: String, label: String): PinResult {
        val activity = context.findActivity() ?: context
        val displayLabel = label.ifBlank { "小应用" }.take(40)
        val shortcutId = SHORTCUT_ID_PREFIX + miniAppId
        val launchIntent = buildLaunchIntent(activity, miniAppId)
        val shortcut = ShortcutInfoCompat.Builder(activity, shortcutId)
            .setShortLabel(displayLabel)
            .setLongLabel(displayLabel)
            .setIcon(IconCompat.createWithResource(activity, R.mipmap.ic_launcher))
            .setIntent(launchIntent)
            .build()

        // 先注册为动态快捷方式，提高部分机型 pin 成功率
        runCatching {
            ShortcutManagerCompat.pushDynamicShortcut(activity, shortcut)
        }

        val result = when {
            ShortcutManagerCompat.isRequestPinShortcutSupported(activity) -> {
                val success = try {
                    val callback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        createPinResultCallback(activity, displayLabel)
                    } else {
                        null
                    }
                    ShortcutManagerCompat.requestPinShortcut(activity, shortcut, callback)
                } catch (e: Exception) {
                    false
                }
                if (success) {
                    PinResult.REQUESTED
                } else {
                    // 官方 API 失败时再试旧广播
                    if (sendLegacyInstallShortcut(activity, launchIntent, displayLabel)) {
                        PinResult.LEGACY_SENT
                    } else {
                        PinResult.FAILED
                    }
                }
            }
            sendLegacyInstallShortcut(activity, launchIntent, displayLabel) -> PinResult.LEGACY_SENT
            else -> PinResult.UNSUPPORTED
        }

        showFeedback(activity, result, displayLabel)
        return result
    }

    /**
     * 快捷方式启动 Intent：不要加 NEW_TASK/CLEAR_TOP（由启动器处理）。
     */
    private fun buildLaunchIntent(context: Context, miniAppId: String): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("miniapp://app/$miniAppId")
            putExtra(MainActivity.EXTRA_MINI_APP_ID, miniAppId)
            // 显式组件，避免部分启动器无法解析自定义 scheme
            setClass(context, MainActivity::class.java)
        }
    }

    private fun createPinResultCallback(context: Context, label: String): android.content.IntentSender {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                Toast.makeText(ctx, "「$label」已添加到桌面", Toast.LENGTH_SHORT).show()
                try {
                    ctx.unregisterReceiver(this)
                } catch (_: Exception) {
                }
            }
        }
        val filter = IntentFilter(ACTION_PIN_RESULT)
        ContextCompat.registerReceiver(
            context.applicationContext,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        val broadcast = Intent(ACTION_PIN_RESULT).setPackage(context.packageName)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getBroadcast(context, label.hashCode(), broadcast, flags).intentSender
    }

    /**
     * 旧版 INSTALL_SHORTCUT：对小米/华为等仍可能有效；Android 8+ 原生桌面会忽略。
     */
    @Suppress("DEPRECATION")
    private fun sendLegacyInstallShortcut(
        context: Context,
        launchIntent: Intent,
        label: String,
    ): Boolean {
        return try {
            val shortcutIntent = Intent("com.android.launcher.action.INSTALL_SHORTCUT").apply {
                putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent)
                putExtra(Intent.EXTRA_SHORTCUT_NAME, label)
                putExtra(
                    Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(context, R.mipmap.ic_launcher),
                )
                putExtra("duplicate", false)
            }
            context.sendBroadcast(shortcutIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun showFeedback(context: Context, result: PinResult, label: String) {
        val message = when (result) {
            PinResult.REQUESTED ->
                "请在系统弹窗中确认添加「$label」。若无弹窗，请到系统设置允许本应用「创建桌面快捷方式」"
            PinResult.LEGACY_SENT ->
                "已请求添加「$label」。若桌面未出现，请在系统设置中允许创建快捷方式"
            PinResult.UNSUPPORTED ->
                "当前桌面不支持固定快捷方式，请使用列表中的「控制台」入口"
            PinResult.FAILED ->
                "添加失败。请检查系统是否禁止本应用创建桌面快捷方式"
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun Context.findActivity(): Activity? {
        var current: Context = this
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }

    /** 是否已固定到桌面（API 26+，仅供调试/提示）。 */
    fun isPinned(context: Context, miniAppId: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val sm = context.getSystemService(ShortcutManager::class.java) ?: return false
        val id = SHORTCUT_ID_PREFIX + miniAppId
        return sm.pinnedShortcuts.any { it.id == id }
    }

    /**
     * 小应用被删除后清理快捷方式：
     * - 移除动态快捷方式；
     * - 已固定到桌面的快捷方式置灰并提示，避免点击后进入「未找到」。
     */
    fun removeShortcut(context: Context, miniAppId: String) {
        val id = SHORTCUT_ID_PREFIX + miniAppId
        runCatching { ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(id)) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                context.getSystemService(ShortcutManager::class.java)
                    ?.disableShortcuts(listOf(id), "小应用已删除")
            }
        }
    }
}
