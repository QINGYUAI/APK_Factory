// Author: PKQ32
// Created: 2026-07-12
// Description: 将小应用配置注入 Runner 模板，改写独立包名/桌面名并重签名

package com.example.myapplication.export

import android.content.Context
import com.android.apksig.ApkSigner
import com.example.myapplication.data.JsonMappers
import com.example.myapplication.domain.model.MiniApp
import java.io.File
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate

object ApkExporter {

    private const val TEMPLATE_ASSET = "templates/runner.apk"
    private const val KEYSTORE_ASSET = "templates/export.p12"
    private const val KEY_ALIAS = "miniexport"
    private const val KEY_PASS = "android"

    /**
     * 导出可安装 APK。
     * 每个小应用使用独立包名（com.example.maf.a…）与工厂内配置的桌面名。
     */
    fun export(context: Context, miniApp: MiniApp): File {
        val outDir = File(context.cacheDir, "apk-export").apply { mkdirs() }
        val safeName = miniApp.name.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "miniapp" }
        val templateFile = File(outDir, "template-work.apk")
        val unsigned = File(outDir, "${safeName}-unsigned.apk")
        val signed = File(outDir, "${safeName}.apk")

        context.assets.open(TEMPLATE_ASSET).use { input ->
            templateFile.outputStream().use { output -> input.copyTo(output) }
        }

        val newPackage = ExportIdentity.packageNameFor(miniApp)
        val label = ExportIdentity.labelFor(miniApp)
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
        )
        templateFile.delete()

        signApk(context, unsigned, signed)
        unsigned.delete()
        return signed
    }

    private fun signApk(context: Context, input: File, output: File) {
        // Android 默认 KeyStore 类型与桌面 JKS 不兼容，固定使用 PKCS12
        val keyStore = KeyStore.getInstance("PKCS12")
        context.assets.open(KEYSTORE_ASSET).use { stream ->
            keyStore.load(stream, KEY_PASS.toCharArray())
        }
        val key = keyStore.getKey(KEY_ALIAS, KEY_PASS.toCharArray()) as PrivateKey
        val cert = keyStore.getCertificate(KEY_ALIAS) as X509Certificate
        val signerConfig = ApkSigner.SignerConfig.Builder("miniexport", key, listOf(cert)).build()
        ApkSigner.Builder(listOf(signerConfig))
            .setInputApk(input)
            .setOutputApk(output)
            .setMinSdkVersion(34)
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .build()
            .sign()
    }
}
