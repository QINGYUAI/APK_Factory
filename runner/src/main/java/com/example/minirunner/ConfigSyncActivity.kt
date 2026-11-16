// Author: PKQ32
// Created: 2026-07-12
// Description: 接收工厂推送的配置 zip，写入 synced_config 覆盖 assets

package com.example.minirunner

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.example.myapplication.data.JsonMappers
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
            // 触发器可能随配置变化：立刻按新配置重排，并允许一次性触发器按新时间重新布防
            runCatching { StandaloneTriggerScheduler.resync(this, resetOnceFired = true) }
            val name = intent?.getStringExtra(ConfigSyncContract.EXTRA_MINI_APP_NAME).orEmpty()
            finishWithToast(
                if (name.isBlank()) "配置已同步，后台触发已更新"
                else "「$name」配置已同步，后台触发已更新",
            )
        } else {
            finishWithToast("同步失败：${ok.exceptionOrNull()?.message ?: "未知错误"}")
        }
    }

    private fun applyZip(uri: Uri) {
        val dest = MiniAppConfigStore.configDir(this)
        // 先解压到暂存目录并校验，成功后再原子替换，避免半包配置把可用配置覆盖坏
        val staging = File(filesDir, MiniAppConfigStore.DIR_NAME + ".tmp")
        staging.deleteRecursively()
        if (!staging.mkdirs()) error("无法创建配置目录")

        try {
            var total = 0L
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
                        val outFile = File(staging, name)
                        val root = staging.canonicalPath + File.separator
                        if (!outFile.canonicalPath.startsWith(root)) {
                            zis.closeEntry()
                            entry = zis.nextEntry
                            continue
                        }
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            outFile.outputStream().use { output ->
                                total += zis.copyTo(output)
                            }
                            if (total > MAX_CONFIG_BYTES) error("配置包过大，已拒绝同步")
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            } ?: error("无法读取配置包")

            val json = File(staging, MiniAppConfigStore.JSON_NAME)
            if (!json.exists() || json.length() <= 0L) {
                error("配置包缺少 miniapp.json")
            }
            runCatching { JsonMappers.miniAppFromJson(json.readText(Charsets.UTF_8)) }
                .getOrElse { error("配置内容无效：${it.message ?: "无法解析"}") }

            if (dest.exists() && !dest.deleteRecursively()) {
                error("无法清理旧配置")
            }
            if (!staging.renameTo(dest)) {
                // 极少数文件系统不支持 rename：退回拷贝
                if (!staging.copyRecursively(dest, overwrite = true)) error("无法写入配置文件")
                staging.deleteRecursively()
            }
        } catch (e: Exception) {
            staging.deleteRecursively()
            throw e
        }
    }

    private fun finishWithToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }

    companion object {
        /** 配置包体积上限，防止异常/恶意包撑爆存储。 */
        private const val MAX_CONFIG_BYTES = 512L * 1024 * 1024
    }
}
