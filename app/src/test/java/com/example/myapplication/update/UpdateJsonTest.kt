// Author: PKQ32
// Created: 2026-11-16
// Description: GitHub releases/latest JSON 解析与 APK 资产选择单测

package com.example.myapplication.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateJsonTest {

    private val releaseJson = """
        {
          "tag_name": "v1.2.3",
          "body": "修复若干问题\n新增帮助页面",
          "html_url": "https://github.com/QINGYUAI/APK_Factory/releases/tag/v1.2.3",
          "assets": [
            {
              "name": "checksums.txt",
              "browser_download_url": "https://example.com/checksums.txt",
              "size": 128
            },
            {
              "name": "AppFactory-1.2.3.apk",
              "browser_download_url": "https://example.com/AppFactory-1.2.3.apk",
              "size": 123456
            }
          ]
        }
    """.trimIndent()

    @Test
    fun parsesTagNotesAndPreferredApkAsset() {
        val info = UpdateJson.parseRelease(releaseJson)
        assertNotNull(info)
        requireNotNull(info)
        assertEquals("1.2.3", info.version)
        assertEquals("v1.2.3", info.tagName)
        assertEquals("https://example.com/AppFactory-1.2.3.apk", info.apkUrl)
        assertEquals(123456L, info.apkSize)
        assertEquals("https://github.com/QINGYUAI/APK_Factory/releases/tag/v1.2.3", info.pageUrl)
        assertEquals(true, info.notes.contains("帮助页面"))
    }

    @Test
    fun fallsBackToAnyApkAsset() {
        val json = """
            {
              "tag_name": "v1.1.0",
              "assets": [
                {"name": "release.apk", "browser_download_url": "https://example.com/release.apk", "size": 1}
              ]
            }
        """.trimIndent()
        val info = UpdateJson.parseRelease(json)
        assertNotNull(info)
        assertEquals("https://example.com/release.apk", info?.apkUrl)
        assertEquals("1.1.0", info?.version)
        assertEquals("", info?.pageUrl)
    }

    @Test
    fun ignoresReleaseWithoutApkAsset() {
        val json = """
            {
              "tag_name": "v1.1.0",
              "assets": [
                {"name": "source.zip", "browser_download_url": "https://example.com/source.zip", "size": 10}
              ]
            }
        """.trimIndent()
        assertNull(UpdateJson.parseRelease(json))
    }

    @Test
    fun rejectsMalformedOrIncompleteJson() {
        assertNull(UpdateJson.parseRelease("not json"))
        assertNull(UpdateJson.parseRelease("""{"assets": []}"""))
        assertNull(UpdateJson.parseRelease("""{"tag_name": "nightly", "assets": []}"""))
        assertNull(
            UpdateJson.parseRelease(
                """{"tag_name":"v1.0.0","assets":[{"name":"a.apk"}]}""",
            ),
        )
    }
}
