// Author: PKQ32
// Created: 2026-07-12
// Description: Runner 启动页：赛博风格运行监控，支持图标展示与停止

package com.example.minirunner

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.JsonMappers
import com.example.myapplication.domain.ActionResult
import com.example.myapplication.domain.model.MiniApp
import com.example.myapplication.engine.RunReport
import com.example.myapplication.engine.StepOutcome
import com.example.myapplication.ui.editor.actionIcon
import com.example.myapplication.ui.editor.actionTitleZh
import com.example.myapplication.ui.icons.AppIconCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RunnerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val miniApp = runCatching {
            assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
                .let { JsonMappers.miniAppFromJson(it) }
        }.getOrElse {
            Toast.makeText(this, "配置损坏：${it.message}", Toast.LENGTH_LONG).show()
            MiniApp(
                id = "broken",
                name = "迷你应用",
                createdAt = 0,
                updatedAt = 0,
            )
        }
        title = miniApp.name
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Cyan,
                    onPrimary = Color(0xFF001018),
                    background = Bg,
                    surface = Surface,
                    onBackground = OnBg,
                    onSurface = OnBg,
                    error = Error,
                ),
            ) {
                RunnerScreen(miniApp = miniApp)
            }
        }
    }

    companion object {
        const val ASSET_NAME = "miniapp.json"
    }
}

private val Bg = Color(0xFF070B14)
private val Surface = Color(0xFF0E1626)
private val SurfaceVariant = Color(0xFF152238)
private val Cyan = Color(0xFF00E5FF)
private val Stroke = Color(0xFF1E3A4F)
private val Muted = Color(0xFF8BA3B8)
private val OnBg = Color(0xFFE6F4FF)
private val Success = Color(0xFF3DFFB0)
private val Warn = Color(0xFFFFB020)
private val Error = Color(0xFFFF4D6A)
private val PanelShape = RoundedCornerShape(14.dp)

@Composable
private fun RunnerScreen(miniApp: MiniApp) {
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<RunReport?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var runJob by remember { mutableStateOf<Job?>(null) }

    fun stopNow() {
        runJob?.cancel()
    }

    fun runNow() {
        if (running) return
        runJob?.cancel()
        runJob = scope.launch {
            running = true
            error = null
            report = null
            try {
                val result = withContext(Dispatchers.Default) {
                    RunnerApplication.instance.executor.run(miniApp.steps)
                }
                report = result
            } catch (e: Exception) {
                error = e.message ?: "执行失败"
            } finally {
                running = false
                runJob = null
            }
        }
    }

    LaunchedEffect(miniApp.id) {
        if (miniApp.steps.isNotEmpty() && miniApp.enabled) {
            runNow()
        }
    }

    DisposableEffect(Unit) {
        onDispose { runJob?.cancel() }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "hero") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF003D4D), SurfaceVariant, Bg),
                        ),
                        PanelShape,
                    )
                    .border(1.dp, Stroke, PanelShape)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = AppIconCatalog.iconOf(miniApp.iconKey),
                        contentDescription = null,
                        tint = Cyan,
                        modifier = Modifier.size(36.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = miniApp.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Cyan,
                        )
                        Text(
                            text = "共 ${miniApp.steps.size} 个动作",
                            color = Muted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    StatusChip(
                        text = when {
                            running -> "运行中"
                            report?.stopped == true -> "已停止"
                            report?.success == true -> "成功"
                            report != null -> "完成"
                            else -> "就绪"
                        },
                        active = running || report?.success == true,
                    )
                }
            }
        }

        item(key = "actions") {
            if (running) {
                Button(
                    onClick = { stopNow() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Error),
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Text("停止", modifier = Modifier.padding(start = 6.dp))
                }
            } else {
                Button(
                    onClick = { runNow() },
                    enabled = miniApp.steps.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Color(0xFF001018)),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Text(
                        if (report == null) "立即运行" else "重新运行",
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }

        when {
            error != null -> item(key = "error") {
                Panel {
                    Text("执行异常", style = MaterialTheme.typography.titleMedium, color = OnBg)
                    Text(error!!, color = Error)
                }
            }
            running -> item(key = "running") {
                Panel {
                    Text("正在执行动作链…", style = MaterialTheme.typography.titleMedium, color = OnBg)
                    Text("可随时点停止中断后续步骤", color = Muted)
                }
            }
            report != null -> {
                val r = report!!
                item(key = "summary") {
                    Panel {
                        Text(
                            text = when {
                                r.stopped -> "已手动停止"
                                r.success -> "执行成功"
                                else -> "执行完成（有失败步骤）"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = when {
                                r.stopped -> Warn
                                r.success -> Success
                                else -> Warn
                            },
                        )
                        Text("共记录 ${r.stepOutcomes.size} 条步骤结果", color = Muted)
                    }
                }
                itemsIndexed(
                    r.stepOutcomes,
                    key = { index, o -> "${o.stepId}-$index" },
                ) { index, outcome ->
                    StepCard(index = index + 1, outcome = outcome)
                }
            }
            miniApp.steps.isEmpty() -> item(key = "empty") {
                Panel {
                    Text("未配置动作", color = Muted)
                }
            }
        }
    }
}

@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface, PanelShape)
            .border(1.dp, Stroke, PanelShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun StatusChip(text: String, active: Boolean) {
    Text(
        text = text,
        modifier = Modifier
            .background(
                if (active) Color(0xFF003D4D) else SurfaceVariant,
                RoundedCornerShape(999.dp),
            )
            .border(1.dp, if (active) Cyan.copy(alpha = 0.45f) else Stroke, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = if (active) Success else Muted,
    )
}

@Composable
private fun StepCard(index: Int, outcome: StepOutcome) {
    val stopped = outcome.result is ActionResult.Failed &&
        (outcome.result as ActionResult.Failed).code == "STOPPED"
    val ok = outcome.result is ActionResult.Success
    val detail = when (val res = outcome.result) {
        is ActionResult.Success -> "执行完成"
        is ActionResult.Failed -> if (stopped) "用户已停止" else res.message
    }
    val tint = when {
        ok -> Success
        stopped -> Warn
        else -> Error
    }
    Panel {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = if (stopped) Icons.Filled.Stop else actionIcon(outcome.type),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "步骤 $index · ${if (stopped) "停止" else actionTitleZh(outcome.type)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = OnBg,
                )
                Text(outcome.type, style = MaterialTheme.typography.labelSmall, color = Muted)
            }
            StatusChip(
                text = when {
                    ok -> "成功"
                    stopped -> "已停止"
                    else -> "失败"
                },
                active = ok,
            )
        }
        Text(detail, color = if (ok || stopped) Muted else Error)
    }
}
