// Author: PKQ32
// Created: 2026-11-16
// Description: 导出包名/桌面名/Manifest 改写规则单测

package com.example.myapplication.export

import com.example.myapplication.domain.model.MiniApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportIdentityTest {

    private fun app(id: String, name: String = "测试") = MiniApp(
        id = id,
        name = name,
        createdAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun packageName_isStableForSameId() {
        assertEquals(
            ExportIdentity.packageNameFor(app("same-id")),
            ExportIdentity.packageNameFor(app("same-id")),
        )
    }

    @Test
    fun packageName_differsForDifferentId() {
        assertNotEquals(
            ExportIdentity.packageNameFor(app("a")),
            ExportIdentity.packageNameFor(app("b")),
        )
    }

    @Test
    fun packageName_hasExpectedPrefix() {
        val pkg = ExportIdentity.packageNameFor(app("abc"))
        assertTrue(pkg.startsWith("com.example.maf.a"))
        assertEquals("com.example.maf.a".length + 12, pkg.length)
    }

    @Test
    fun label_trimsAndFallsBack() {
        assertEquals("晨间模式", ExportIdentity.labelFor(app("1", "  晨间模式  ")))
        assertEquals("小应用", ExportIdentity.labelFor(app("1", "   ")))
    }

    @Test
    fun label_stripsNewLinesAndLimitsLength() {
        val label = ExportIdentity.labelFor(app("1", "第一行\n第二行"))
        assertEquals("第一行 第二行", label)
        assertTrue(ExportIdentity.labelFor(app("1", "x".repeat(100))).length <= 40)
    }

    @Test
    fun rewrite_keepsRunnerComponentClasses() {
        listOf(
            "com.example.minirunner.RunnerApplication",
            "com.example.minirunner.RunnerActivity",
            "com.example.minirunner.ConfigSyncActivity",
        ).forEach { assertFalse(ExportIdentity.shouldRewriteManifestValue(it)) }
    }

    @Test
    fun rewrite_keepsFixedActionAndRewritesPackage() {
        assertFalse(ExportIdentity.shouldRewriteManifestValue("com.apkfactory.action.APPLY_MINIAPP_CONFIG"))
        assertTrue(ExportIdentity.shouldRewriteManifestValue("com.example.minirunner"))
        assertTrue(ExportIdentity.shouldRewriteManifestValue("com.example.minirunner.fileprovider"))
        assertFalse(ExportIdentity.shouldRewriteManifestValue("android.intent.action.MAIN"))

        assertEquals(
            "com.example.maf.a123.fileprovider",
            ExportIdentity.rewriteManifestValue(
                "com.example.minirunner.fileprovider",
                "com.example.maf.a123",
            ),
        )
    }
}
