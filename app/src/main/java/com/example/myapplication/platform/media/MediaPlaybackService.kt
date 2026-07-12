// Author: PKQ32
// Created: 2026-07-12
// Description: 媒体前台服务：MediaStyle 通知 + 进度条 + 暂停/继续/停止

package com.example.myapplication.platform.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat

class MediaPlaybackService : Service() {

    private var session: MediaSessionCompat? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val progressTicker = object : Runnable {
        override fun run() {
            val active = MediaPlaybackController.snapshotProgress()
            if (active == null) {
                stopProgressTicker()
                return
            }
            updateSession(active)
            // 系统媒体控件进度主要读 MediaSession；低频刷新通知文案
            if (!active.paused) {
                val manager = getSystemService(NotificationManager::class.java)
                manager?.notify(NOTIFICATION_ID, buildNotification(active))
                mainHandler.postDelayed(this, PROGRESS_INTERVAL_MS)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        session = MediaSessionCompat(this, "MiniAppMedia").apply {
            setCallback(
                object : MediaSessionCompat.Callback() {
                    override fun onPlay() = MediaPlaybackController.resume()
                    override fun onPause() = MediaPlaybackController.pause()
                    override fun onStop() = MediaPlaybackController.stop()
                    override fun onSeekTo(pos: Long) = MediaPlaybackController.seekTo(pos)
                },
            )
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> MediaPlaybackController.pause()
            ACTION_RESUME -> MediaPlaybackController.resume()
            ACTION_STOP -> {
                stopProgressTicker()
                MediaPlaybackController.stop()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SYNC, null -> Unit
        }
        val state = MediaPlaybackController.snapshotProgress()
            ?: MediaPlaybackController.state.value as? MediaPlaybackUiState.Active
        if (state == null) {
            stopProgressTicker()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        updateSession(state)
        val notification = buildNotification(state)
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            },
        )
        if (state.paused) {
            stopProgressTicker()
        } else {
            startProgressTicker()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopProgressTicker()
        session?.isActive = false
        session?.release()
        session = null
        super.onDestroy()
    }

    private fun startProgressTicker() {
        mainHandler.removeCallbacks(progressTicker)
        mainHandler.postDelayed(progressTicker, PROGRESS_INTERVAL_MS)
    }

    private fun stopProgressTicker() {
        mainHandler.removeCallbacks(progressTicker)
    }

    private fun updateSession(state: MediaPlaybackUiState.Active) {
        val session = session ?: return
        session.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, state.title)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, state.title)
                .putLong(
                    MediaMetadataCompat.METADATA_KEY_DURATION,
                    state.durationMs.coerceAtLeast(0L),
                )
                .build(),
        )
        val pbState = if (state.paused) {
            PlaybackStateCompat.STATE_PAUSED
        } else {
            PlaybackStateCompat.STATE_PLAYING
        }
        session.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_SEEK_TO,
                )
                .setState(
                    pbState,
                    state.positionMs.coerceAtLeast(0L),
                    if (state.paused) 0f else 1f,
                )
                .build(),
        )
    }

    private fun buildNotification(state: MediaPlaybackUiState.Active): Notification {
        val pauseResumeAction = if (state.paused) {
            NotificationCompat.Action(
                android.R.drawable.ic_media_play,
                "继续",
                pendingAction(ACTION_RESUME),
            )
        } else {
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause,
                "暂停",
                pendingAction(ACTION_PAUSE),
            )
        }
        val stopAction = NotificationCompat.Action(
            android.R.drawable.ic_menu_close_clear_cancel,
            "停止",
            pendingAction(ACTION_STOP),
        )
        val progressText = formatProgress(state.positionMs, state.durationMs)
        val content = if (state.kind == MediaKind.Video) "视频播放中" else "音频播放中"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(state.title)
            .setContentText(
                if (state.paused) "已暂停 · $progressText" else "$content · $progressText",
            )
            .setSubText(progressText)
            .setOngoing(!state.paused)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .addAction(pauseResumeAction)
            .addAction(stopAction)
            .setStyle(
                MediaNotificationCompat.MediaStyle()
                    .setMediaSession(session?.sessionToken)
                    .setShowActionsInCompactView(0, 1),
            )
            .build()
    }

    private fun formatProgress(positionMs: Long, durationMs: Long): String {
        fun fmt(ms: Long): String {
            val totalSec = (ms / 1000L).coerceAtLeast(0L)
            val m = totalSec / 60L
            val s = totalSec % 60L
            return "%d:%02d".format(m, s)
        }
        return if (durationMs > 0L) {
            "${fmt(positionMs)} / ${fmt(durationMs)}"
        } else {
            fmt(positionMs)
        }
    }

    private fun pendingAction(action: String): PendingIntent {
        val intent = Intent(this, MediaPlaybackService::class.java).setAction(action)
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "媒体播放",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "音乐与视频播放控制"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_SYNC = "com.example.myapplication.media.SYNC"
        const val ACTION_PAUSE = "com.example.myapplication.media.PAUSE"
        const val ACTION_RESUME = "com.example.myapplication.media.RESUME"
        const val ACTION_STOP = "com.example.myapplication.media.STOP"
        private const val CHANNEL_ID = "media_playback"
        private const val NOTIFICATION_ID = 42001
        private const val PROGRESS_INTERVAL_MS = 500L
    }
}
