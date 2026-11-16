// Author: PKQ32
// Created: 2026-07-11
// Description: 动作参数校验单测：直接测真实 DelayHandler，避免与实现脱节

package com.example.myapplication.domain

import com.example.myapplication.platform.handlers.DelayHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActionStepValidationTest {

    private val handler = DelayHandler()

    @Test
    fun delay_requiresMillis() {
        assertEquals("需要 millis 参数（毫秒）", handler.validate(emptyMap()))
        assertEquals("需要 millis 参数（毫秒）", handler.validate(mapOf("millis" to "abc")))
    }

    @Test
    fun delay_rejectsNegativeMillis() {
        assertEquals("millis 必须 ≥ 0", handler.validate(mapOf("millis" to "-1")))
    }

    @Test
    fun delay_acceptsZeroAndPositive() {
        assertNull(handler.validate(mapOf("millis" to "0")))
        assertNull(handler.validate(mapOf("millis" to "100")))
    }
}
