// Author: PKQ32
// Created: 2026-07-11
// Description: ActionStep / Trigger / MiniApp 与 JSON 互转

package com.example.myapplication.data

import com.example.myapplication.domain.FailurePolicy
import com.example.myapplication.domain.model.ActionStep
import com.example.myapplication.domain.model.MiniApp
import com.example.myapplication.domain.model.Trigger
import org.json.JSONArray
import org.json.JSONObject

object JsonMappers {
    fun stepsToJson(steps: List<ActionStep>): String = stepsToArray(steps).toString()

    private fun stepsToArray(steps: List<ActionStep>): JSONArray {
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
        return arr
    }

    fun stepsFromJson(json: String): List<ActionStep> {
        val arr = runCatching { JSONArray(json.ifBlank { "[]" }) }.getOrNull() ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                // 单条损坏不应导致整份配置不可用
                val o = arr.optJSONObject(i) ?: continue
                val id = o.optString("id").takeIf { it.isNotBlank() } ?: continue
                val type = o.optString("type").takeIf { it.isNotBlank() } ?: continue
                add(
                    ActionStep(
                        id = id,
                        type = type,
                        params = paramsFromJson(o.optJSONObject("params")),
                        order = o.optInt("order", i),
                        onFailure = runCatching {
                            FailurePolicy.valueOf(o.optString("onFailure", "ABORT"))
                        }.getOrDefault(FailurePolicy.ABORT),
                    ),
                )
            }
        }
    }

    fun triggersToJson(triggers: List<Trigger>): String = triggersToArray(triggers).toString()

    private fun triggersToArray(triggers: List<Trigger>): JSONArray {
        val arr = JSONArray()
        triggers.forEach { t ->
            arr.put(JSONObject().apply {
                put("id", t.id)
                put("type", t.type)
                put("enabled", t.enabled)
                put("params", paramsToJson(t.params))
            })
        }
        return arr
    }

    fun triggersFromJson(json: String): List<Trigger> {
        val arr = runCatching { JSONArray(json.ifBlank { "[]" }) }.getOrNull() ?: return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.optString("id").takeIf { it.isNotBlank() } ?: continue
                val type = o.optString("type").takeIf { it.isNotBlank() } ?: continue
                add(
                    Trigger(
                        id = id,
                        type = type,
                        params = paramsFromJson(o.optJSONObject("params")),
                        enabled = o.optBoolean("enabled", true),
                    ),
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
        // 兼容手写/旧版本配置里的数字、布尔值
        return paramsObj.keys().asSequence().associateWith { key ->
            paramsObj.opt(key)?.toString().orEmpty()
        }
    }

    /** 完整小应用序列化（供导出 APK / Runner 读取）。 */
    fun miniAppToJson(app: MiniApp): String {
        return JSONObject().apply {
            put("id", app.id)
            put("name", app.name)
            put("iconKey", app.iconKey)
            put("enabled", app.enabled)
            put("runOnHostOpen", app.runOnHostOpen)
            put("createdAt", app.createdAt)
            put("updatedAt", app.updatedAt)
            put("steps", stepsToArray(app.steps))
            put("triggers", triggersToArray(app.triggers))
        }.toString(2)
    }

    fun miniAppFromJson(json: String): MiniApp {
        val o = JSONObject(json)
        return MiniApp(
            id = o.getString("id"),
            name = o.optString("name", "迷你应用"),
            iconKey = o.optString("iconKey", "default"),
            enabled = o.optBoolean("enabled", true),
            runOnHostOpen = o.optBoolean("runOnHostOpen", false),
            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
            steps = stepsFromJson(o.optJSONArray("steps")?.toString() ?: "[]"),
            triggers = triggersFromJson(o.optJSONArray("triggers")?.toString() ?: "[]"),
        )
    }
}
