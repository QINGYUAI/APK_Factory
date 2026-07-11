# 应用工厂（MiniApp Factory）一期实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有 Compose 工程中实现「应用工厂」一期：可创建/编辑小应用、串行执行手机动作、支持手动/定时/事件/打开 App 触发，并提供桌面快捷方式与权限中心。

**Architecture:** Domain 模型 + Room 持久化 + ActionRegistry/ActionExecutor 编排引擎 + Platform 适配层执行系统 API + Compose UI。触发由 TriggerManager 统一注册（WorkManager / Broadcast / UsageStats / 宿主启动）。二期导出 APK 不在本计划内。

**Tech Stack:** Kotlin、Jetpack Compose、Material3、Room、WorkManager、AndroidX Core、JUnit4（`app/src/test`）

**Spec:** `docs/superpowers/specs/2026-07-11-miniapp-factory-design.md`

---

## 文件结构（一期，单模块分包）

```text
app/src/main/java/com/example/myapplication/
  MainActivity.kt
  MiniAppApplication.kt
  domain/
    model/MiniAppModels.kt          # MiniApp, ActionStep, Trigger, RunLog, enums
    ActionHandler.kt                # 接口 + ActionResult
    FailurePolicy.kt
  engine/
    ActionRegistry.kt
    ActionExecutor.kt
    TriggerManager.kt
    Debouncer.kt
  data/
    db/AppDatabase.kt
    db/Entities.kt
    db/Daos.kt
    db/Converters.kt
    MiniAppRepository.kt
    RunLogRepository.kt
  platform/
    PlatformActionContext.kt
    handlers/
      VibrateHandler.kt
      DelayHandler.kt
      SetVolumeHandler.kt
      PlayMusicHandler.kt
      FlashlightHandler.kt
      SetBrightnessHandler.kt
      OpenAppHandler.kt
      WifiHandler.kt
      BluetoothHandler.kt
      SetRingerModeHandler.kt
      ScreenshotHandler.kt
      NotifyHandler.kt
    AppOpenMonitor.kt               # UsageStats 轮询
    ShortcutHelper.kt
    EventReceivers.kt
  ui/
    navigation/AppNav.kt
    list/MiniAppListScreen.kt
    editor/MiniAppEditorScreen.kt
    run/RunStatusScreen.kt
    permissions/PermissionCenterScreen.kt
    theme/…（沿用现有）
  worker/ScheduleMiniAppWorker.kt
  di/ServiceLocator.kt              # 一期简易定位，不做 Hilt

app/src/test/java/com/example/myapplication/
  engine/ActionExecutorTest.kt
  engine/DebouncerTest.kt
  domain/ActionStepValidationTest.kt

app/src/main/AndroidManifest.xml    # 权限、Application、Receiver、deep link
app/build.gradle.kts                # Room / WorkManager 依赖
gradle/libs.versions.toml
```

---

### Task 1: 引入依赖与 Application 入口

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/example/myapplication/MiniAppApplication.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: 在版本目录增加 Room / WorkManager / Lifecycle 依赖坐标**

在 `gradle/libs.versions.toml` 的 `[versions]` 增加：

```toml
room = "2.7.2"
work = "2.10.2"
ksp = "2.2.10-2.0.2"
```

在 `[libraries]` 增加：

```toml
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "work" }
```

在 `[plugins]` 增加：

```toml
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 2: 修改 app 模块构建脚本**

`app/build.gradle.kts` plugins 增加：

```kotlin
alias(libs.plugins.ksp)
```

`dependencies` 增加：

```kotlin
implementation(libs.androidx.room.runtime)
implementation(libs.androidx.room.ktx)
ksp(libs.androidx.room.compiler)
implementation(libs.androidx.work.runtime.ktx)
```

根 `build.gradle.kts` 增加（若需要 apply false）：

```kotlin
alias(libs.plugins.ksp) apply false
```

- [ ] **Step 3: 创建 Application 类**

```kotlin
package com.example.myapplication

