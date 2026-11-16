// Author: PKQ32
// Created: 2026-07-12
// Description: 将小应用配置打成 zip，供已安装独立包热更新（不重装）

package com.example.myapplication.export

import android.content.Context
import com.example.myapplication.data.JsonMappers
import com.example.myapplication.domain.model.MiniApp
import com.example.myapplication.platform.MediaUriHelper
import com.example.myapplication.platform.MiniAppConfigStore
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ConfigSyncPackager {

    fun buildZip(context: Context, miniApp: MiniApp): File {
        val prepared = MediaExportHelper.prepare(context, miniApp)
        val steps = prepared.miniApp.steps.map { step ->
            val path = step.params["path"] ?: return@map step
            if (!MediaUriHelper.isAssetPath(path)) return@map step
            val syncPath = MediaUriHelper.SYNC_PREFIX + MediaUriHelper.assetPathOf(path)
            step.copy(params = step.params + ("path" to syncPath) + ("uri" to ""))
        }
        val payload = prepared.miniApp.copy(steps = steps)
        val jsonBytes = JsonMappers.miniAppToJson(payload).toByteArray(Charsets.UTF_8)

        val outDir = File(context.cacheDir, "config-sync").apply { mkdirs() }
        val zipFile = File(outDir, "${miniApp.id}.zip")
        if (zipFile.exists()) zipFile.delete()

        ZipOutputStream(zipFile.outputStream().buffered()).use { zos ->
            zos.putNextEntry(ZipEntry(MiniAppConfigStore.JSON_NAME))
            zos.write(jsonBytes)
            zos.closeEntry()
            for (asset in prepared.assetFiles) {
                val entryName = asset.apkPath.removePrefix("assets/").trimStart('/')
                if (entryName.isBlank()) continue
                zos.putNextEntry(ZipEntry(entryName))
                asset.file.inputStream().buffered().use { input -> input.copyTo(zos) }
                zos.closeEntry()
            }
        }
        return zipFile
    }
}
