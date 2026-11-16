// Author: PKQ32
// Created: 2026-11-16
// Description: MiniAppRunner 单飞、停止语义、失败与日志落库单测（假 DAO）

package com.example.myapplication.engine

import com.example.myapplication.data.MiniAppRepository
import com.example.myapplication.data.RunLogRepository
import com.example.myapplication.data.db.MiniAppDao
import com.example.myapplication.data.db.MiniAppEntity
import com.example.myapplication.data.db.RunLogDao
import com.example.myapplication.data.db.RunLogEntity
import com.example.myapplication.domain.ActionHandler
import com.example.myapplication.domain.ActionResult
import com.example.myapplication.domain.model.ActionStep
import com.example.myapplication.domain.model.MiniApp
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniAppRunnerTest {

    private class FakeMiniAppDao : MiniAppDao {
        val apps = linkedMapOf<String, MiniAppEntity>()
        override fun observeAll(): Flow<List<MiniAppEntity>> = flowOf(apps.values.toList())
        override suspend fun getAll(): List<MiniAppEntity> = apps.values.toList()
        override suspend fun getById(id: String): MiniAppEntity? = apps[id]
        override suspend fun getHostOpenApp(): MiniAppEntity? =
            apps.values.firstOrNull { it.runOnHostOpen && it.enabled }
        override suspend fun upsert(entity: MiniAppEntity) {
            apps[entity.id] = entity
        }
        override suspend fun clearHostOpenFlags(exceptId: String, now: Long) {
            apps.keys.toList().forEach { id ->
                val entity = apps.getValue(id)
                if (id != exceptId && entity.runOnHostOpen) {
                    apps[id] = entity.copy(runOnHostOpen = false, updatedAt = now)
                }
            }
        }
        override suspend fun delete(id: String) {
            apps.remove(id)
        }
    }

    private class FakeRunLogDao : RunLogDao {
        val logs = mutableListOf<RunLogEntity>()
        var pruneCount = 0
        override suspend fun insert(entity: RunLogEntity) {
            logs += entity
        }
        override fun observeForApp(miniAppId: String, limit: Int): Flow<List<RunLogEntity>> =
            flowOf(logs.filter { it.miniAppId == miniAppId }.take(limit))
        override suspend fun prune(miniAppId: String, keep: Int) {
            pruneCount++
        }
        override suspend fun deleteForApp(miniAppId: String) {
            logs.removeAll { it.miniAppId == miniAppId }
        }
    }

    private class SlowHandler(
        private val delayMs: Long,
        override val type: String = "slow",
    ) : ActionHandler {
        override fun validate(params: Map<String, String>): String? = null
        override suspend fun execute(params: Map<String, String>): ActionResult {
            delay(delayMs)
            return ActionResult.Success
        }
    }

    private class Fake {
        val appDao = FakeMiniAppDao()
        val logDao = FakeRunLogDao()
        val repo = MiniAppRepository(appDao)
        val logRepo = RunLogRepository(logDao)
    }

    private fun app(id: String = "a", enabled: Boolean = true) = MiniApp(
        id = id,
        name = "测试",
        enabled = enabled,
        createdAt = 1L,
        updatedAt = 1L,
        steps = listOf(ActionStep("s1", "slow", order = 0)),
    )

    private fun runner(
        fake: Fake,
        handler: ActionHandler,
        debounceMs: Long = 3000,
        maxQueued: Int = MiniAppRunner.DEFAULT_MAX_QUEUED,
    ): MiniAppRunner {
        val registry = ActionRegistry().apply { register(handler) }
        return MiniAppRunner(
            miniApps = fake.repo,
            runLogs = fake.logRepo,
            executor = ActionExecutor(registry),
            debouncer = Debouncer(debounceMs),
            onStopMedia = {},
            maxQueued = maxQueued,
        )
    }

    @Test
    fun successfulRun_persistsLogAndPrunes() = runBlocking {
        val fake = Fake()
        fake.repo.save(app())
        val runner = runner(fake, SlowHandler(delayMs = 0))

        val report = runner.run("a", "manual")

        assertNotNull(report)
        assertTrue(report!!.success)
        assertEquals(1, fake.logDao.logs.size)
        assertEquals(1, fake.logDao.pruneCount)
        assertNull(runner.runningId.value)
    }

    @Test
    fun stop_cancelsActionChainWithoutCancellingCaller() = runBlocking {
        val fake = Fake()
        fake.repo.save(app())
        val runner = runner(fake, SlowHandler(delayMs = 10_000))

        var report: RunReport? = null
        val caller = launch {
            report = runner.run("a", "manual")
        }
        delay(200)
        assertEquals("a", runner.runningId.value)

        runner.stop()
        withTimeout(2_000) { caller.join() }

        assertFalse(caller.isCancelled)
        assertNotNull(report)
        assertTrue(report!!.stopped)
        assertFalse(report!!.success)
        assertNull(runner.runningId.value)
    }

    @Test
    fun secondRunWhileBusy_isSkipped() = runBlocking {
        val fake = Fake()
        fake.repo.save(app())
        val runner = runner(fake, SlowHandler(delayMs = 500))

        var second: RunReport? = null
        val first = launch { runner.run("a", "manual") }
        delay(100)
        second = runner.run("a", "event")
        withTimeout(2_000) { first.join() }

        assertNull(second)
    }

    @Test
    fun queuedRun_waitsInsteadOfSkipping() = runBlocking {
        val fake = Fake()
        fake.repo.save(app("a"))
        fake.repo.save(app("b"))
        val runner = runner(fake, SlowHandler(delayMs = 300))

        val first = launch { runner.run("a", "manual") }
        delay(100)
        val queued = async { runner.runQueued("b", "schedule") }

        withTimeout(5_000) { first.join() }
        val report = withTimeout(5_000) { queued.await() }

        assertNotNull(report)
        assertTrue(report!!.success)
        assertEquals(2, fake.logDao.logs.size)
        assertNull(runner.runningId.value)
    }

    @Test
    fun queuedRun_dedupesSameTriggerWhilePending() = runBlocking {
        val fake = Fake()
        fake.repo.save(app("a"))
        fake.repo.save(app("b"))
        val runner = runner(fake, SlowHandler(delayMs = 300))

        val first = launch { runner.run("a", "manual") }
        delay(100)
        val queued = async { runner.runQueued("b", "event") }
        delay(50)
        val duplicate = withTimeout(500) { runner.runQueued("b", "event") }

        assertNull(duplicate)
        assertNotNull(withTimeout(5_000) { queued.await() })
        withTimeout(5_000) { first.join() }
    }

    @Test
    fun queuedRun_dropsWhenQueueIsFull() = runBlocking {
        val fake = Fake()
        listOf("a", "b", "c").forEach { fake.repo.save(app(it)) }
        val runner = runner(fake, SlowHandler(delayMs = 300), maxQueued = 1)

        val first = launch { runner.run("a", "manual") }
        delay(100)
        val queued = async { runner.runQueued("b", "event") }
        delay(50)
        val overflow = withTimeout(500) { runner.runQueued("c", "event") }

        assertNull(overflow)
        assertNotNull(withTimeout(5_000) { queued.await() })
        withTimeout(5_000) { first.join() }
    }

    @Test
    fun disabledApp_returnsDisabledWithoutRunning() = runBlocking {
        val fake = Fake()
        fake.repo.save(app(enabled = false))
        val runner = runner(fake, SlowHandler(delayMs = 0))

        val report = runner.run("a", "manual")

        assertNotNull(report)
        assertFalse(report!!.success)
        val failure = report.stepOutcomes.single().result as ActionResult.Failed
        assertEquals("DISABLED", failure.code)
        assertTrue(fake.logDao.logs.isEmpty())
    }

    @Test
    fun missingApp_returnsNotFound() = runBlocking {
        val fake = Fake()
        val runner = runner(fake, SlowHandler(delayMs = 0))

        val report = runner.run("missing", "manual")

        val failure = report!!.stepOutcomes.single().result as ActionResult.Failed
        assertEquals("NOT_FOUND", failure.code)
    }

    @Test
    fun debounce_blocksRapidRepeat() = runBlocking {
        val fake = Fake()
        fake.repo.save(app())
        val runner = runner(fake, SlowHandler(delayMs = 0), debounceMs = 5_000)

        assertNotNull(runner.run("a", "manual"))
        assertNull(runner.run("a", "manual"))
    }
}
