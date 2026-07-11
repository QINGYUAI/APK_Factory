// Author: PKQ32
// Created: 2026-07-11
// Description: 动作执行结果类型与动作处理器接口

package com.example.myapplication.domain

sealed class ActionResult {
    data object Success : ActionResult()
    data class Failed(
        val code: String,
        val message: String,
    ) : ActionResult()
}

interface ActionHandler {
    val type: String
    fun validate(params: Map<String, String>): String?
    suspend fun execute(params: Map<String, String>): ActionResult
}
