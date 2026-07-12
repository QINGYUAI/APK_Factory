// Author: PKQ32
// Created: 2026-07-12
// Description: 页内媒体控制条：暂停/继续、停止（适配安全区）

package com.example.myapplication.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.platform.media.MediaKind
import com.example.myapplication.platform.media.MediaPlaybackController
import com.example.myapplication.platform.media.MediaPlaybackUiState

private val BarShape = RoundedCornerShape(14.dp)
private val Cyan = Color(0xFF00E5FF)
private val Surface = Color(0xFF0E1626)
private val Muted = Color(0xFF8BA3B8)
private val OnBg = Color(0xFFE6F4FF)
private val ErrorRed = Color(0xFFFF4D6A)

/**
 * 订阅 [MediaPlaybackController]；无播放时不占位。
 */
@Composable
fun MediaControlBar(
    modifier: Modifier = Modifier,
) {
    val state by MediaPlaybackController.state.collectAsStateWithLifecycle()
    val active = state as? MediaPlaybackUiState.Active ?: return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Surface, BarShape)
            .border(1.dp, Cyan.copy(alpha = 0.35f), BarShape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = if (active.kind == MediaKind.Video) {
                    Icons.Filled.Videocam
                } else {
                    Icons.Filled.MusicNote
                },
                contentDescription = null,
                tint = Cyan,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = active.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = OnBg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = when {
                        active.paused -> "已暂停 · ${formatProgress(active.positionMs, active.durationMs)}"
                        active.kind == MediaKind.Video ->
                            "视频播放中 · ${formatProgress(active.positionMs, active.durationMs)}"
                        else ->
                            "音频播放中 · ${formatProgress(active.positionMs, active.durationMs)}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Muted,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { MediaPlaybackController.togglePause() },
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = if (active.paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = if (active.paused) "继续" else "暂停",
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            Button(
                onClick = { MediaPlaybackController.stop() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
            ) {
                Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("停止", modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

private fun formatProgress(positionMs: Long, durationMs: Long): String {
    fun fmt(ms: Long): String {
        val totalSec = (ms / 1000L).coerceAtLeast(0L)
        val m = totalSec / 60L
        val s = totalSec % 60L
        return "%d:%02d".format(m, s)
    }
    return if (durationMs > 0L) "${fmt(positionMs)} / ${fmt(durationMs)}" else fmt(positionMs)
}
