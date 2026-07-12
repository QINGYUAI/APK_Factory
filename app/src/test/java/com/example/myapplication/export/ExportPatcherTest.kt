// Author: PKQ32
// Created: 2026-07-12
// Description: 验证导出补丁会改写独立包名与桌面名，并保留 Runner 组件类名

package com.example.myapplication.export

import com.example.myapplication.domain.model.MiniApp
import com.reandroid.apk.ApkModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExportPatcherTest {

    @Test
    fun patch_rewritesPackageAndLabel_keepsComponentClasses() {
        val template = File("src/main/assets/templates/runner.apk")
        assertTrue("缺少模板 APK，请先 assemble runner", template.exists())

        val miniApp = MiniApp(
            id = "11111111-2222-3333-4444-555555555555",
            name = "晨间模式",
            createdAt = 1L,
            updatedAt = 1L,
        )
        val newPackage = ExportIdentity.packageNameFor(miniApp)
        val label = ExportIdentity.labelFor(miniApp)
        val out = File("build/tmp-export-test/patched.apk")
        out.parentFile?.mkdirs()

        ExportPatcher.patch(
            templateFile = template,
            outputFile = out,
            newPackage = newPackage,
            label = label,
            jsonBytes = """{"id":"${miniApp.id}","name":"${miniApp.name}"}""".toByteArray(),
        )

        val module = ApkModule.loadApkFile(out)
        try {
            val manifest = module.androidManifest!!
            assertEquals(newPackage, manifest.packageName)
            assertEquals(
                "${ExportIdentity.TEMPLATE_PACKAGE}.RunnerApplication",
                manifest.applicationClassName,
            )
            assertEquals(
                "${ExportIdentity.TEMPLATE_PACKAGE}.RunnerActivity",
                manifest.mainActivityClassName,
            )
            assertEquals(label, manifest.applicationLabelString)
            assertFalse(manifest.packageName == ExportIdentity.TEMPLATE_PACKAGE)
            assertTrue(newPackage.startsWith("com.example.maf.a"))
        } finally {
            module.close()
        }
    }

    @Test
    fun differentMiniApps_getDifferentPackages() {
        val a = MiniApp(id = "aaa", name = "A", createdAt = 1, updatedAt = 1)
        val b = MiniApp(id = "bbb", name = "B", createdAt = 1, updatedAt = 1)
        assertTrue(
            ExportIdentity.packageNameFor(a) != ExportIdentity.packageNameFor(b),
        )
    }
}