import android.app.Application
import com.example.myapplication.di.ServiceLocator

class MiniAppApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
```

（`ServiceLocator` 在 Task 4 实现；本步可先建空壳 `di/ServiceLocator.kt`：`object ServiceLocator { fun init(app: Application) {} }`）

- [ ] **Step 4: Manifest 注册 Application**

在 `<application>` 增加 `android:name=".MiniAppApplication"`。

- [ ] **Step 5: 同步 Gradle 确认能编译**

Run: `.\gradlew.bat :app:assembleDebug`

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add gradle/libs.versions.toml build.gradle.kts app/build.gradle.kts app/src/main/AndroidManifest.xml app/src/main/java/com/example/myapplication/MiniAppApplication.kt app/src/main/java/com/example/myapplication/di/ServiceLocator.kt
git commit -m "$(cat <<'EOF'
chore: 引入 Room/WorkManager 并注册 Application

EOF
)"
```

---

### Task 2: Domain 模型与失败策略

**Files:**
- Create: `app/src/main/java/com/example/myapplication/domain/FailurePolicy.kt`
- Create: `app/src/main/java/com/example/myapplication/domain/ActionHandler.kt`
- Create: `app/src/main/java/com/example/myapplication/domain/model/MiniAppModels.kt`
- Test: `app/src/test/java/com/example/myapplication/domain/ActionStepValidationTest.kt`

- [ ] **Step 1: 写失败策略与结果类型**

```kotlin
package com.example.myapplication.domain

enum class FailurePolicy { SKIP, ABORT }

sealed class ActionResult {
    data object Success : ActionResult()
    data class Failed(
        val code: String,
        val message: String,
    ) : ActionResult()
}

interface ActionHandler {
    val type: String
    fun validate(params: Map<String, String>): String?
    suspend fun execute(params: Map<String, String>): ActionResult
}
```

- [ ] **Step 2: 写核心模型**

```kotlin
package com.example.myapplication.domain.model

import com.example.myapplication.domain.FailurePolicy

data class MiniApp(
    val id: String,
    val name: String,
    val iconKey: String = "default",
    val enabled: Boolean = true,
    val runOnHostOpen: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val steps: List<ActionStep> = emptyList(),
    val triggers: List<Trigger> = emptyList(),
)

data class ActionStep(
    val id: String,
    val type: String,
    val params: Map<String, String> = emptyMap(),
    val order: Int,
    val onFailure: FailurePolicy = FailurePolicy.ABORT,
)

data class Trigger(
    val id: String,
    val type: String, // manual | schedule | event | appOpened | hostOpened
    val params: Map<String, String> = emptyMap(),
    val enabled: Boolean = true,
)

data class RunLog(
    val id: String,
    val miniAppId: String,
    val startedAt: Long,
    val endedAt: Long?,
    val stepResultsJson: String,
    val success: Boolean,
)

object ActionTypes {
    const val VIBRATE = "vibrate"
    const val PLAY_MUSIC = "playMusic"
    const val SET_VOLUME = "setVolume"
    const val FLASHLIGHT = "flashlight"
    const val SET_BRIGHTNESS = "setBrightness"
    const val OPEN_APP = "openApp"
    const val DELAY = "delay"
    const val WIFI = "wifi"
    const val BLUETOOTH = "bluetooth"
    const val SET_RINGER_MODE = "setRingerMode"
    const val SCREENSHOT = "screenshot"
    const val NOTIFY = "notify"
}

object TriggerTypes {
    const val MANUAL = "manual"
    const val SCHEDULE = "schedule"
    const val EVENT = "event"
    const val APP_OPENED = "appOpened"
    const val HOST_OPENED = "hostOpened"
}
```

- [ ] **Step 3: 写校验单测（delay 必须有非负 millis）**

```kotlin
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
```

- [ ] **Step 4: 运行单测**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.example.myapplication.domain.ActionStepValidationTest`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/myapplication/domain app/src/test/java/com/example/myapplication/domain
git commit -m "$(cat <<'EOF'
feat: 添加小应用 Domain 模型与动作接口

EOF
)"
```

