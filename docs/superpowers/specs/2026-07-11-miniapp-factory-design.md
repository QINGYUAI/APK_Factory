# 应用工厂（MiniApp Factory）设计文档

- 日期：2026-07-11
- 状态：一期已实现（分支 `feature/miniapp-factory`；截图动作为 `NEEDS_UI` 桩，Wi‑Fi 在多数机型为 `UNSUPPORTED`；计划见 docs/superpowers/plans/2026-07-11-miniapp-factory.md）
- 2026-11-16：完成稳定性加固与 ViewModel 状态层改造（重复接收器、执行器异常兜底、流式导出、本机签名、Room schema、日程单测），变更清单见 `需求.md`
- 2026-11-16（第二轮）：运行会话取消作用域、日志保留与索引迁移、配置同步原子替换、JSON 容错与动作保存校验、快捷方式清理，详见 `需求.md`
- 工程：`D:\web\android`（现有 Kotlin + Jetpack Compose 模板工程）
- 选定方案：方案一 — 动作插件 + 本地编排引擎

## 1. 背景与目标

### 1.1 原始需求

快速生成「手机小应用」：用户新增一个软件后，可自动执行手机相关操作（震动、播放音乐、调大音量等），并覆盖更多手机能力。

### 1.2 产品决策（已确认）

| 维度 | 决策 |
|------|------|
| 形态 | **应用工厂**：在本 App 内配置操作序列，生成可运行的「小应用」 |
| 交付 | **一期**应用内运行 + 桌面快捷方式；**二期**支持导出独立 APK |
| 能力范围 | 大范围手机能力（见 §3 动作清单），架构按可扩展插件设计 |
| 触发 | 手动、定时、系统事件；打开指定第三方 App；打开本 App 自动执行（两者都要） |

### 1.3 成功标准

- 用户可创建、编辑、启用/禁用小应用，并串行执行动作链
- 至少覆盖震动、播音乐、调音量，并具备扩展其他系统能力的统一注册方式
- 支持手动 / 定时 / 事件 / 打开第三方 App / 打开本 App 触发
- 权限不足或机型不支持时有明确失败信息，不崩溃
- 一期配置 schema 可被二期 Runner APK 复用

## 2. 现状基线

当前仓库为 Android Studio 默认「My Application」模板：

- 包名：`com.example.myapplication`
- 技术栈：Kotlin、Jetpack Compose、Material3
- SDK：minSdk 34 / targetSdk 36 / compileSdk 36
- 入口：`MainActivity` 仅展示 `Hello Android!`
- 尚无业务模块、无相关权限声明

## 3. 整体架构

```
┌─────────────────────────────────────────┐
│  UI（Compose）                           │
│  小应用列表 / 编辑器 / 运行日志 / 权限引导   │
├─────────────────────────────────────────┤
│  Domain                                  │
│  MiniApp · Action · Trigger · RunSession │
├─────────────────────────────────────────┤
│  Engine                                  │
│  ActionExecutor（串行执行动作链）          │
│  TriggerManager（注册/注销触发器）         │
├─────────────────────────────────────────┤
│  Data                                    │
│  Room（小应用/动作/触发器）+ 可选 DataStore │
├─────────────────────────────────────────┤
│  Platform（Android API 适配）             │
│  震动/音频/音量/手电筒/亮度/系统设置…      │
│  快捷方式 · WorkManager · 事件监听        │
└─────────────────────────────────────────┘
```

### 3.1 原则

- UI 不直接调用系统 API；手机能力一律经 `Action` 插件
- 只执行白名单动作类型，不执行任意用户代码/脚本
- 一期与二期共用同一声明式配置 schema（JSON）

### 3.2 分期

| 阶段 | 内容 |
|------|------|
| 一期 | 应用内 CRUD、执行引擎、手动/定时/基础事件、hostOpened/appOpened、桌面快捷方式、权限中心、运行日志 |
| 二期 | Runner 模板模块 + 嵌入配置打包签名，导出可独立安装的 APK |
| 可选增强 | 前台常驻监听（原方案二能力），按需引入，非一期必做 |

