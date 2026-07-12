// Author: PKQ32
// Created: 2026-07-11
// Description: 串行动作链执行器，支持失败策略 ABORT / SKIP，步骤间可响应停止

package com.example.myapplication.engine

import com.example.myapplication.domain.ActionResult
import com.example.myapplication.domain.FailurePolicy
import com.example.myapplication.domain.model.ActionStep
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

data class StepOutcome(
    val stepId: String,
    val type: String,
    val result: ActionResult,
)

data class RunReport(
    val success: Boolean,
    val stepOutcomes: List<StepOutcome>,
    /** 是否因用户停止而中断 */
    val stopped: Boolean = false,
)

class ActionExecutor(private val registry: ActionRegistry) {
    suspend fun run(steps: List<ActionStep>): RunReport {
        val outcomes = mutableListOf<StepOutcome>()
        try {
            for (step in steps.sortedBy { it.order }) {
                coroutineContext.ensureActive()
                val handler = registry.get(step.type)
                val result = if (handler == null) {
                    ActionResult.Failed("UNKNOWN_TYPE", "Unknown action: ${step.type}")
                } else {
                    val err = handler.validate(step.params)
                    if (err != null) ActionResult.Failed("INVALID_PARAMS", err)
                    else handler.execute(step.params)
                }
                outcomes += StepOutcome(step.id, step.type, result)
                if (result is ActionResult.Failed && step.onFailure == FailurePolicy.ABORT) {
                    return RunReport(success = false, stepOutcomes = outcomes)
                }
            }
        } catch (e: CancellationException) {
            outcomes += StepOutcome(
                stepId = "-",
                type = "stop",
                result = ActionResult.Failed("STOPPED", "用户已停止"),
            )
            return RunReport(success = false, stepOutcomes = outcomes, stopped = true)
        }
        val success = outcomes.all { outcome ->
            when (val r = outcome.result) {
                is ActionResult.Success -> true
                is ActionResult.Failed -> {
                    val policy = steps.first { it.id == outcome.stepId }.onFailure
                    policy == FailurePolicy.SKIP
                }
            }
        }
        return RunReport(success = success, stepOutcomes = outcomes)
    }
}
