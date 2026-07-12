// Author: PKQ32
// Created: 2026-07-12
// Description: 媒体播放状态（含进度，供通知栏与页内控制条）

package com.example.myapplication.platform.media

enum class MediaKind {
    Audio,
    Video,
}

sealed class MediaPlaybackUiState {
    data object Idle : MediaPlaybackUiState()

    data class Active(
        val title: String,
        val kind: MediaKind,
        val paused: Boolean,
        val durationMs: Long = 0L,
        val positionMs: Long = 0L,
    ) : MediaPlaybackUiState()
}
