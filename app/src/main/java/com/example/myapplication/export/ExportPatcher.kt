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
        /** 额外 assets，key 为 APK 内路径如 assets/media/xxx.mp3 */
        extraAssets: Map<String, ByteArray> = emptyMap(),
        iconKey: String = "default",
        versionCode: Int = 1,
        versionName: String = "1.0",
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
            manifest.setVersionCode(versionCode)
            manifest.setVersionName(versionName)
            rewriteAppNameResources(module, label)
            applyLauncherIcon(module, manifest, iconKey)

            module.removeInputSource(EMBEDDED_JSON_PATH)
            module.add(ByteInputSource(jsonBytes, EMBEDDED_JSON_PATH))

            for ((path, bytes) in extraAssets) {
                module.removeInputSource(path)
                // 媒体必须未压缩，否则 MediaPlayer.openFd / 部分机型无法直接播放
                val source = ByteInputSource(bytes, path).apply {
                    setUncompressed(true)
                }
                module.add(source)
            }

            if (outputFile.exists()) outputFile.delete()
            module.writeApk(outputFile)
        } finally {
            module.close()
        }
    }

    /**
     * 将 application / roundIcon 指向预设 mipmap（如 ic_app_music）。
     */
    private fun applyLauncherIcon(
        module: ApkModule,
        manifest: AndroidManifestBlock,
        iconKey: String,
    ) {
        val name = LauncherIconPack.mipmapName(iconKey)
        val table = module.tableBlock ?: return
        var iconId = 0
        var roundId = 0
        for (pkg in table.listPackages()) {
            val iconEntries = pkg.getEntries("mipmap", name)
            if (iconEntries.hasNext()) {
                iconId = iconEntries.next().resourceId
            }
            val roundEntries = pkg.getEntries("mipmap", "${name}_round")
            if (roundEntries.hasNext()) {
                roundId = roundEntries.next().resourceId
            }
        }
        if (iconId != 0) {
            manifest.setIconResourceId(iconId)
        }
        if (roundId != 0) {
            setRoundIconResourceId(manifest, roundId)
        }
    }

    /** android.R.attr.roundIcon */
    private const val ATTR_ROUND_ICON = 0x0101052c

    /** ARSCLib 部分版本无 setRoundIconResourceId，退化为写属性。 */
    private fun setRoundIconResourceId(manifest: AndroidManifestBlock, resourceId: Int) {
        runCatching {
            val method = manifest.javaClass.getMethod("setRoundIconResourceId", Int::class.javaPrimitiveType)
            method.invoke(manifest, resourceId)
            return
        }
        val app = runCatching { manifest.javaClass.getMethod("getApplicationElement").invoke(manifest) }.getOrNull()
            ?: runCatching {
                manifest.javaClass.getMethod("getOrCreateApplicationElement").invoke(manifest)
            }.getOrNull()
            ?: return
        runCatching {
            val getOrCreate = app.javaClass.methods.firstOrNull {
                it.name == "getOrCreateAndroidAttribute" && it.parameterTypes.size == 2
            } ?: return
            val attr = getOrCreate.invoke(app, "roundIcon", ATTR_ROUND_ICON) ?: return
            val setType = attr.javaClass.methods.firstOrNull {
                it.name == "setTypeAndData" && it.parameterTypes.size == 2
            }
            setType?.invoke(attr, ValueType.REFERENCE, resourceId)
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
