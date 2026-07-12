// Author: PKQ32
// Created: 2026-07-12
// Description: 小应用图标选择网格（彩色预览与导出桌面图标一致）

package com.example.myapplication.ui.icons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.CyberCyan
import com.example.myapplication.ui.theme.CyberStroke

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppIconPicker(
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppIconCatalog.all.forEach { spec ->
            val selected = spec.key == selectedKey
            Column(
                modifier = Modifier
                    .width(72.dp)
                    .border(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) CyberCyan else CyberStroke,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .clickable { onSelect(spec.key) }
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(spec.tintBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = spec.icon,
                        contentDescription = spec.title,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    text = spec.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) CyberCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
