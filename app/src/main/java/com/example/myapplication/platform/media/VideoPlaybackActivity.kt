// Author: PKQ32
// Created: 2026-07-12
// Description: 应用内视频播放页，支持暂停/停止并同步通知栏

package com.example.myapplication.platform.media

import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class VideoPlaybackActivity : ComponentActivity(), MediaPlaybackController.VideoHost {

    private lateinit var videoView: VideoView
    private var finishedByUser = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MediaPlaybackController.init(applicationContext)

        val uri = intent?.data
        if (uri == null) {
            Toast.makeText(this, "无效的视频地址", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val title = intent?.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "视频播放" }

        videoView = VideoView(this).apply {
            setBackgroundColor(0xFF070B14.toInt())
        }
        setContentView(videoView)
        ViewCompat.setOnApplyWindowInsetsListener(videoView) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val controller = MediaController(this)
        controller.setAnchorView(videoView)
        videoView.setMediaController(controller)
        videoView.setVideoURI(uri)
        videoView.setOnPreparedListener {
            MediaPlaybackController.attachVideo(this, title)
            videoView.start()
        }
        videoView.setOnCompletionListener {
            finishedByUser = true
            MediaPlaybackController.stop()
            finish()
        }
        videoView.setOnErrorListener { _, _, _ ->
            Toast.makeText(this, "无法播放该视频", Toast.LENGTH_SHORT).show()
            MediaPlaybackController.stop()
            finish()
            true
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finishedByUser = true
                    MediaPlaybackController.stop()
                    finish()
                }
            },
        )
    }

    override fun pauseVideo() {
        if (::videoView.isInitialized && videoView.isPlaying) {
            videoView.pause()
        }
    }

    override fun resumeVideo() {
        if (::videoView.isInitialized) {
            videoView.start()
        }
    }

    override fun stopVideo() {
        if (::videoView.isInitialized) {
            runCatching { videoView.stopPlayback() }
        }
        if (!finishedByUser && !isFinishing) {
            finishedByUser = true
            finish()
        }
    }

    override fun isVideoPlaying(): Boolean =
        ::videoView.isInitialized && videoView.isPlaying

    override fun getPositionMs(): Long =
        if (::videoView.isInitialized) videoView.currentPosition.toLong().coerceAtLeast(0L) else 0L

    override fun getDurationMs(): Long =
        if (::videoView.isInitialized) videoView.duration.toLong().coerceAtLeast(0L) else 0L

    override fun seekTo(positionMs: Long) {
        if (::videoView.isInitialized) {
            videoView.seekTo(positionMs.toInt().coerceAtLeast(0))
        }
    }
    override fun onDestroy() {
        MediaPlaybackController.detachVideo(this)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_TITLE = "title"
    }
}
