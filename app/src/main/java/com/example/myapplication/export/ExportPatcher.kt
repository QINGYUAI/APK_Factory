// Author: PKQ32
// Created: 2026-07-12
// Description: 对 Runner 模板 APK 改写包名、桌面名并注入 miniapp.json

package com.example.myapplication.export

import com.reandroid.apk.ApkModule
import com.reandroid.archive.ByteInputSource
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import com.reandroid.arsc.chunk.xml.ResXmlAttribute
import com.reandroid.arsc.value.ValueType
import java.io.File

internal object ExportPatcher {

    private const val EMBEDDED_JSON_PATH = "assets/miniapp.json"
    private const val COMPONENT_APPLICATION =
        "${ExportIdentity.TEMPLATE_PACKAGE}.RunnerApplication"
    private const val COMPONENT_ACTIVITY =
        "${ExportIdentity.TEMPLATE_PACKAGE}.RunnerActivity"

    fun patch(
        templateFile: File,
        outputFile: File,
        newPackage: String,
        label: String,
        jsonBytes: ByteArray,
    ) {
        val module = ApkModule.loadApkFile(templateFile)
        try {
            module.removeDir("META-INF")

            val manifest = module.androidManifest
                ?: error("模板 APK 缺少 AndroidManifest.xml")

            manifest.ensureFullClassNames()
            rewritePackageBoundStrings(manifest, newPackage)
            manifest.packageName = newPackage
            manifest.applicationClassName = COMPONENT_APPLICATION
            manifest.mainActivityClassName = COMPONENT_ACTIVITY
            manifest.setApplicationLabel(label)
            rewriteAppNameResources(module, label)

            module.removeInputSource(EMBEDDED_JSON_PATH)
            module.add(ByteInputSource(jsonBytes, EMBEDDED_JSON_PATH))

            if (outputFile.exists()) outputFile.delete()
            module.writeApk(outputFile)
        } finally {
            module.close()
        }
    }

    private fun rewritePackageBoundStrings(manifest: AndroidManifestBlock, newPackage: String) {
        val attrs = manifest.recursiveAttributes()
        while (attrs.hasNext()) {
            val attr: ResXmlAttribute = attrs.next()
            if (attr.valueType != ValueType.STRING) continue
            val raw = attr.valueAsString ?: continue
            if (!ExportIdentity.shouldRewriteManifestValue(raw)) continue
            attr.setValueAsString(ExportIdentity.rewriteManifestValue(raw, newPackage))
        }
    }

    private fun rewriteAppNameResources(module: ApkModule, label: String) {
        val table = module.tableBlock ?: return
        for (pkg in table.listPackages()) {
            val entries = pkg.getEntries("string", "app_name")
            while (entries.hasNext()) {
                entries.next().setValueAsString(label)
            }
        }
    }
}
