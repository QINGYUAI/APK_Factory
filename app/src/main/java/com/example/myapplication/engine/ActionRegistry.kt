// Author: PKQ32
// Created: 2026-07-11
// Description: 动作处理器注册表，按类型注册与查询 ActionHandler

package com.example.myapplication.engine

import com.example.myapplication.domain.ActionHandler

class ActionRegistry {
    private val handlers = linkedMapOf<String, ActionHandler>()

    fun register(handler: ActionHandler) {
        handlers[handler.type] = handler
    }

    fun get(type: String): ActionHandler? = handlers[type]

    fun allTypes(): Set<String> = handlers.keys
}