---

### Task 3: ActionRegistry、Debouncer、ActionExecutor（纯逻辑）

**Files:**
- Create: `app/src/main/java/com/example/myapplication/engine/ActionRegistry.kt`
- Create: `app/src/main/java/com/example/myapplication/engine/Debouncer.kt`
- Create: `app/src/main/java/com/example/myapplication/engine/ActionExecutor.kt`
- Test: `app/src/test/java/com/example/myapplication/engine/DebouncerTest.kt`
- Test: `app/src/test/java/com/example/myapplication/engine/ActionExecutorTest.kt`

- [ ] **Step 1: 写 Debouncer 失败单测**

```kotlin
package com.example.myapplication.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DebouncerTest {
    @Test
    fun blocksRapidRepeat() {
        val d = Debouncer(windowMs = 3000)
        assertTrue(d.shouldRun("app1", now = 1000))
        assertFalse(d.shouldRun("app1", now = 2000))
        assertTrue(d.shouldRun("app1", now = 4500))
        assertTrue(d.shouldRun("app2", now = 4600))
    }
}
```

- [ ] **Step 2: 实现 Debouncer**

```kotlin
package com.example.myapplication.engine

class Debouncer(private val windowMs: Long = 3000) {
    private val last = mutableMapOf<String, Long>()

    fun shouldRun(key: String, now: Long = System.currentTimeMillis()): Boolean {
        val prev = last[key]
        if (prev != null && now - prev < windowMs) return false
        last[key] = now
        return true
    }
}
```

- [ ] **Step 3: 实现 ActionRegistry**

```kotlin
package com.example.myapplication.engine

import com.example.myapplication.domain.ActionHandler

class ActionRegistry {
    private val handlers = linkedMapOf<String, ActionHandler>()

    fun register(handler: ActionHandler) {
        handlers[handler.type] = handler
    }

    fun get(type: String): ActionHandler? = handlers[type]

    fun allTypes(): Set<String> = handlers.keys
}
```

- [ ] **Step 4: 写 ActionExecutor 单测（SKIP / ABORT）**

```kotlin
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
```

- [ ] **Step 5: 实现 ActionExecutor**

```kotlin
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
        val success = outcomes.none { it.result is ActionResult.Failed }
        return RunReport(success = success, stepOutcomes = outcomes)
    }
}
```

说明：`skipContinuesChain` 中若第一步 Failed+SKIP、第二步 Success，按「最终 success = 没有任何 Failed」会得到 false。若产品定义「SKIP 后整体仍算成功」，将最后一行改为：

```kotlin
val success = outcomes.none {
    it.result is ActionResult.Failed && steps.first { s -> s.id == it.stepId }.onFailure == FailurePolicy.ABORT
}
```

更清晰的一期约定：**整体 success = 所有步骤均为 Success 或（Failed 且策略为 SKIP）**。请用下面最终逻辑替换 `run` 末尾：

```kotlin
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
```

并保证 ABORT 分支提前 `return RunReport(success = false, …)`。

- [ ] **Step 6: 跑引擎单测**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.example.myapplication.engine.*`

Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/myapplication/engine app/src/test/java/com/example/myapplication/engine
git commit -m "$(cat <<'EOF'
feat: 实现动作注册表、防抖与串行执行器

EOF
)"
```

---

### Task 4: Room 存储与 Repository + ServiceLocator

**Files:**
- Create: `app/src/main/java/com/example/myapplication/data/db/Entities.kt`
- Create: `app/src/main/java/com/example/myapplication/data/db/Converters.kt`
- Create: `app/src/main/java/com/example/myapplication/data/db/Daos.kt`
- Create: `app/src/main/java/com/example/myapplication/data/db/AppDatabase.kt`
- Create: `app/src/main/java/com/example/myapplication/data/MiniAppRepository.kt`
- Create: `app/src/main/java/com/example/myapplication/data/RunLogRepository.kt`
- Modify: `app/src/main/java/com/example/myapplication/di/ServiceLocator.kt`

