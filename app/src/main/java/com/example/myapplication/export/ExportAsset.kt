// Author: PKQ32
// Created: 2026-11-16
// Description: 导出资源描述：APK 内路径 + 本地文件，打包时按流读取，避免整段载入内存

package com.example.myapplication.export

import java.io.File

data class ExportAsset(
    /** APK 内路径，如 assets/media/step_xxx.mp3 */
    val apkPath: String,
    val file: File,
)
