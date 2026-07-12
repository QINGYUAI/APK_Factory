// Author: PKQ32
// Created: 2026-07-11
// Description: 赛博风格共用面板与顶栏组件

package com.example.myapplication.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

private val PanelShape = RoundedCornerShape(14.dp)

/**
 * 带青色描边的科技面板。
 */
@Composable
fun CyberPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, PanelShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/**
 * 页面顶部渐变氛围条 + 标题区。
 */
@Composable
fun CyberHeroHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        CyberCyanDim,
                        CyberSurfaceVariant,
                        CyberBg,
                    ),
                ),
            )
            .border(1.dp, CyberStroke, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = CyberCyan,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = CyberMuted,
            )
        }
    }
}

/**
 * 状态胶囊（已启用 / 已授权 等）。
 */
@Composable
fun CyberStatusChip(
    text: String,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val bg = if (active) CyberCyanDim else ColorMutedChipBg
    val fg = if (active) CyberSuccess else CyberMuted
    val stroke = if (active) CyberCyan.copy(alpha = 0.45f) else CyberStroke
    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, stroke, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = fg,
    )
}

private val ColorMutedChipBg = CyberSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberTopBar(
    title: String,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = CyberOnBg,
            )
        },
        navigationIcon = { navigationIcon?.invoke() },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = CyberBg,
            titleContentColor = CyberOnBg,
            navigationIconContentColor = CyberCyan,
            actionIconContentColor = CyberCyan,
        ),
    )
}

@Composable
fun CyberSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(2.dp))
                .background(CyberCyan)
                .padding(horizontal = 3.dp, vertical = 10.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = CyberOnBg,
        )
    }
}