- [ ] **Step 1: 实体与类型转换**

`Entities.kt`：用一张 `mini_apps` 表存 MiniApp 元数据；`steps_json` / `triggers_json` 用 JSON 字符串；`run_logs` 表存运行日志。

```kotlin
package com.example.myapplication.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mini_apps")
data class MiniAppEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconKey: String,
    val enabled: Boolean,
    val runOnHostOpen: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val stepsJson: String,
    val triggersJson: String,
)

@Entity(tableName = "run_logs")
data class RunLogEntity(
    @PrimaryKey val id: String,
    val miniAppId: String,
    val startedAt: Long,
    val endedAt: Long?,
    val stepResultsJson: String,
    val success: Boolean,
)
```

`Converters` 可省略（已用 String 存 JSON）。用 `org.json.JSONArray`/`JSONObject` 或手写简单编解码函数放在 `JsonMappers.kt`：

```kotlin
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
                put("params", JSONObject(s.params as Map<*, *>))
            })
        }
        return arr.toString()
    }

    fun stepsFromJson(json: String): List<ActionStep> {
        val arr = JSONArray(json)
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val paramsObj = o.optJSONObject("params") ?: JSONObject()
                val params = paramsObj.keys().asSequence().associateWith { paramsObj.getString(it) }
                add(
                    ActionStep(
                        id = o.getString("id"),
                        type = o.getString("type"),
                        params = params,
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
                put("params", JSONObject(t.params as Map<*, *>))
            })
        }
        return arr.toString()
    }

    fun triggersFromJson(json: String): List<Trigger> {
        val arr = JSONArray(json.ifBlank { "[]" })
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val paramsObj = o.optJSONObject("params") ?: JSONObject()
                val params = paramsObj.keys().asSequence().associateWith { paramsObj.getString(it) }
                add(
                    Trigger(
                        id = o.getString("id"),
                        type = o.getString("type"),
                        params = params,
                        enabled = o.optBoolean("enabled", true),
                    )
                )
            }
        }
    }
}
```

- [ ] **Step 2: DAO 与 Database**

```kotlin
package com.example.myapplication.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface MiniAppDao {
    @Query("SELECT * FROM mini_apps ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<MiniAppEntity>>

    @Query("SELECT * FROM mini_apps WHERE id = :id")
    suspend fun getById(id: String): MiniAppEntity?

    @Query("SELECT * FROM mini_apps WHERE runOnHostOpen = 1 AND enabled = 1 LIMIT 1")
    suspend fun getHostOpenApp(): MiniAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MiniAppEntity)

    @Query("DELETE FROM mini_apps WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface RunLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RunLogEntity)

    @Query("SELECT * FROM run_logs WHERE miniAppId = :miniAppId ORDER BY startedAt DESC LIMIT :limit")
    fun observeForApp(miniAppId: String, limit: Int = 20): Flow<List<RunLogEntity>>
}

@Database(entities = [MiniAppEntity::class, RunLogEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun miniAppDao(): MiniAppDao
    abstract fun runLogDao(): RunLogDao
}
```

- [ ] **Step 3: Repository 映射 Entity ↔ Domain**

实现 `MiniAppRepository`：`observeAll` / `get` / `save` / `delete` / `getHostOpenApp`，内部调用 `JsonMappers`。  
实现 `RunLogRepository`：`append` / `observeForApp`。

- [ ] **Step 4: 完善 ServiceLocator**

