// Author: PKQ32
// Created: 2026-07-11
// Description: 播放本地音频动作处理器，通过 MediaPlayer 播放 uri 或 path

package com.example.myapplication.platform.handlers

import android.media.MediaPlayer
import android.net.Uri
import com.example.myapplication.domain.ActionHandler
import com.example.myapplication.domain.ActionResult
import com.example.myapplication.domain.model.ActionTypes
import com.example.myapplication.platform.PlatformActionContext
import java.io.File
import java.io.IOException

class PlayMusicHandler(
    private val ctx: PlatformActionContext,
) : ActionHandler {
    override val type = ActionTypes.PLAY_MUSIC

    override fun validate(params: Map<String, String>): String? {
        val uri = params["uri"]
        val path = params["path"]
        if (uri.isNullOrBlank() && path.isNullOrBlank()) {
            return "uri or path required"
        }
        return null
    }

    override suspend fun execute(params: Map<String, String>): ActionResult {
        val uriParam = params["uri"]?.takeIf { it.isNotBlank() }
        val pathParam = params["path"]?.takeIf { it.isNotBlank() }
        if (uriParam == null && pathParam == null) {
            return ActionResult.Failed("INVALID_PARAMS", "uri or path required")
        }

        if (pathParam != null && uriParam == null) {
            val file = File(pathParam)
            if (!file.exists() || !file.isFile) {
                return ActionResult.Failed("FILE_NOT_FOUND", "Audio file not found: $pathParam")
            }
        }

        val source = uriParam ?: pathParam!!
        val player = MediaPlayer()
        return try {
            if (uriParam != null) {
                player.setDataSource(ctx.appContext, Uri.parse(uriParam))
            } else {
                player.setDataSource(pathParam!!)
            }
            player.setOnCompletionListener { it.release() }
            player.setOnErrorListener { mp, _, _ ->
                mp.release()
                true
            }
            player.prepare()
            player.start()
            ActionResult.Success
        } catch (e: IOException) {
            player.release()
            ActionResult.Failed("FILE_NOT_FOUND", e.message ?: "Cannot open audio: $source")
        } catch (e: SecurityException) {
            player.release()
            ActionResult.Failed("NO_PERMISSION", e.message ?: "READ_MEDIA_AUDIO required")
        } catch (e: IllegalArgumentException) {
            player.release()
            ActionResult.Failed("INVALID_SOURCE", e.message ?: "Invalid audio source: $source")
        } catch (e: Exception) {
            player.release()
            ActionResult.Failed("PLAY_FAILED", e.message ?: "Failed to play audio")
        }
    }
}
