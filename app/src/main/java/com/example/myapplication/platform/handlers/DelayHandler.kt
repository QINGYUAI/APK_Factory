// Author: PKQ32
// Created: 2026-07-11
// Description: 延时动作处理器，按 millis 暂停执行

package com.example.myapplication.platform.handlers

import com.example.myapplication.domain.ActionHandler
import com.example.myapplication.domain.ActionResult
import com.example.myapplication.domain.model.ActionTypes
import kotlinx.coroutines.delay

class DelayHandler : ActionHandler {
    override val type = ActionTypes.DELAY

    override fun validate(params: Map<String, String>): String? {
        val ms = params["millis"]?.toLongOrNull() ?: return "millis required"
        return if (ms < 0) "millis must be >= 0" else null
    }

    override suspend fun execute(params: Map<String, String>): ActionResult {
        delay(params.getValue("millis").toLong())
        return ActionResult.Success
    }
}
