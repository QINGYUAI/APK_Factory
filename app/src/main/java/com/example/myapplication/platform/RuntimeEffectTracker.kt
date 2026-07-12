// Author: PKQ32
// Created: 2026-07-12
// Description: 运行中效果追踪：动作链 / 音视频 / 手电筒，供针对性关闭

package com.example.myapplication.platform

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import com.example.myapplication.platform.media.MediaKind
import com.example.myapplication.platform.media.MediaPlaybackController
import com.example.myapplication.platform.media.MediaPlaybackUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RuntimeEffectKind {
    ActionChain,
    Audio,
    Video,
    Flashlight,
}

data class RuntimeEffectItem(
    val kind: RuntimeEffectKind,
    val titleZh: String,
    val detail: String = "",
)

object RuntimeEffectTracker {

    private val _flashlightOn = MutableStateFlow(false)
    val flashlightOn: StateFlow<Boolean> = _flashlightOn.asStateFlow()

    fun setFlashlightOn(on: Boolean) {
        _flashlightOn.value = on
    }

    fun listActive(
        actionChainRunning: Boolean,
        mediaState: MediaPlaybackUiState = MediaPlaybackController.state.value,
        torchOn: Boolean = _flashlightOn.value,
    ): List<RuntimeEffectItem> {
        val items = mutableListOf<RuntimeEffectItem>()
        if (actionChainRunning) {
            items += RuntimeEffectItem(
                kind = RuntimeEffectKind.ActionChain,
                titleZh = "动作链",
                detail = "正在逐步执行配置的动作",
            )
        }
        val media = mediaState as? MediaPlaybackUiState.Active
        if (media != null) {
            when (media.kind) {
                MediaKind.Audio -> items += RuntimeEffectItem(
                    kind = RuntimeEffectKind.Audio,
                    titleZh = "音乐播放",
                    detail = media.title.ifBlank { "正在播放音频" } +
                        if (media.paused) "（已暂停）" else "",
                )
                MediaKind.Video -> items += RuntimeEffectItem(
                    kind = RuntimeEffectKind.Video,
                    titleZh = "视频播放",
                    detail = media.title.ifBlank { "正在播放视频" } +
                        if (media.paused) "（已暂停）" else "",
                )
            }
        }
        if (torchOn) {
            items += RuntimeEffectItem(
                kind = RuntimeEffectKind.Flashlight,
                titleZh = "手电筒",
                detail = "闪光灯处于开启状态",
            )
        }
        return items
    }

    /**
     * 仅关闭 [targets] 中列出的效果；返回实际已关闭的中文名。
     */
    fun stop(
        context: Context,
        targets: List<RuntimeEffectKind>,
        cancelActionChain: () -> Unit,
    ): List<String> {
        val closed = mutableListOf<String>()
        val set = targets.toSet()
        if (RuntimeEffectKind.ActionChain in set) {
            cancelActionChain()
            closed += "动作链"
        }
        if (RuntimeEffectKind.Audio in set || RuntimeEffectKind.Video in set) {
            val media = MediaPlaybackController.state.value as? MediaPlaybackUiState.Active
            if (media != null) {
                val shouldStop = when (media.kind) {
                    MediaKind.Audio -> RuntimeEffectKind.Audio in set
                    MediaKind.Video -> RuntimeEffectKind.Video in set
                }
                if (shouldStop) {
                    runCatching { MediaPlaybackController.stop() }
                    closed += if (media.kind == MediaKind.Audio) "音乐播放" else "视频播放"
                }
            }
        }
        if (RuntimeEffectKind.Flashlight in set && _flashlightOn.value) {
            if (turnOffFlashlight(context)) {
                closed += "手电筒"
            }
        }
        return closed
    }

    fun turnOffFlashlight(context: Context): Boolean {
        val cameraManager =
            context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return false
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return false
            cameraManager.setTorchMode(cameraId, false)
            _flashlightOn.value = false
            true
        } catch (_: Exception) {
            false
        }
    }
}