## 4. 核心数据模型

### 4.1 MiniApp

| 字段 | 说明 |
|------|------|
| id | 唯一标识 |
| name | 显示名称 |
| icon | 图标（资源或自定义） |
| enabled | 是否启用（含触发） |
| runOnHostOpen | 打开本 App 时是否作为默认执行项 |
| createdAt / updatedAt | 时间戳 |
| steps | 有序 ActionStep 列表 |
| triggers | Trigger 列表 |

### 4.2 ActionStep

| 字段 | 说明 |
|------|------|
| type | 白名单动作类型字符串 |
| params | JSON 参数 |
| order | 执行顺序 |
| onFailure | `skip`（跳过继续）或 `abort`（中止整链） |

### 4.3 Trigger

| 字段 | 说明 |
|------|------|
| type | `manual` / `schedule` / `event` / `appOpened` / `hostOpened` |
| params | 如间隔/日历规则、Broadcast action、目标包名等 |
| enabled | 是否启用 |

### 4.4 RunLog

记录每次运行：miniAppId、起止时间、每步结果、错误码与消息，供列表与运行页展示。

## 5. 动作清单（一期目标）

| type | 说明 | 权限 / 注意 |
|------|------|-------------|
| vibrate | 震动时长/模式 | `VIBRATE` |
| playMusic | 播放本地音频/URI | 媒体读权限 |
| setVolume | 调节媒体/铃声音量 | 部分机型可能限制 |
| flashlight | 手电筒开关 | Camera 手电筒特性 |
| setBrightness | 屏幕亮度 | `WRITE_SETTINGS`，常需跳转系统设置 |
| openApp | 打开指定包名 | 查询已安装应用 |
| delay | 等待 N 毫秒 | 无 |
| wifi | Wi‑Fi 开关 | 权限与 Android 10+ 限制多，尽力而为 |
| bluetooth | 蓝牙开关 | 蓝牙相关权限 |
| setRingerMode | 静音/震动/响铃 | 勿扰/通知策略相关权限 |
| screenshot | 截图 | MediaProjection 用户授权 |
| notify | 本地通知 | Android 13+ 通知权限 |

未就绪类型可在编辑器中标记为占位/灰度，但 schema 预留 type，减少后续破坏性变更。

### 5.1 扩展方式

```text
ActionRegistry.register(type) -> ActionHandler
ActionHandler.validate(params)
ActionHandler.execute(context, params) -> ActionResult
```

新增能力 = 新 Handler + 权限声明 + 编辑器表单，不改 Executor 主流程。

## 6. 触发器设计

| type | 行为 |
|------|------|
| manual | 列表「运行」；桌面快捷方式 / deep link 带 miniAppId |
| schedule | WorkManager 周期或指定时间 |
| event | 系统 Broadcast（如充电、网络变化等，一期选常用子集） |
| appOpened | 指定第三方包名进入前台时触发；优先 UsageStats，备选无障碍服务 |
| hostOpened | 本 App 冷/热启动时，执行 `runOnHostOpen == true` 的小应用 |

防抖：同一 miniApp 在短时间窗口内不重复风暴触发（可配置，如 3～5 秒）。

## 7. 执行流、权限与错误处理

### 7.1 执行流

1. 触发源 → `TriggerManager` 解析 `miniAppId`
2. `ActionExecutor` 加载有序步骤
3. 每步：权限预检/申请 → Platform 适配 → 写 RunLog
4. 完成或中止 → 更新 UI；可选系统通知

并发：默认同时仅一个 `RunSession`；新触发可配置排队或忽略（一期默认忽略进行中的重复触发）。

### 7.2 权限策略

- 按动作懒申请；编辑器可预检
- 设置类能力引导至系统设置页，禁止「假成功」
- `appOpened`：引导「使用情况访问权限」；不足时再提示无障碍备选
- `screenshot`：MediaProjection 授权失败则该步失败并记录

