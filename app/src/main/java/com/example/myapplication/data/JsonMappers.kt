// Author: PKQ32
// Created: 2026-07-11
// Description: ActionStep / Trigger 与 JSON 字符串互转

package com.example.myapplication.data

import com.example.myapplication.domain.FailurePolicy
import com.example.myapplication.domain.model.ActionStep
import com.example.myapplication.domain.model.Trigger
import org.json.JSONArray
import org.json.JSONObject

object JsonMappers {
    fun stepsToJson(steps: List<ActionStep>): String {
        val arr = JSONArray()
        steps.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id)
                put("type", s.type)
                put("order", s.order)
                put("onFailure", s.onFailure.name)
                put("params", paramsToJson(s.params))
            })
        }
        return arr.toString()
    }

    fun stepsFromJson(json: String): List<ActionStep> {
        val arr = JSONArray(json.ifBlank { "[]" })
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    ActionStep(
                        id = o.getString("id"),
                        type = o.getString("type"),
                        params = paramsFromJson(o.optJSONObject("params")),
                        order = o.getInt("order"),
                        onFailure = FailurePolicy.valueOf(o.optString("onFailure", "ABORT")),
                    )
                )
            }
        }
    }

    fun triggersToJson(triggers: List<Trigger>): String {
        val arr = JSONArray()
        triggers.forEach { t ->
            arr.put(JSONObject().apply {
                put("id", t.id)
                put("type", t.type)
                put("enabled", t.enabled)
                put("params", paramsToJson(t.params))
            })
        }
        return arr.toString()
    }

    fun triggersFromJson(json: String): List<Trigger> {
        val arr = JSONArray(json.ifBlank { "[]" })
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    Trigger(
                        id = o.getString("id"),
                        type = o.getString("type"),
                        params = paramsFromJson(o.optJSONObject("params")),
                        enabled = o.optBoolean("enabled", true),
                    )
                )
            }
        }
    }

    private fun paramsToJson(params: Map<String, String>): JSONObject =
        JSONObject().apply {
            params.forEach { (key, value) -> put(key, value) }
        }

    private fun paramsFromJson(paramsObj: JSONObject?): Map<String, String> {
        if (paramsObj == null) return emptyMap()
        return paramsObj.keys().asSequence().associateWith { paramsObj.getString(it) }
    }
}
