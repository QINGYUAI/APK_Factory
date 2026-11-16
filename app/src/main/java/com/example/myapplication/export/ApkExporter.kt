// Author: PKQ32
// Created: 2026-07-12
// Description: 将小应用配置注入 Runner 模板，改写独立包名/桌面名并重签名

package com.example.myapplication.export

import android.content.Context
import com.android.apksig.ApkSigner
import com.example.myapplication.data.JsonMappers
import com.example.myapplication.domain.model.MiniApp
import java.io.File

object ApkExporter {

    private const val TEMPLATE_ASSET = "templates/runner.apk"

    /**
     * 导出可安装 APK。
     * 每个小应用使用独立包名（com.example.maf.a…）与工厂内配置的桌面名。
     */
    fun export(context: Context, miniApp: MiniApp): File {
        val outDir = File(context.cacheDir, "apk-export").apply { mkdirs() }
        // 只清理可安全删除的中间产物；保留近期已签名包，避免打断正在进行的安装
        val staleBefore = System.currentTimeMillis() - 6 * 60 * 60 * 1000L
        outDir.listFiles()?.forEach { file ->
            if (!file.isFile) return@forEach
            val intermediate = file.name == "template-work.apk" || file.name.endsWith("-unsigned.apk")
            if (intermediate || file.lastModified() < staleBefore) {
                file.delete()
            }
        }
        val safeName = miniApp.name.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "miniapp" }
        val templateFile = File(outDir, "template-work.apk")
        val unsigned = File(outDir, "${safeName}-unsigned.apk")
        val signed = File(outDir, "${safeName}.apk")

        context.assets.open(TEMPLATE_ASSET).use { input ->
            templateFile.outputStream().use { output -> input.copyTo(output) }
        }

        val newPackage = ExportIdentity.packageNameFor(miniApp)
        val label = ExportIdentity.labelFor(miniApp)
        val versionCode = ExportVersionHelper.nextVersionCode(context, miniApp)
        val versionName = ExportVersionHelper.versionNameFor(versionCode, miniApp)
        // 将音乐/视频复制进 APK assets，避免 content URI 权限无法跨包转移
        val prepared = MediaExportHelper.prepare(context, miniApp)
        val jsonBytes = JsonMappers.miniAppToJson(prepared.miniApp).toByteArray(Charsets.UTF_8)

        ExportPatcher.patch(
            templateFile = templateFile,
            outputFile = unsigned,
            newPackage = newPackage,
            label = label,
            jsonBytes = jsonBytes,
            extraAssets = prepared.assetFiles,
            iconKey = miniApp.iconKey,
            versionCode = versionCode,
            versionName = versionName,
        )
        templateFile.delete()

        if (signed.exists()) signed.delete()
        signApk(unsigned, signed)
        unsigned.delete()
        return signed
    }

    private fun signApk(input: File, output: File) {
        val signerConfig = ExportSigning.signerConfig()
        ApkSigner.Builder(listOf(signerConfig))
            .setInputApk(input)
            .setOutputApk(output)
            .setMinSdkVersion(34)
            // minSdk 34 只需 v2/v3；关闭 v1 可避免 JarSigner 额外读取密钥信息
            .setV1SigningEnabled(false)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .build()
            .sign()
    }
}