### 7.3 错误处理

| 情况 | 行为 |
|------|------|
| 权限拒绝 | 记日志；按 `onFailure` 跳过或中止 |
| 机型不支持 | 错误码 `UNSUPPORTED`，不崩溃 |
| 目标 App 未安装 | `openApp` 失败并提示 |
| 单步超时 | 默认建议 60s/步，可配置；超时后按 `onFailure` |
| 进程被杀 | 定时由 WorkManager 恢复；事件监听一期不做强常驻前台服务 |

### 7.4 安全

- 仅白名单 Action
- 二期 APK 只嵌入声明式配置，不引入脚本引擎

## 8. 界面（一期）

| 页面 | 职责 |
|------|------|
| 小应用列表 | 创建、启用/禁用、运行、编辑、查看最近日志 |
| 编辑器 | 名称、动作增删排序、触发器、`runOnHostOpen` |
| 动作配置表单 | 按 type 动态表单 |
| 运行状态 | 当前步骤、成功/失败、停止 |
| 权限与能力中心 | 授权状态汇总、跳转系统设置 |

交互路径：列表 → 编辑 → 运行。保持单一职责页面，避免仪表盘式堆砌。

## 9. 二期：导出独立 APK

1. 维护精简 **Runner 模板模块**（启动即读嵌入配置并执行，可带极简状态 UI）
2. 导出流程：序列化 MiniApp → 写入模板 assets → 组装包 → 签名 → 分享/保存
3. 安装后在桌面显示独立应用名与图标
4. 与一期共用 schema 与 Action 实现（Runner 依赖同一 engine/platform 库模块更佳）

一期过渡：桌面快捷方式 + deep link 打开宿主并执行指定小应用。

## 10. 建议模块划分（实现时）

```text
:app                    UI、权限引导、触发注册入口
:core:domain            模型与接口
:core:data              Room / 仓库
:core:engine            ActionExecutor、TriggerManager、Registry
:core:platform          各 Action 的 Android 实现
（二期）:runner         导出用精简应用壳
```

一期若求快，可先单模块分包（`ui` / `domain` / `engine` / `data` / `platform`），二期再拆 Gradle 模块。

## 11. 测试要点

- 单元：参数校验、失败策略、Trigger 解析、防抖
- 仪器/真机：震动、音量、通知等；权限拒绝路径
- 机型抽测：Wi‑Fi / 蓝牙 / 亮度 / 勿扰在 1～2 个品牌上的真实能力
- 回归：保存后再运行；`hostOpened` / `appOpened` / 定时不重复风暴

## 12. 明确不做（一期）

- 强常驻前台监控中枢（整套方案二）
- JS/Lua 等脚本引擎（方案三）
- 云同步、账号体系
- 保证截图 / Wi‑Fi 等在全机型 100% 可用（允许尽力而为 + 明确失败）

## 13. 风险与缓解

| 风险 | 缓解 |
|------|------|
| OEM 对后台/广播限制 | 定时用 WorkManager；关键能力引导「自启动/电池无限制」说明 |
| Wi‑Fi/系统设置 API 废弃或受限 | Platform 层按 API Level 分支；不支持则 `UNSUPPORTED` |
| UsageStats 延迟 | 文档说明延迟；必要时提供无障碍备选 |
| 导出 APK 签名与打包复杂 | 二期专项；一期用快捷方式验证产品闭环 |
| minSdk 34 限制旧设备 | 与当前工程一致；若需扩大用户面再单独评估降 minSdk |

## 14. 后续步骤

设计文档用户确认后：

1. 使用实现计划流程拆分任务（数据层 → 引擎 → 基础动作 → UI → 触发器 → 权限中心 → 快捷方式）
2. 再进入编码实现

---

## 修订记录

| 日期 | 说明 |
|------|------|
| 2026-07-11 | 初稿：方案一，覆盖 §1～§4 已确认设计 |