```kotlin
package com.example.myapplication.di

import android.app.Application
import androidx.room.Room
import com.example.myapplication.data.MiniAppRepository
import com.example.myapplication.data.RunLogRepository
import com.example.myapplication.data.db.AppDatabase
import com.example.myapplication.engine.ActionExecutor
import com.example.myapplication.engine.ActionRegistry
import com.example.myapplication.engine.Debouncer

object ServiceLocator {
    lateinit var db: AppDatabase
        private set
    lateinit var miniApps: MiniAppRepository
        private set
    lateinit var runLogs: RunLogRepository
        private set
    lateinit var registry: ActionRegistry
        private set
    lateinit var executor: ActionExecutor
        private set
    val debouncer = Debouncer()

    fun init(app: Application) {
        db = Room.databaseBuilder(app, AppDatabase::class.java, "miniapp.db").build()
        miniApps = MiniAppRepository(db.miniAppDao())
        runLogs = RunLogRepository(db.runLogDao())
        registry = ActionRegistry()
        // handlers 在 Task 5 注册
        executor = ActionExecutor(registry)
    }
}
```

- [ ] **Step 5: 编译**

Run: `.\gradlew.bat :app:compileDebugKotlin`

Expected: SUCCESS

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/myapplication/data app/src/main/java/com/example/myapplication/di
git commit -m "$(cat <<'EOF'
feat: 添加 Room 持久化与仓库层

EOF
)"
```

---

### Task 5: Platform 基础 Handler（vibrate / delay / setVolume / notify）

**Files:**
- Create: `app/src/main/java/com/example/myapplication/platform/PlatformActionContext.kt`
- Create: handlers 上述四个文件
- Modify: `AndroidManifest.xml`（`VIBRATE`、`POST_NOTIFICATIONS`）
- Modify: `ServiceLocator.init` 注册 handler

- [ ] **Step 1: PlatformActionContext**

```kotlin
package com.example.myapplication.platform

import android.content.Context

class PlatformActionContext(val appContext: Context)
```

Handler 构造注入 `PlatformActionContext`。

- [ ] **Step 2: DelayHandler**

```kotlin
package com.example.myapplication.platform.handlers

import com.example.myapplication.domain.ActionHandler
import com.example.myapplication.domain.ActionResult
import com.example.myapplication.domain.model.ActionTypes
import kotlinx.coroutines.delay

class DelayHandler : ActionHandler {
    override val type = ActionTypes.DELAY
    override fun validate(params: Map<String, String>): String? {
        val ms = params["millis"]?.toLongOrNull() ?: return "millis required"
        return if (ms < 0) "millis must be >= 0" else null
    }
    override suspend fun execute(params: Map<String, String>): ActionResult {
        delay(params.getValue("millis").toLong())
        return ActionResult.Success
    }
}
```

- [ ] **Step 3: VibrateHandler**

使用 `Vibrator` / `VibratorManager`（API 34+），参数 `millis`（默认 200）。Manifest 加 `<uses-permission android:name="android.permission.VIBRATE" />`。

- [ ] **Step 4: SetVolumeHandler**

用 `AudioManager`，参数 `stream=music|ring`、`percent=0..100`。非法 percent 校验失败。

- [ ] **Step 5: NotifyHandler**

创建通知渠道 `miniapp_actions`，参数 `title`/`text`。Manifest 加 `POST_NOTIFICATIONS`；运行时权限在 UI 层申请。

- [ ] **Step 6: 在 ServiceLocator 注册这四个 Handler**

- [ ] **Step 7: 编译 + 真机烟雾（可选）手动调 Executor 跑 vibrate+delay**

- [ ] **Step 8: Commit**

```bash
git commit -m "$(cat <<'EOF'
feat: 实现震动、延时、音量与通知动作

