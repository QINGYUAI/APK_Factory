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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.icons.AppIconCatalog
import com.example.myapplication.ui.icons.AppIconPicker
import com.example.myapplication.ui.theme.CyberCyan
import com.example.myapplication.ui.theme.CyberPanel
import com.example.myapplication.ui.theme.CyberStroke
import com.example.myapplication.ui.theme.CyberTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMiniAppWizardScreen(
    onBack: () -> Unit,
    onFinished: (miniAppId: String, runNow: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val vm: CreateMiniAppViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        vm.messages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(Unit) {
        vm.created.collect { created -> onFinished(created.id, created.runNow) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CyberTopBar(
                title = when (state.step) {
                    0 -> "新建 · 1/3 基本信息"
                    1 -> "新建 · 2/3 触发方式"
                    else -> "新建 · 3/3 确认"
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (!vm.previousStep()) onBack()
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
            when (state.step) {
                0 -> StepBasic(
                    name = state.name,
                    onNameChange = vm::setName,
                    iconKey = state.iconKey,
                    onIconChange = vm::setIconKey,
                    templateKey = state.templateKey,
                    onTemplateChange = vm::setTemplateKey,
                    modifier = Modifier.weight(1f),
                )
                1 -> StepTrigger(
                    triggerKey = state.triggerKey,
                    onTriggerChange = vm::setTriggerKey,
                    modifier = Modifier.weight(1f),
                )
                else -> {
                    StepConfirm(
                        name = state.name.ifBlank { "未命名小应用" },
                        iconKey = state.iconKey,
                        templateKey = state.templateKey,
                        triggerKey = state.triggerKey,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.step < 2) {
                    Button(
                        onClick = { vm.nextStep() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("下一步")
                    }
                } else {
                    OutlinedButton(
                        onClick = { vm.save(runNow = false) },
                        modifier = Modifier.weight(1f),
                        enabled = !state.saving,
                    ) {
                        Text("保存")
                    }
                    Button(
                        onClick = { vm.save(runNow = true) },
                        modifier = Modifier.weight(1f),
                        enabled = !state.saving,
                    ) {
                        Text(if (state.saving) "保存中…" else "保存并试运行")
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
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
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
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
