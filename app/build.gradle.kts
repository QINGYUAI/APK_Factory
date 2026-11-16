plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.myapplication"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // 桌面显示名来自 res/values/strings.xml → 应用工厂
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

ksp {
    // Room 导出版本 schema，供后续 Migration / AutoMigration 使用
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation("androidx.media:media:1.7.0")
    implementation("com.android.tools.build:apksig:8.7.3")
    implementation("io.github.reandroid:ARSCLib:1.4.0")
    testImplementation(libs.junit)
    // 本地 JVM 单测中的 org.json 来自 android.jar 桩，需真实实现才能测序列化
    testImplementation("org.json:json:20240303")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

val isWindows = System.getProperty("os.name").lowercase().contains("windows")
val runnerProjectDir = rootProject.layout.projectDirectory.dir("runner")
val runnerApk = runnerProjectDir.file("build/outputs/apk/debug/runner-debug.apk")
val templateOutDir = layout.projectDirectory.dir("src/main/assets/templates")

/** 把主工程 SDK 配置同步给独立 runner 工程 */
val prepareRunnerLocalProps by tasks.registering(Copy::class) {
    from(rootProject.file("local.properties"))
    into(runnerProjectDir)
    onlyIf { rootProject.file("local.properties").exists() }
}

/**
 * 在独立工程中编译导出模板 APK（不会作为本 IDE 的可运行 App 出现）。
 */
val assembleRunnerTemplate by tasks.registering(Exec::class) {
    dependsOn(prepareRunnerLocalProps)
    workingDir = rootProject.projectDir
    if (isWindows) {
        commandLine("cmd", "/c", ".\\gradlew.bat", "-p", "runner", "assembleDebug")
    } else {
        commandLine("./gradlew", "-p", "runner", "assembleDebug")
    }
    inputs.dir(runnerProjectDir.dir("src"))
    inputs.dir(rootProject.file("app/src/main/java"))
    outputs.file(runnerApk)
}

/** 将模板拷入应用工厂 assets，供「添加到桌面」导出使用 */
val syncRunnerTemplate by tasks.registering(Copy::class) {
    dependsOn(assembleRunnerTemplate)
    from(runnerApk)
    into(templateOutDir)
    rename { "runner.apk" }
}

tasks.named("preBuild").configure {
    dependsOn(syncRunnerTemplate)
}

tasks.register("installFactory") {
    group = "install"
    description = "安装应用工厂（com.example.myapplication），并清理迷你应用模板"
    dependsOn("installDebug")
    doFirst {
        val adb = findAdb()
        if (adb != null) {
            val code = ProcessBuilder(adb, "uninstall", "com.example.minirunner")
                .redirectErrorStream(true)
                .start()
                .waitFor()
            logger.lifecycle("卸载模板包 com.example.minirunner 结束（exit=$code，不存在时也可忽略）")
        } else {
            logger.warn("未找到 adb，跳过卸载模板包；请确认运行的是「应用工厂」配置")
        }
    }
    doLast {
        logger.lifecycle("已安装应用工厂。请在 App 内用「添加到桌面」生成各小应用。")
    }
}

fun findAdb(): String? {
    val localProps = rootProject.file("local.properties")
    if (!localProps.exists()) return null
    val sdkLine = localProps.readLines().firstOrNull { it.startsWith("sdk.dir=") } ?: return null
    val sdkDir = sdkLine.substringAfter("=")
        .replace("\\:", ":")
        .replace("\\\\", "\\")
    val adbWin = File(sdkDir, "platform-tools/adb.exe")
    val adbUnix = File(sdkDir, "platform-tools/adb")
    return when {
        adbWin.exists() -> adbWin.absolutePath
        adbUnix.exists() -> adbUnix.absolutePath
        else -> null
    }
}