EOF
)"
```

---

### Task 6: 其余 Platform Handler

**Files:** `platform/handlers/` 下剩余：`PlayMusicHandler`、`FlashlightHandler`、`SetBrightnessHandler`、`OpenAppHandler`、`WifiHandler`、`BluetoothHandler`、`SetRingerModeHandler`、`ScreenshotHandler`

每个 Handler 必须：`type` 常量、`validate`、`execute` 返回 `Success` 或 `Failed(code, message)`（不支持时用 `UNSUPPORTED`）。

权限（按需写入 Manifest）：

- `READ_MEDIA_AUDIO`（播音乐）
- `CAMERA` / 手电筒不强制 CAMERA 权限时优先 `CameraManager.setTorchMode`
- `WRITE_SETTINGS`（亮度）
- `BLUETOOTH_CONNECT`、`NEARBY_WIFI_DEVICES` 等按 target 36 声明
- `ACCESS_NOTIFICATION_POLICY`（响铃模式）
- 截图：`MediaProjection` 需 Activity 回调，`ScreenshotHandler` 一期可返回 `Failed("NEEDS_UI", "Screenshot requires user consent UI")` 并在 Run 流程预留挂起请求；若一期来不及做完 MediaProjection 全流程，保持明确失败码，并在权限中心标注「需额外授权」。

- [ ] **Step 1: 实现并注册全部剩余 Handler**
- [ ] **Step 2: 编译通过**
- [ ] **Step 3: Commit**

```bash
git commit -m "$(cat <<'EOF'
feat: 扩展手电筒、亮度、开应用、网络与截图等动作

EOF
)"
```

---

### Task 7: 运行会话 API（供 UI / 触发器调用）

**Files:**
- Create: `app/src/main/java/com/example/myapplication/engine/MiniAppRunner.kt`

- [ ] **Step 1: 实现单飞运行**

```kotlin
package com.example.myapplication.engine

import com.example.myapplication.data.MiniAppRepository
import com.example.myapplication.data.RunLogRepository
import com.example.myapplication.domain.ActionResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class MiniAppRunner(
    private val miniApps: MiniAppRepository,
    private val runLogs: RunLogRepository,
    private val executor: ActionExecutor,
    private val debouncer: Debouncer,
) {
    private val mutex = Mutex()
    @Volatile var runningId: String? = null
        private set

    suspend fun run(miniAppId: String, source: String): RunReport? = mutex.withLock {
        if (!debouncer.shouldRun("$source:$miniAppId")) return null
        val app = miniApps.get(miniAppId) ?: return RunReport(
            success = false,
            stepOutcomes = listOf(
                StepOutcome("-", "-", ActionResult.Failed("NOT_FOUND", "MiniApp not found"))
            )
        )
        if (!app.enabled) return RunReport(
            success = false,
            stepOutcomes = listOf(
                StepOutcome("-", "-", ActionResult.Failed("DISABLED", "MiniApp disabled"))
            )
        )
        runningId = miniAppId
        val started = System.currentTimeMillis()
        return try {
            val report = executor.run(app.steps)
            val arr = JSONArray()
            report.stepOutcomes.forEach { o ->
                arr.put(JSONObject().apply {
                    put("stepId", o.stepId)
                    put("type", o.type)
                    put("ok", o.result is ActionResult.Success)
                    if (o.result is ActionResult.Failed) {
                        put("code", o.result.code)
                        put("message", o.result.message)
                    }
                })
            }
            runLogs.append(
                id = UUID.randomUUID().toString(),
                miniAppId = miniAppId,
                startedAt = started,
                endedAt = System.currentTimeMillis(),
                stepResultsJson = arr.toString(),
                success = report.success,
            )
            report
        } finally {
            runningId = null
        }
    }
}
```

在 `ServiceLocator` 暴露 `runner`。

- [ ] **Step 2: Commit**

```bash
git commit -m "$(cat <<'EOF'
feat: 添加小应用运行会话与日志落库

EOF
)"
```

---

### Task 8: Compose 导航与小应用列表页

**Files:**
- Create: `ui/navigation/AppNav.kt`
- Create: `ui/list/MiniAppListScreen.kt`
- Modify: `MainActivity.kt`
- Modify: `res/values/strings.xml`

- [ ] **Step 1: 依赖导航（若尚未添加）**

`libs.versions.toml` + `app/build.gradle.kts` 增加 `androidx.navigation:navigation-compose`。

- [ ] **Step 2: 列表页**

展示名称、启用开关、运行按钮、进入编辑；FAB 创建默认空小应用（生成 UUID、默认一步 `delay` 100ms 或空 steps）。

- [ ] **Step 3: MainActivity 改为 `AppNav()`，处理 deep link intent extras `miniAppId`**

- [ ] **Step 4: 编译安装，确认列表可创建空应用**

- [ ] **Step 5: Commit**

```bash
git commit -m "$(cat <<'EOF'
feat: 添加小应用列表与导航骨架

