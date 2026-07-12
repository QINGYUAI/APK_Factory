// Author: PKQ32
// Created: 2026-07-12
// Description: Handler 中文失败文案与校验提示统一

package com.example.myapplication.platform.handlers

object ActionMessages {
    const val ENABLED_REQUIRED = "请填写 enabled（true/false）"
    const val ENABLED_BOOL = "enabled 只能是 true 或 false"
    const val MILLIS_REQUIRED = "请填写毫秒数 millis"
    const val MILLIS_NUMBER = "millis 必须是数字"
    const val MILLIS_NON_NEG = "millis 必须大于等于 0"
}
