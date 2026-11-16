// Author: PKQ32
// Created: 2026-11-16
// Description: 导出签名密钥改为本机 AndroidKeyStore 生成，不再在 APK 内分发通用私钥

package com.example.myapplication.export

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.android.apksig.ApkSigner
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.security.auth.x500.X500Principal

/**
 * 每个安装实例独立签名：
 * - 私钥生成在 AndroidKeyStore，不可导出；
 * - 导出包更新必须来自同一台设备上的同一份应用数据，卸载工厂后无法再覆盖安装；
 * - 相比随包 p12，避免了「任何人拿到工厂 APK 就能伪造所有导出包更新」。
 */
object ExportSigning {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "apk_factory_export_signing"
    private const val SIGNER_NAME = "apkfactory"

    fun signerConfig(): ApkSigner.SignerConfig {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateKey()
        }
        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry
            ?: error("本机签名密钥不可用，请重启应用后重试")
        val certificate = entry.certificate as? X509Certificate
            ?: error("本机签名证书不可用，无法导出安装包")
        return ApkSigner.SignerConfig.Builder(
            SIGNER_NAME,
            entry.privateKey,
            listOf(certificate),
        ).build()
    }

    private fun generateKey() {
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
        )
            .setKeySize(2048)
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .setCertificateSubject(X500Principal("CN=APK Factory Export"))
            .setCertificateSerialNumber(BigInteger.valueOf(System.currentTimeMillis()))
            .build()
        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE).apply {
            initialize(spec)
            generateKeyPair()
        }
    }
}
