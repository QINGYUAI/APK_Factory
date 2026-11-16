// Author: PKQ32
// Created: 2026-11-16
// Description: 应用内使用说明、常见问题与版本更新入口

package com.example.myapplication.ui.help

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.di.ServiceLocator
import com.example.myapplication.ui.theme.CyberCyan
import com.example.myapplication.ui.theme.CyberError
import com.example.myapplication.ui.theme.CyberHeroHeader
import com.example.myapplication.ui.theme.CyberMuted
import com.example.myapplication.ui.theme.CyberPanel
import com.example.myapplication.ui.theme.CyberSectionTitle
import com.example.myapplication.ui.theme.CyberTopBar
import com.example.myapplication.update.UpdateUiState

private const val RELEASES_PAGE = "https://github.com/QINGYUAI/APK_Factory/releases"

private data class HelpSection(
    val title: String,
    val lines: List<String>,
)

private val helpSections = listOf(
    HelpSection(
        title = "快速开始",
        lines = listOf(
            "1. 在首页点右下角「+」，按向导选择模板、填写名称并保存。",
            "2. 在编辑页按顺序添加动作；每个动作可设置失败策略，保存后即可运行。",
            "3. 为小应用配置触发器：手动、定时、系统事件或打开指定 App。",
            "4. 需要独立软件时点「添加到桌面」生成 APK；安装后先打开一次，系统才允许后台触发。",
        ),
    ),
    HelpSection(
        title = "动作说明",
        lines = listOf(
            "· 动作按顺序串行执行；某个动作失败时可按配置跳过或中止整条链路。",
            "· 播放音频/视频、通知、调节亮度等能力受系统权限与厂商后台策略影响。",
            "· 截图动作目前为占位能力，执行时会提示需要界面授权。",
        ),
    ),
    HelpSection(
        title = "触发器说明",
        lines = listOf(
            "· 后台触发采用有界队列并合并重复事件；手动运行在忙碌时保持「立即跳过」。",
            "· 定时/日程触发需要精确闹钟权限，未授权时系统可能使用近似时间。",
            "· 独立包支持定时与静态系统事件后台触发；「打开指定 App」与亮屏/灭屏触发仅在应用工厂内可用。",
            "· 修改系统时间、时区、开机或应用更新后，独立包会自动重排后台闹钟。",
        ),
    ),
    HelpSection(
        title = "独立包与配置同步",
        lines = listOf(
            "· 安装独立包后，修改动作/媒体可在小应用菜单里「同步配置」热更新，无需重装。",
            "· 更换图标或升级引擎时使用「更新应用」，会生成更高版本号并覆盖安装。",
            "· 导出签名密钥保存在本机密钥库；卸载应用工厂或清除数据后，将无法再覆盖更新已安装的独立包。",
            "· 导出的独立包与应用工厂数据相互独立，卸载应用工厂不会卸载已安装的小应用。",
        ),
    ),
    HelpSection(
        title = "权限提示",
        lines = listOf(
            "· 通知：发送通知动作；Android 13+ 需手动授权。",
            "· 使用情况访问：识别「打开指定 App」触发。",
            "· 修改系统设置：调节屏幕亮度。",
            "· 勿扰访问：切换响铃/静音/勿扰模式。",
            "· 精确闹钟：定时触发准时执行；安装未知应用：安装导出的 APK 与更新包。",
            "· 以上入口都可在「权限中心」一键查看并跳转系统设置。",
        ),
    ),
    HelpSection(
        title = "常见问题",
        lines = listOf(
            "· 到点没有执行？确认独立包已打开过一次、授予精确闹钟权限，并允许其在后台运行。",
            "· 通知/播放没有反应？检查通知权限、媒体音量与系统后台限制。",
            "· 检查更新提示网络失败？部分网络无法访问 GitHub，可点「打开发布页」手动下载。",
            "· 更新安装失败？请确认已允许「安装未知应用」，且安装包来自本仓库 Releases，签名校验不通过时应用会拒绝安装。",
            "· 为什么更新不能全自动安装？Android 要求普通应用由用户在系统安装器中确认，应用只做自动检查与一键下载。",
        ),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val updateManager = ServiceLocator.updateManager
    val state by updateManager.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CyberTopBar(
                title = "使用说明",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = CyberCyan,
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                CyberHeroHeader(
                    title = "应用工厂手册",
                    subtitle = "从动作编排到独立安装包；下方可检查版本更新",
                )
            }
            item {
                UpdatePanel(
                    state = state,
                    onToggleAutoCheck = updateManager::setAutoCheckEnabled,
                    onCheck = updateManager::checkManually,
                    onInstall = updateManager::downloadAndInstall,
                    onOpenReleasePage = { openUrl(context, RELEASES_PAGE) },
                )
            }
            items(helpSections) { section ->
                CyberPanel {
                    CyberSectionTitle(section.title)
                    section.lines.forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                CyberPanel {
                    CyberSectionTitle("关于")
                    Text(
                        text = "应用工厂 v${state.currentVersion}（版本号 ${state.currentVersionCode}）",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "所有配置与运行记录仅保存在本机，不会上传。更新包来自 GitHub Releases。",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberMuted,
                    )
                    OutlinedButton(onClick = { openUrl(context, RELEASES_PAGE) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = CyberCyan,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("访问 GitHub Releases")
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdatePanel(
    state: UpdateUiState,
    onToggleAutoCheck: (Boolean) -> Unit,
    onCheck: () -> Unit,
    onInstall: () -> Unit,
    onOpenReleasePage: () -> Unit,
) {
    CyberPanel {
        CyberSectionTitle("版本更新")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "当前版本",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "v${state.currentVersion}",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberMuted,
                )
            }
            Text(
                text = if (state.checking) "检查中…" else "GitHub Releases",
                style = MaterialTheme.typography.labelMedium,
                color = if (state.checking) CyberCyan else CyberMuted,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "自动检查更新",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "启动时检查，每天最多一次",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberMuted,
                )
            }
            Switch(
                checked = state.autoCheck,
                onCheckedChange = onToggleAutoCheck,
            )
        }
        state.error?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = CyberError,
            )
        }
        state.info?.let { info ->
            Text(
                text = "发现新版本 v${info.version}",
                style = MaterialTheme.typography.titleSmall,
                color = CyberCyan,
            )
            val notes = info.notes.ifBlank { "该版本未提供更新说明" }
            Text(
                text = notes.take(600),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.downloading) {
                LinearProgressIndicator(
                    progress = { state.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "下载中 ${state.progress}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberMuted,
                )
            }
            Button(
                onClick = onInstall,
                enabled = !state.downloading && !state.checking,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(if (state.downloading) "下载中…" else "立即更新")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onCheck,
                enabled = !state.checking && !state.downloading,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(if (state.checking) "检查中…" else "检查更新")
            }
            OutlinedButton(
                onClick = onOpenReleasePage,
                modifier = Modifier.weight(1f),
            ) {
                Text("打开发布页")
            }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.onFailure {
        Toast.makeText(context, "无法打开浏览器：$url", Toast.LENGTH_LONG).show()
    }
}
