// Author: PKQ32
// Created: 2026-11-16
// Description: 配置序列化单测：往返一致、脏数据容错、旧格式兼容

package com.example.myapplication.data

import com.example.myapplication.domain.FailurePolicy
import com.example.myapplication.domain.model.ActionStep
import com.example.myapplication.domain.model.MiniApp
import com.example.myapplication.domain.model.Trigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonMappersTest {

    private val sample = MiniApp(
        id = "app-1",
        name = "晨间流程",
        iconKey = "sun",
        enabled = true,
        runOnHostOpen = true,
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_100_000L,
        steps = listOf(
            ActionStep(
                id = "s1",
                type = "delay",
                params = mapOf("millis" to "500"),
                order = 0,
                onFailure = FailurePolicy.SKIP,
            ),
            ActionStep(id = "s2", type = "vibrate", order = 1),
        ),
        triggers = listOf(
            Trigger(id = "t1", type = "dailyAt", params = mapOf("hour" to "7", "minute" to "30")),
            Trigger(id = "t2", type = "event", enabled = false),
        ),
    )

    @Test
    fun miniApp_roundTripsThroughJson() {
        val restored = JsonMappers.miniAppFromJson(JsonMappers.miniAppToJson(sample))
        assertEquals(sample, restored)
    }

    @Test
    fun steps_skipMalformedEntriesButKeepValidOnes() {
        val json = """
            [
              {"id":"ok","type":"delay","order":0,"onFailure":"SKIP","params":{"millis":"1"}},
              {"type":"vibrate","order":1},
              "not-an-object",
              {"id":"bad-policy","type":"vibrate","order":2,"onFailure":"WHATEVER"}
            ]
        """.trimIndent()

        val steps = JsonMappers.stepsFromJson(json)
        assertEquals(listOf("ok", "bad-policy"), steps.map { it.id })
        assertEquals(FailurePolicy.SKIP, steps[0].onFailure)
        // 未知策略回退为默认 ABORT
        assertEquals(FailurePolicy.ABORT, steps[1].onFailure)
    }

    @Test
    fun triggers_skipEntriesWithoutIdOrType() {
        val json = """
            [
              {"id":"t1","type":"dailyAt","params":{"hour":"8"}},
              {"id":"no-type"},
              {"type":"event"}
            ]
        """.trimIndent()

        val triggers = JsonMappers.triggersFromJson(json)
        assertEquals(1, triggers.size)
        assertEquals("t1", triggers.single().id)
        assertTrue(triggers.single().enabled)
    }

    @Test
    fun brokenJson_degradesToEmptyListInsteadOfCrashing() {
        assertTrue(JsonMappers.stepsFromJson("{not-a-json-array").isEmpty())
        assertTrue(JsonMappers.triggersFromJson("").isEmpty())
    }

    @Test
    fun params_acceptNumbersAndBooleansFromHandWrittenConfig() {
        val json = """
            {
              "id": "legacy",
              "name": "旧配置",
              "steps": [{"id":"s1","type":"setVolume","order":0,"params":{"level":3}}],
              "triggers": [{"id":"t1","type":"schedule","params":{"intervalMinutes":15,"enabled":true}}]
            }
        """.trimIndent()

        val app = JsonMappers.miniAppFromJson(json)
        assertEquals("3", app.steps.single().params["level"])
        assertEquals("15", app.triggers.single().params["intervalMinutes"])
        assertEquals("true", app.triggers.single().params["enabled"])
        assertEquals("旧配置", app.name)
        assertTrue(app.enabled)
    }

    @Test
    fun missingOptionalFields_useDefaults() {
        val app = JsonMappers.miniAppFromJson("""{"id":"minimal"}""")
        assertEquals("minimal", app.id)
        assertEquals("迷你应用", app.name)
        assertEquals("default", app.iconKey)
        assertTrue(app.steps.isEmpty())
        assertTrue(app.triggers.isEmpty())
    }
}
