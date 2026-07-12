// Author: PKQ32
// Created: 2026-07-12
// Description: 将小应用配置注入 Runner 模板 APK 并重签名，生成可安装包

package com.example.myapplication.export

import android.content.Context
import com.android.apksig.ApkSigner
import com.example.myapplication.data.JsonMappers
import com.example.myapplication.domain.model.MiniApp
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ApkExporter {

    private const val TEMPLATE_ASSET = "templates/runner.apk"
    private const val KEYSTORE_ASSET = "templates/export.jks"
    private const val KEY_ALIAS = "miniexport"
    private const val KEY_PASS = "android"
    private const val EMBEDDED_JSON_PATH = "assets/miniapp.json"

    /**
     * 导出可安装 APK 到 cache 目录。
     * 注意：包名固定为 com.example.minirunner，重复安装会覆盖上一版。
     */
    fun export(context: Context, miniApp: MiniApp): File {
        val outDir = File(context.cacheDir, "apk-export").apply { mkdirs() }
        val safeName = miniApp.name.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "miniapp" }
        val unsigned = File(outDir, "${safeName}-unsigned.apk")
        val signed = File(outDir, "${safeName}.apk")

        val templateBytes = context.assets.open(TEMPLATE_ASSET).use { it.readBytes() }
        val jsonBytes = JsonMappers.miniAppToJson(miniApp).toByteArray(Charsets.UTF_8)

        rewriteApkWithAsset(templateBytes, EMBEDDED_JSON_PATH, jsonBytes, unsigned)
        signApk(context, unsigned, signed)
        unsigned.delete()
        return signed
    }

    private fun rewriteApkWithAsset(
        templateApk: ByteArray,
        assetPath: String,
        assetBytes: ByteArray,
        output: File,
    ) {
        ZipInputStream(BufferedInputStream(ByteArrayInputStream(templateApk))).use { zis ->
            ZipOutputStream(BufferedOutputStream(FileOutputStream(output))).use { zos ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    // 去掉旧签名与旧配置，其余条目按解压内容重新压缩写入
                    if (!name.startsWith("META-INF/") && name != assetPath) {
                        zos.putNextEntry(ZipEntry(name))
                        zis.copyTo(zos)
                        zos.closeEntry()
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
                zos.putNextEntry(ZipEntry(assetPath))
                zos.write(assetBytes)
                zos.closeEntry()
            }
        }
    }

    private fun signApk(context: Context, input: File, output: File) {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
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