EOF
)"
```

---

### Task 9: 编辑器与动作/触发器表单

**Files:**
- Create: `ui/editor/MiniAppEditorScreen.kt`
- Create: `ui/editor/ActionForm.kt`
- Create: `ui/editor/TriggerForm.kt`

- [ ] **Step 1: 编辑名称、`enabled`、`runOnHostOpen`**
- [ ] **Step 2: 步骤列表：按 `ActionRegistry.allTypes()` 添加；上下排序；删除；`onFailure` 切换**
- [ ] **Step 3: 按 type 显示参数字段（vibrate.millis、setVolume.percent 等）**
- [ ] **Step 4: 触发器：添加 schedule（`intervalMinutes`）、event（`action=android.intent.action.ACTION_POWER_CONNECTED` 等）、appOpened（`packageName`）**
- [ ] **Step 5: 保存调用 `miniApps.save`，返回列表**
- [ ] **Step 6: Commit**

```bash
git commit -m "$(cat <<'EOF'
feat: 实现小应用编辑器与动作触发配置

EOF
)"
```

---

### Task 10: 运行状态页与权限中心

**Files:**
- Create: `ui/run/RunStatusScreen.kt`
- Create: `ui/permissions/PermissionCenterScreen.kt`
- Create: `platform/PermissionHelper.kt`

- [ ] **Step 1: 运行页调用 `runner.run`，逐步展示 outcome**
- [ ] **Step 2: PermissionHelper 封装通知、使用情况访问、WRITE_SETTINGS、勿扰等跳转**
- [ ] **Step 3: 权限中心列出状态与「去设置」按钮**
- [ ] **Step 4: Commit**

```bash
git commit -m "$(cat <<'EOF'
feat: 添加运行状态页与权限能力中心

EOF
)"
```

---

### Task 11: TriggerManager — 定时 / 充电事件 / hostOpened

**Files:**
- Create: `engine/TriggerManager.kt`
- Create: `worker/ScheduleMiniAppWorker.kt`
- Create: `platform/EventReceivers.kt`
- Modify: `MiniAppApplication` / `MainActivity` 启动时 `triggerManager.resync()`
- Modify: `AndroidManifest.xml` 注册 Receiver

- [ ] **Step 1: ScheduleMiniAppWorker** 读取 input `miniAppId`，调 `ServiceLocator.runner.run(id, "schedule")`

- [ ] **Step 2: TriggerManager.resync()**

遍历所有 enabled MiniApp 的 triggers：

- `schedule` → `PeriodicWorkRequest` 或 `OneTime`+链式，unique name `schedule-$miniAppId-$triggerId`
- `event` → 确保对应 Receiver 已注册（Manifest 静态注册 `ACTION_POWER_CONNECTED` / `CONNECTIVITY_CHANGE` 需注意限制；一期至少支持 `ACTION_POWER_CONNECTED`）
- 取消已删除触发的 Work

- [ ] **Step 3: PowerConnectedReceiver** 查找含匹配 event 的小应用并 `runner.run`

- [ ] **Step 4: MainActivity.onCreate / onStart** 若有 `getHostOpenApp()` 则 `runner.run(..., "hostOpened")`

- [ ] **Step 5: 保存编辑后调用 `resync()`**

- [ ] **Step 6: Commit**

```bash
git commit -m "$(cat <<'EOF'
feat: 实现定时、充电事件与宿主启动触发

