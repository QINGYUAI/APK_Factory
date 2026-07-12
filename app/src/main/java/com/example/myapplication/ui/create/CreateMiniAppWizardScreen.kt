// Author: PKQ32
// Created: 2026-07-12
// Description: 新建小应用三步向导：命名模板 → 触发方式 → 预览保存

package com.example.myapplication.ui.create

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.myapplication.di.ServiceLocator
import com.example.myapplication.domain.model.MiniApp
import com.example.myapplication.domain.model.Trigger
import com.example.myapplication.domain.model.TriggerTypes
import com.example.myapplication.ui.icons.AppIconCatalog
import com.example.myapplication.ui.icons.AppIconPicker
import com.example.myapplication.ui.theme.CyberCyan
import com.example.myapplication.ui.theme.CyberPanel
import com.example.myapplication.ui.theme.CyberStroke
import com.example.myapplication.ui.theme.CyberTopBar
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMiniAppWizardScreen(
    onBack: () -> Unit,
    onFinished: (miniAppId: String, runNow: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var iconKey by remember { mutableStateOf("default") }
    var templateKey by remember { mutableStateOf("acceptance") }
    var triggerKey by remember { mutableStateOf("manual") }
    var saving by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CyberTopBar(
                title = when (step) {
                    0 -> "新建 · 1/3 基本信息"
                    1 -> "新建 · 2/3 触发方式"
                    else -> "新建 · 3/3 确认"
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (step == 0) onBack() else step -= 1
                        },
                    ) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (step) {
                0 -> StepBasic(
                    name = name,
                    onNameChange = { name = it },
                    iconKey = iconKey,
                    onIconChange = { iconKey = it },
                    templateKey = templateKey,
                    onTemplateChange = { templateKey = it },
                )
                1 -> StepTrigger(
                    triggerKey = triggerKey,
                    onTriggerChange = { triggerKey = it },
                )
                else -> StepConfirm(
                    name = name.ifBlank { "未命名小应用" },
                    iconKey = iconKey,
                    templateKey = templateKey,
                    triggerKey = triggerKey,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (step < 2) {
                    Button(
                        onClick = {
                            if (step == 0 && name.isBlank()) {
                                Toast.makeText(context, "请填写应用名称", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            step += 1
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("下一步")
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            if (saving) return@OutlinedButton
                            scope.launch {
                                saving = true
                                try {
                                    val id = saveMiniApp(name, iconKey, templateKey, triggerKey)
                                    ServiceLocator.triggerManager.resync()
                                    onFinished(id, false)
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "保存失败：${e.message}",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                } finally {
                                    saving = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !saving,
                    ) {
                        Text("保存")
                    }
                    Button(
                        onClick = {
                            if (saving) return@Button
                            scope.launch {
                                saving = true
                                try {
                                    val id = saveMiniApp(name, iconKey, templateKey, triggerKey)
                                    ServiceLocator.triggerManager.resync()
                                    onFinished(id, true)
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "保存失败：${e.message}",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                } finally {
                                    saving = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !saving,
                    ) {
                        Text(if (saving) "保存中…" else "保存并试运行")
                    }
                }
            }
        }
    }
}

@Composable
private fun StepBasic(
    name: String,
    onNameChange: (String) -> Unit,
    iconKey: String,
    onIconChange: (String) -> Unit,
    templateKey: String,
    onTemplateChange: (String) -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 8.dp),
    ) {
        item {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("小应用名称") },
                placeholder = { Text("例如：充电提醒") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        item {
            Text("图标", style = MaterialTheme.typography.titleSmall, color = CyberCyan)
            Spacer(modifier = Modifier.height(6.dp))
            AppIconPicker(selectedKey = iconKey, onSelect = onIconChange)
        }
        item {
            Text("动作模板", style = MaterialTheme.typography.titleSmall, color = CyberCyan)
        }
        items(ActionTemplates.all, key = { it.key }) { tpl ->
            SelectCard(
                selected = templateKey == tpl.key,
                title = tpl.titleZh,
                subtitle = tpl.subtitle,
                onClick = { onTemplateChange(tpl.key) },
            )
        }
    }
}

@Composable
private fun StepTrigger(
    triggerKey: String,
    onTriggerChange: (String) -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text(
                text = "选择一种主要触发方式（之后可在编辑器里继续添加）",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(TriggerOptions.wizardChoices, key = { it.key }) { opt ->
            SelectCard(
                selected = triggerKey == opt.key,
                title = opt.titleZh,
                subtitle = buildString {
                    append(opt.subtitle)
                    if (opt.permissionHint != null) {
                        append("\n权限：")
                        append(opt.permissionHint)
                    }
                },
                onClick = { onTriggerChange(opt.key) },
            )
        }
    }
}

@Composable
private fun StepConfirm(
    name: String,
    iconKey: String,
    templateKey: String,
    triggerKey: String,
) {
    val tpl = ActionTemplates.all.first { it.key == templateKey }
    val trigger = TriggerOptions.wizardChoices.first { it.key == triggerKey }
    val iconTitle = AppIconCatalog.specOf(iconKey).title
    CyberPanel {
        Text("名称：$name", style = MaterialTheme.typography.titleMedium)
        Text("图标：$iconTitle", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("动作：${tpl.titleZh}（${tpl.subtitle}）", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("触发：${trigger.titleZh}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = "保存后可在编辑器继续调整动作与触发器；已安装的桌面应用请用「更新应用」覆盖。",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SelectCard(
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) CyberCyan else CyberStroke,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) CyberCyan else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Text("已选", color = CyberCyan, style = MaterialTheme.typography.labelMedium)
            }
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private suspend fun saveMiniApp(
    name: String,
    iconKey: String,
    templateKey: String,
    triggerKey: String,
): String {
    val id = UUID.randomUUID().toString()
    val now = System.currentTimeMillis()
    val tpl = ActionTemplates.all.first { it.key == templateKey }
    val option = TriggerOptions.wizardChoices.first { it.key == triggerKey }
    val triggers = mutableListOf<Trigger>()
    var runOnHostOpen = false
    when (option.kind) {
        TriggerOption.Kind.Manual -> Unit
        TriggerOption.Kind.HostOpen -> runOnHostOpen = true
        TriggerOption.Kind.TriggerType -> {
            TriggerOptions.buildTrigger(triggerKey)?.let { triggers.add(it) }
        }
    }
    // hostOpened 也可同时写入 trigger 记录便于预览（可选）
    if (runOnHostOpen) {
        triggers.add(
            Trigger(
                id = UUID.randomUUID().toString(),
                type = TriggerTypes.HOST_OPENED,
                enabled = true,
            ),
        )
    }
    val app = MiniApp(
        id = id,
        name = name.trim().ifBlank { "未命名小应用" },
        iconKey = iconKey,
        enabled = true,
        runOnHostOpen = runOnHostOpen,
        createdAt = now,
        updatedAt = now,
        steps = tpl.buildSteps(),
        triggers = triggers,
    )
    ServiceLocator.miniApps.save(app)
    return id
}
