// Author: PKQ32
// Created: 2026-07-11
// Description: ActionExecutor 失败策略（ABORT / SKIP）单测

package com.example.myapplication.engine

import com.example.myapplication.domain.ActionHandler
import com.example.myapplication.domain.ActionResult
import com.example.myapplication.domain.FailurePolicy
import com.example.myapplication.domain.model.ActionStep
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionExecutorTest {
    private fun handler(type: String, result: ActionResult) = object : ActionHandler {
        override val type = type
        override fun validate(params: Map<String, String>) = null
        override suspend fun execute(params: Map<String, String>) = result
    }

    @Test
    fun abortStopsChain() = runBlocking {
        val registry = ActionRegistry().apply {
            register(handler("ok", ActionResult.Success))
            register(handler("bad", ActionResult.Failed("E", "fail")))
            register(handler("later", ActionResult.Success))
        }
        val exec = ActionExecutor(registry)
        val report = exec.run(
            listOf(
                ActionStep("1", "ok", order = 0),
                ActionStep("2", "bad", order = 1, onFailure = FailurePolicy.ABORT),
                ActionStep("3", "later", order = 2),
            )
        )
        assertFalse(report.success)
        assertEquals(2, report.stepOutcomes.size)
    }

    @Test
    fun skipContinuesChain() = runBlocking {
        val registry = ActionRegistry().apply {
            register(handler("bad", ActionResult.Failed("E", "fail")))
            register(handler("ok", ActionResult.Success))
        }
        val exec = ActionExecutor(registry)
        val report = exec.run(
            listOf(
                ActionStep("1", "bad", order = 0, onFailure = FailurePolicy.SKIP),
                ActionStep("2", "ok", order = 1),
            )
        )
        assertTrue(report.success)
        assertEquals(2, report.stepOutcomes.size)
    }
}
