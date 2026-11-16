// Author: PKQ32
// Created: 2026-07-11
// Description: ActionExecutor 顺序执行、失败策略、未知类型与异常兜底单测

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

    private fun throwingHandler(type: String, error: Throwable) = object : ActionHandler {
        override val type = type
        override fun validate(params: Map<String, String>) = null
        override suspend fun execute(params: Map<String, String>): ActionResult = throw error
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

    @Test
    fun ordersStepsByOrderField() = runBlocking {
        val registry = ActionRegistry().apply {
            register(handler("ok", ActionResult.Success))
        }
        val report = ActionExecutor(registry).run(
            listOf(
                ActionStep("second", "ok", order = 2),
                ActionStep("first", "ok", order = 1),
            )
        )
        assertEquals(listOf("first", "second"), report.stepOutcomes.map { it.stepId })
    }

    @Test
    fun unknownType_failsWithoutCrash() = runBlocking {
        val report = ActionExecutor(ActionRegistry()).run(
            listOf(ActionStep("1", "doesNotExist", order = 0)),
        )
        assertFalse(report.success)
        val failure = report.stepOutcomes.single().result as ActionResult.Failed
        assertEquals("UNKNOWN_TYPE", failure.code)
    }

    @Test
    fun handlerException_isConvertedToFailure() = runBlocking {
        val registry = ActionRegistry().apply {
            register(throwingHandler("boom", IllegalStateException("kaboom")))
        }
        val report = ActionExecutor(registry).run(
            listOf(ActionStep("1", "boom", order = 0, onFailure = FailurePolicy.ABORT)),
        )
        assertFalse(report.success)
        val failure = report.stepOutcomes.single().result as ActionResult.Failed
        assertEquals("EXCEPTION", failure.code)
        assertTrue(failure.message.contains("kaboom"))
    }

    @Test
    fun handlerException_withSkipPolicy_continues() = runBlocking {
        val registry = ActionRegistry().apply {
            register(throwingHandler("boom", RuntimeException("bad")))
            register(handler("ok", ActionResult.Success))
        }
        val report = ActionExecutor(registry).run(
            listOf(
                ActionStep("1", "boom", order = 0, onFailure = FailurePolicy.SKIP),
                ActionStep("2", "ok", order = 1),
            ),
        )
        assertEquals(2, report.stepOutcomes.size)
        assertTrue(report.success)
    }

    @Test
    fun emptyChain_isSuccess() = runBlocking {
        val report = ActionExecutor(ActionRegistry()).run(emptyList())
        assertTrue(report.success)
        assertTrue(report.stepOutcomes.isEmpty())
    }
}
