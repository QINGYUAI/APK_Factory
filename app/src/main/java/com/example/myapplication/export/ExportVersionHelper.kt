// Author: PKQ32
// Created: 2026-07-12
// Description: 导出版本号：同一小应用包名稳定，每次导出递增 versionCode 以覆盖更新

package com.example.myapplication.export

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.myapplication.domain.model.MiniApp
import kotlin.math.max

object ExportVersionHelper {

    private const val PREFS = "export_versions"

    /**
     * 下一版 versionCode：取「已安装版本 / 本地记录 / updatedAt」最大值 + 1，
     * 保证同一小应用重新导出时系统走「更新」而非新装。
     */
    fun nextVersionCode(context: Context, miniApp: MiniApp): Int {
        val packageName = ExportIdentity.packageNameFor(miniApp)
        val installed = installedVersionCode(context, packageName)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val local = prefs.getInt(miniApp.id, 0)
        val fromUpdatedAt = (miniApp.updatedAt / 1000L).toInt().coerceAtLeast(1)
        val next = max(max(installed, local), fromUpdatedAt - 1) + 1
        prefs.edit().putInt(miniApp.id, next).apply()
        return next
    }

    fun versionNameFor(versionCode: Int, miniApp: MiniApp): String {
        val label = miniApp.name.trim().ifBlank { "小应用" }
        return "1.0.$versionCode"
    }

    fun isInstalled(context: Context, miniApp: MiniApp): Boolean {
        return installedVersionCode(context, ExportIdentity.packageNameFor(miniApp)) > 0
    }

    private fun installedVersionCode(context: Context, packageName: String): Int {
        return try {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                info.versionCode
            }
        } catch (_: PackageManager.NameNotFoundException) {
            0
        }
    }
}
