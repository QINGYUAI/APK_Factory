// Author: PKQ32
// Created: 2026-07-11
// Description: 串行动作链执行器，支持失败策略 ABORT / SKIP

package com.example.myapplication.engine

import com.example.myapplication.domain.ActionResult
import com.example.myapplication.domain.FailurePolicy
import com.example.myapplication.domain.model.ActionStep

data class StepOutcome(
    val stepId: String,
    val type: String,
    val result: ActionResult,
)

data class RunReport(
    val success: Boolean,
    val stepOutcomes: List<StepOutcome>,
)

class ActionExecutor(private val registry: ActionRegistry) {
    suspend fun run(steps: List<ActionStep>): RunReport {
        val outcomes = mutableListOf<StepOutcome>()
        for (step in steps.sortedBy { it.order }) {
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