EOF
)"
```

---

### Task 12: appOpened 监听（UsageStats）

**Files:**
- Create: `platform/AppOpenMonitor.kt`
- Modify: `TriggerManager` / `MiniAppApplication`
- Modify: Manifest + 权限中心文案（`PACKAGE_USAGE_STATS`）

- [ ] **Step 1: AppOpenMonitor**

用 `UsageStatsManager.queryEvents` 在协程中每 1～2s 轮询（仅在有 `appOpened` 触发且已授权时启动），检测前台包名变化，匹配 triggers 的 `packageName` 后 `runner.run(..., "appOpened")`。

- [ ] **Step 2: 未授权时不轮询，权限中心引导 `Settings.ACTION_USAGE_ACCESS_SETTINGS`**

- [ ] **Step 3: Commit**

```bash
git commit -m "$(cat <<'EOF'
feat: 支持打开指定第三方应用时触发小应用

EOF
)"
```

---

### Task 13: 桌面快捷方式与 Deep Link

**Files:**
- Create: `platform/ShortcutHelper.kt`
- Modify: `MiniAppListScreen` 增加「添加到桌面」
- Modify: `AndroidManifest.xml` intent-filter（`action.VIEW` + scheme `miniapp` host `run`）
- Modify: `MainActivity` 解析 `miniapp://run/{id}` 或 extras

- [ ] **Step 1: ShortcutManagerCompat 请求固定快捷方式，Intent 指向 MainActivity 并带 miniAppId**
- [ ] **Step 2: 冷启动 deep link 直接 `runner.run`**
- [ ] **Step 3: Commit**

```bash
git commit -m "$(cat <<'EOF'
feat: 支持桌面快捷方式与 deep link 运行

EOF
)"
```

---

### Task 14: 一期验收与文档状态更新

**Files:**
- Modify: `docs/superpowers/specs/2026-07-11-miniapp-factory-design.md`（状态改为「一期实现中/已完成」）
- Modify: `需求.md`（可补充验收清单）

- [ ] **Step 1: 按下列清单真机验收**

1. 创建小应用：震动 → 延时 → 调音量 → 通知，手动运行成功  
2. 启用开关关闭后无法运行  
3. `runOnHostOpen` 打开 App 自动跑  
4. 定时触发至少一次  
5. 插充电器触发（支持机型）  
6. 授权使用情况访问后，打开指定包名触发  
7. 桌面快捷方式可启动运行  
8. 权限中心可跳转系统设置  
9. Wi‑Fi/截图等在不支持时出现明确失败码而非闪退  

- [ ] **Step 2: 更新设计文档状态字段为「一期已实现」或列出未完成项（如 MediaProjection 截图）**

- [ ] **Step 3: Commit**

```bash
git commit -m "$(cat <<'EOF'
docs: 更新一期验收说明与文档状态

EOF
)"
```

---

## 本计划不包含（单独后续计划）

- 二期 Runner 模板与导出独立 APK
- 无障碍服务备选（UsageStats 不足时）
- 强常驻前台监控中枢
- 云同步 / 账号

---

## 自检（相对 Spec）

| Spec 项 | 对应 Task |
|---------|-----------|
| Domain / Action 插件 | Task 2–3, 5–6 |
| Room 持久化 | Task 4 |
| ActionExecutor + 失败策略 | Task 3, 7 |
| 动作清单（含尽力而为） | Task 5–6 |
| 手动运行 / 日志 | Task 7–8, 10 |
| 编辑器 | Task 9 |
| 权限中心 | Task 10 |
| schedule / event / hostOpened | Task 11 |
| appOpened | Task 12 |
| 桌面快捷方式 | Task 13 |
| 二期 APK | 明确排除 |
| 防抖 | Task 3 Debouncer + Task 7 |

---

## 执行方式

计划已保存。可选：

1. **Subagent-Driven（推荐）** — 每任务新开子代理，任务间复核  
2. **Inline Execution** — 本会话按 executing-plans 批量推进并设检查点  

请选择 **1** 或 **2** 开始实现；若只要文档、暂不写代码，回复「先不实现」即可。
