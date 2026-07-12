// Author: PKQ32
// Created: 2026-07-12
// Description: 接收工厂推送的配置 zip，写入 synced_config 覆盖 assets

package com.example.minirunner

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.example.myapplication.platform.ConfigSyncContract
import com.example.myapplication.platform.MiniAppConfigStore
import java.io.BufferedInputStream
import java.io.File
import java.util.zip.ZipInputStream

/**
 * 透明 Activity：解压配置后立即结束。
 * 类名保留在 minirunner 包，导出改包名时不改写组件类。
 */
class ConfigSyncActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent?.data
        if (uri == null) {
            finishWithToast("未收到配置文件")
            return
        }
        val ok = runCatching { applyZip(uri) }
        if (ok.isSuccess) {
            val name = intent?.getStringExtra(ConfigSyncContract.EXTRA_MINI_APP_NAME).orEmpty()
            finishWithToast(
                if (name.isBlank()) "配置已同步，下次打开生效"
                else "「$name」配置已同步，下次打开生效",
            )
        } else {
            finishWithToast("同步失败：${ok.exceptionOrNull()?.message ?: "未知错误"}")
        }
    }

    private fun applyZip(uri: Uri) {
        val dest = MiniAppConfigStore.configDir(this)
        if (dest.exists()) dest.deleteRecursively()
        dest.mkdirs()

        contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(BufferedInputStream(input)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name.trimStart('/', '\\')
                    if (name.isBlank() || name.contains("..")) {
                        zis.closeEntry()
                        entry = zis.nextEntry
                        continue
                    }
                    val outFile = File(dest, name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { output -> zis.copyTo(output) }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } ?: error("无法读取配置包")

        val json = MiniAppConfigStore.overrideJsonFile(this)
        if (!json.exists() || json.length() <= 0L) {
            dest.deleteRecursively()
            error("配置包缺少 miniapp.json")
        }
    }

    private fun finishWithToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }
}
