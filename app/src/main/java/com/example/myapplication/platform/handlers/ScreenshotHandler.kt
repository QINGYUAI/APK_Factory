// Author: PKQ32
// Created: 2026-07-11
// Description: 截图动作处理器（一期桩）：需 MediaProjection 用户同意 UI

package com.example.myapplication.platform.handlers

import com.example.myapplication.domain.ActionHandler
import com.example.myapplication.domain.ActionResult
import com.example.myapplication.domain.model.ActionTypes

class ScreenshotHandler : ActionHandler {
    override val type = ActionTypes.SCREENSHOT

    override fun validate(params: Map<String, String>): String? = null

    override suspend fun execute(params: Map<String, String>): ActionResult {
        return ActionResult.Failed(
            "NEEDS_UI",
            "截图需要额外的系统授权界面，一期暂未完成，请稍后再用",
        )
    }
}
