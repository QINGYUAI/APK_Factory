// Author: PKQ32
// Created: 2026-11-16
// Description: 截图动作一期桩行为：校验通过但执行返回 NEEDS_UI，不崩溃

package com.example.myapplication.platform.handlers

import com.example.myapplication.domain.ActionResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotHandlerTest {

    @Test
    fun validate_acceptsAnyParams() {
        assertNull(ScreenshotHandler().validate(emptyMap()))
    }

    @Test
    fun execute_reportsNeedsUiInsteadOfCrashing() = runBlocking {
        val result = ScreenshotHandler().execute(emptyMap())
        assertTrue(result is ActionResult.Failed)
        assertEquals("NEEDS_UI", (result as ActionResult.Failed).code)
    }
}
