// Author: PKQ32
// Created: 2026-07-12
// Description: 向已安装独立小应用推送配置 zip（FileProvider + 显式 Intent）

package com.example.myapplication.export

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import com.example.myapplication.domain.model.MiniApp
import com.example.myapplication.platform.ConfigSyncContract
import java.io.File

object ConfigSyncPusher {

    sealed class Result {
        data object Success : Result()
        data class Failed(val message: String) : Result()
    }

    fun sync(context: Context, miniApp: MiniApp): Result {
        val packageName = ExportIdentity.packageNameFor(miniApp)
        if (!ExportVersionHelper.isInstalled(context, miniApp)) {
            return Result.Failed("尚未安装独立应用，请先「添加到桌面」")
        }
        val zip = runCatching { ConfigSyncPackager.buildZip(context, miniApp) }
            .getOrElse { return Result.Failed(it.message ?: "打包配置失败") }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            zip,
        )
        val intent = Intent(ConfigSyncContract.ACTION_APPLY_CONFIG).apply {
            setPackage(packageName)
            setDataAndType(uri, ConfigSyncContract.MIME_ZIP)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(ConfigSyncContract.EXTRA_MINI_APP_ID, miniApp.id)
            putExtra(ConfigSyncContract.EXTRA_MINI_APP_NAME, miniApp.name)
        }

        val canHandle = context.packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .isNotEmpty()
        if (!canHandle) {
            return Result.Failed("当前独立包过旧，请先点「更新应用」一次以支持配置同步")
        }

        return try {
            context.grantUriPermission(
                packageName,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            context.startActivity(intent)
            Result.Success
        } catch (e: Exception) {
            Result.Failed(e.message ?: "无法打开独立应用接收配置")
        }
    }
}
