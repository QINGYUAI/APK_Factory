// Author: PKQ32
// Created: 2026-07-11
// Description: 动作步骤参数校验单测（delay millis 规则）

package com.example.myapplication.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActionStepValidationTest {
    @Test
    fun delay_requiresNonNegativeMillis() {
        val handler = object : ActionHandler {
            override val type = "delay"
            override fun validate(params: Map<String, String>): String? {
                val ms = params["millis"]?.toLongOrNull()
                return when {
                    ms == null -> "millis required"
                    ms < 0 -> "millis must be >= 0"
                    else -> null
                }
            }
            override suspend fun execute(params: Map<String, String>) = ActionResult.Success
        }
        assertEquals("millis required", handler.validate(emptyMap()))
        assertEquals("millis must be >= 0", handler.validate(mapOf("millis" to "-1")))
        assertNull(handler.validate(mapOf("millis" to "100")))
    }
}
