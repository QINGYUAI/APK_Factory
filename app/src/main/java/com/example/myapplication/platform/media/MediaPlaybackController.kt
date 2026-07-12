// Author: PKQ32
// Created: 2026-07-12
// Description: 全局媒体播放控制器：暂停/进度/停止，同步通知栏 MediaSession

package com.example.myapplication.platform.media

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicReference

/**
 * 应用内单例媒体控制器。音乐用 MediaPlayer；视频由 [VideoHost] 桥接到 VideoView。
 */
object MediaPlaybackController {

    interface VideoHost {
        fun pauseVideo()
        fun resumeVideo()
        fun stopVideo()
        fun isVideoPlaying(): Boolean
        fun getPositionMs(): Long
        fun getDurationMs(): Long
        fun seekTo(positionMs: Long)
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val appContextRef = AtomicReference<Context?>(null)
    private var player: MediaPlayer? = null
    private var videoHost: VideoHost? = null

    private val _state = MutableStateFlow<MediaPlaybackUiState>(MediaPlaybackUiState.Idle)
    val state: StateFlow<MediaPlaybackUiState> = _state.asStateFlow()

    fun init(context: Context) {
        appContextRef.set(context.applicationContext)
    }

    fun playAudio(filePath: String, title: String) {
        runOnMain {
            stopInternal(clearState = false)
            val ctx = requireContext()
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                setDataSource(filePath)
                setOnCompletionListener { stop() }
                setOnErrorListener { _, _, _ ->
                    stop()
                    true
                }
                prepare()
                start()
            }
            player = mp
            val duration = mp.duration.toLong().coerceAtLeast(0L)
            _state.value = MediaPlaybackUiState.Active(
                title = title.ifBlank { "正在播放" },
                kind = MediaKind.Audio,
                paused = false,
                durationMs = duration,
                positionMs = 0L,
            )
            syncService(ctx)
        }
    }

    fun attachVideo(host: VideoHost, title: String) {
        runOnMain {
            stopInternal(clearState = false)
            videoHost = host
            val ctx = requireContext()
            val duration = host.getDurationMs().coerceAtLeast(0L)
            _state.value = MediaPlaybackUiState.Active(
                title = title.ifBlank { "正在播放视频" },
                kind = MediaKind.Video,
                paused = false,
                durationMs = duration,
                positionMs = host.getPositionMs().coerceAtLeast(0L),
            )
            syncService(ctx)
        }
    }

    fun pause() {
        runOnMain {
            val current = _state.value as? MediaPlaybackUiState.Active ?: return@runOnMain
            when (current.kind) {
                MediaKind.Audio -> player?.takeIf { it.isPlaying }?.pause()
                MediaKind.Video -> videoHost?.pauseVideo()
            }
            _state.value = current.copy(paused = true, positionMs = currentPositionMs())
            appContextRef.get()?.let { syncService(it) }
        }
    }

    fun resume() {
        runOnMain {
            val current = _state.value as? MediaPlaybackUiState.Active ?: return@runOnMain
            when (current.kind) {
                MediaKind.Audio -> player?.start()
                MediaKind.Video -> videoHost?.resumeVideo()
            }
            _state.value = current.copy(paused = false, positionMs = currentPositionMs())
            appContextRef.get()?.let { syncService(it) }
        }
    }

    fun togglePause() {
        val current = _state.value as? MediaPlaybackUiState.Active ?: return
        if (current.paused) resume() else pause()
    }

    fun seekTo(positionMs: Long) {
        runOnMain {
            val current = _state.value as? MediaPlaybackUiState.Active ?: return@runOnMain
            val target = positionMs.coerceIn(0L, current.durationMs.takeIf { it > 0 } ?: Long.MAX_VALUE)
            when (current.kind) {
                MediaKind.Audio -> player?.seekTo(target.toInt())
                MediaKind.Video -> videoHost?.seekTo(target)
            }
            _state.value = current.copy(positionMs = target)
            appContextRef.get()?.let { syncService(it) }
        }
    }

    /** 供通知服务轮询进度，不强制刷新通知渠道。 */
    fun snapshotProgress(): MediaPlaybackUiState.Active? {
        val current = _state.value as? MediaPlaybackUiState.Active ?: return null
        val pos = currentPositionMs()
        val duration = when (current.kind) {
            MediaKind.Audio -> player?.duration?.toLong()?.coerceAtLeast(0L) ?: current.durationMs
            MediaKind.Video -> {
                val d = videoHost?.getDurationMs()?.coerceAtLeast(0L) ?: 0L
                if (d > 0L) d else current.durationMs
            }
        }
        val updated = current.copy(positionMs = pos, durationMs = duration)
        if (updated != current) {
            _state.value = updated
        }
        return updated
    }

    fun stop() {
        runOnMain {
            stopInternal(clearState = true)
            appContextRef.get()?.let { ctx ->
                ctx.stopService(Intent(ctx, MediaPlaybackService::class.java))
            }
        }
    }

    fun detachVideo(host: VideoHost) {
        runOnMain {
            if (videoHost === host) {
                videoHost = null
                if (_state.value is MediaPlaybackUiState.Active &&
                    (_state.value as MediaPlaybackUiState.Active).kind == MediaKind.Video
                ) {
                    _state.value = MediaPlaybackUiState.Idle
                    appContextRef.get()?.let { it.stopService(Intent(it, MediaPlaybackService::class.java)) }
                }
            }
        }
    }

    private fun currentPositionMs(): Long {
        return when ((_state.value as? MediaPlaybackUiState.Active)?.kind) {
            MediaKind.Audio -> player?.currentPosition?.toLong()?.coerceAtLeast(0L) ?: 0L
            MediaKind.Video -> videoHost?.getPositionMs()?.coerceAtLeast(0L) ?: 0L
            null -> 0L
        }
    }

    private fun stopInternal(clearState: Boolean) {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        runCatching { videoHost?.stopVideo() }
        videoHost = null
        if (clearState) {
            _state.value = MediaPlaybackUiState.Idle
        }
    }

    private fun syncService(context: Context) {
        val intent = Intent(context, MediaPlaybackService::class.java).apply {
            action = MediaPlaybackService.ACTION_SYNC
        }
        ContextCompat.startForegroundService(context, intent)
    }

    private fun requireContext(): Context =
        appContextRef.get() ?: error("MediaPlaybackController 未初始化")

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }
}
