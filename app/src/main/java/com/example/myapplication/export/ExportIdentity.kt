// Author: PKQ32
// Created: 2026-07-12
// Description: 按小应用生成唯一包名与桌面显示名

package com.example.myapplication.export

import com.example.myapplication.domain.model.MiniApp
import java.security.MessageDigest

object ExportIdentity {

    const val TEMPLATE_PACKAGE = "com.example.minirunner"

    /** 组件类名仍留在模板命名空间，不能随 applicationId 改写。 */
    private val KEEP_COMPONENT_NAMES = setOf(
        "$TEMPLATE_PACKAGE.RunnerApplication",
        "$TEMPLATE_PACKAGE.RunnerActivity",
        "$TEMPLATE_PACKAGE.ConfigSyncActivity",
    )

    /**
     * 唯一 applicationId：同一小应用 id 稳定，不同小应用互不覆盖。
     * 形如 com.example.maf.a + 12 位十六进制。
     */
    fun packageNameFor(miniApp: MiniApp): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(miniApp.id.toByteArray(Charsets.UTF_8))
        val hex = digest.take(6).joinToString("") { b -> "%02x".format(b) }
        return "com.example.maf.a$hex"
    }

    /** 桌面显示名（来自应用工厂配置）。 */
    fun labelFor(miniApp: MiniApp): String {
        return miniApp.name
            .trim()
            .replace(Regex("[\\r\\n]+"), " ")
            .take(40)
            .ifBlank { "小应用" }
    }

    fun shouldRewriteManifestValue(value: String): Boolean {
        if (value in KEEP_COMPONENT_NAMES) return false
        // 固定跨包 action，禁止随 applicationId 改写
        if (value.startsWith("com.apkfactory.")) return false
        if (value == TEMPLATE_PACKAGE) return true
        return value.startsWith("$TEMPLATE_PACKAGE.")
    }

    fun rewriteManifestValue(value: String, newPackage: String): String {
        if (value == TEMPLATE_PACKAGE) return newPackage
        if (value.startsWith("$TEMPLATE_PACKAGE.")) {
            return newPackage + value.removePrefix(TEMPLATE_PACKAGE)
        }
        return value
    }
}
