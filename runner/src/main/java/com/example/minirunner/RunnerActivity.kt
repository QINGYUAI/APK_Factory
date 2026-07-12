// Author: PKQ32
// Created: 2026-07-12
// Description: Runner 启动页：读取 assets/miniapp.json 并执行动作链

package com.example.minirunner

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.JsonMappers
import com.example.myapplication.domain.ActionResult
import com.example.myapplication.domain.model.MiniApp
import com.example.myapplication.engine.RunReport
import kotlinx.coroutines.Dispatchers
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
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF00E5FF))) {
                RunnerScreen(miniApp = miniApp)
            }
        }
    }

    companion object {
        const val ASSET_NAME = "miniapp.json"
    }
}

@Composable
private fun RunnerScreen(miniApp: MiniApp) {
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<RunReport?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun runNow() {
        scope.launch {
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
            }
        }
    }

    LaunchedEffect(miniApp.id) {
        if (miniApp.steps.isNotEmpty() && miniApp.enabled) {
            runNow()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B14))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = miniApp.name,
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFF00E5FF),
        )
        Text(
            text = "共 ${miniApp.steps.size} 个动作",
            color = Color(0xFF8BA3B8),
        )
        Button(
            onClick = { runNow() },
            enabled = !running && miniApp.steps.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (running) "执行中…" else "重新运行")
        }
        when {
            error != null -> Text(error!!, color = Color(0xFFFF4D6A))
            report != null -> {
                val r = report!!
                Text(
                    text = if (r.success) "执行成功" else "执行完成（有失败步骤）",
                    color = if (r.success) Color(0xFF3DFFB0) else Color(0xFFFFB020),
                )
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(r.stepOutcomes) { index, outcome ->
                        val ok = outcome.result is ActionResult.Success
                        val detail = when (val res = outcome.result) {
                            is ActionResult.Success -> "成功"
                            is ActionResult.Failed -> res.message
                        }
                        Text(
                            text = "${index + 1}. ${outcome.type} · $detail",
                            color = if (ok) Color(0xFFE6F4FF) else Color(0xFFFF4D6A),
                        )
                    }
                }
            }
            miniApp.steps.isEmpty() -> Text("未配置动作", color = Color(0xFF8BA3B8))
        }
    }
}
