# 应用工厂（APK Factory）

把手机操作编排成「小应用」：在应用工厂里配置动作序列与触发器，直接在应用内运行，或导出为带独立图标、名称和包名的可安装 APK。

- 平台：Android 14+（minSdk 34 / targetSdk 36）
- 技术栈：Kotlin、Jetpack Compose、Room、WorkManager、AlarmManager
- 安装包：[GitHub Releases](https://github.com/QINGYUAI/APK_Factory/releases/latest)

## 功能

### 动作编排

震动、播放音频/视频、调节音量、手电筒、屏幕亮度、打开指定 App、延时、Wi‑Fi 面板、蓝牙、响铃/静音模式、本地通知等，按顺序串行执行；单个动作失败可按策略跳过或中止整条链路。截图动作目前为占位（`NEEDS_UI`）。

### 触发器

- 手动运行、桌面快捷方式、`miniapp://` deep link
- 间隔触发、单次定时、每天定时、每周定时、时段内循环（支持例外日期）
- 系统事件：充电/拔充、开机、解锁、耳机、飞行模式、电量
- 打开指定第三方 App（UsageStats）
- 打开应用工厂时自动运行

后台触发采用有界排队并合并重复事件，手动运行保持「忙碌即跳过」的即时反馈。

### 独立安装包

- 将当前配置、媒体与图标注入 Runner 模板，改写包名/桌面名并重新签名
- 同一小应用再次导出时递增版本号，可直接覆盖更新
- 已安装后可用「同步配置」热更新动作和媒体，无需重装；换图标/引擎时用「更新应用」
- 独立包支持定时与静态系统事件后台触发：AlarmManager 调度，WorkManager 执行；开机、更新、改时间/时区后自动重排
- 未授予精确闹钟权限时自动退回近似时间，独立包内提供授权入口

### 可观测性

运行状态逐步展示、可停止当前动作链；运行历史支持查看与清空；权限中心汇总特殊权限与跳转入口。

## 下载安装

1. 打开 [Releases](https://github.com/QINGYUAI/APK_Factory/releases/latest)，下载 `AppFactory-*.apk`
2. 在手机上允许「安装未知应用」后安装
3. 首次打开应用工厂，按引导授予所需权限
4. 新建小应用后，可用「添加到桌面」生成独立安装包

> 应用工厂生成的独立小应用也通过系统安装器安装，需要允许应用工厂「安装未知应用」。

## 使用流程

```text
安装应用工厂
   ↓
新建小应用 → 添加动作 → 配置触发器 → 保存
   ↓
运行（应用内 / 快捷方式 / deep link / 触发器）
   ↓
添加到桌面 → 生成独立 APK → 安装
   ↓
后续修改：同步配置（热更新）或更新应用（图标 / 引擎）
```

## 项目结构

```text
app/                     应用工厂
  src/main/java/.../
    data/                Room 实体、DAO、仓库、JSON 映射
    domain/              小应用 / 动作 / 触发器模型
    engine/              动作执行器、触发器同步、运行会话
    platform/            系统能力适配、媒体、广播接收器、配置同步
    export/              Runner 模板改写、签名、导出预检
    ui/                  Compose 页面与 ViewModel
    worker/              WorkManager 定时任务
runner/                  导出模板工程（独立 Gradle 工程，不直接安装）
docs/                    设计文档
需求.md                  需求与验收清单
```

构建时 `:app:preBuild` 会在 `runner/` 工程中编译模板 APK，并复制到 `app/src/main/assets/templates/runner.apk`（该文件不进版本库）。`runner/src/shared/` 由构建任务从 `app/` 同步生成。

导出链路：读取模板 → ARSCLib 改写清单与资源 → 注入 `miniapp.json` 与媒体 → apksig V2/V3 签名。导出签名密钥由 `AndroidKeyStore` 在本机生成，不随安装包分发。

## 从源码构建

环境要求：

- JDK 17
- Android SDK Platform 36 与 Build-Tools

```bash
git clone https://github.com/QINGYUAI/APK_Factory.git
cd APK_Factory
./gradlew :app:assembleDebug
```

Windows 使用 `gradlew.bat`。安装到已连接设备：

```bash
./gradlew :app:installFactory
```

### Release 签名

Release 构建需要本地签名文件，仓库不包含密钥：

1. 创建 `keystore/release.jks`
2. 在项目根目录创建 `keystore.properties`：

```properties
storeFile=keystore/release.jks
storePassword=你的密码
keyAlias=apkfactory
keyPassword=你的密码
```

3. 构建：

```bash
./gradlew :app:assembleRelease
```

产物位于 `app/build/outputs/apk/release/`。`keystore.properties` 与 `*.jks` 已被 `.gitignore` 排除，请自行备份；丢失后将无法再覆盖更新已发布的应用。

## 测试

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

当前包含 59 个 JVM 单元测试，覆盖 JSON 容错、日程计算、执行器异常、运行会话停止/排队语义、导出标识等。

## 已知限制

- 截图动作尚未实现，执行时返回 `NEEDS_UI`
- 「打开指定 App」依赖使用情况访问权限；独立包内不支持该触发与亮屏/灭屏触发
- 独立包安装后需先打开一次，系统才允许其后台触发
- 后台播放音视频、拉起其他 App 可能被系统限制
- Android 10+ 上 Wi‑Fi 开关仅能打开系统面板
- 独立包的导出签名密钥保存在应用工厂的本机密钥库中：卸载应用工厂或清除其数据后，无法再覆盖更新已安装的独立小应用

## 文档

- [需求与验收清单](需求.md)
- [设计文档](docs/superpowers/specs/2026-07-11-miniapp-factory-design.md)
