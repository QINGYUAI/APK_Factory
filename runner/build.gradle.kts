plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val sharedSrcDir = layout.projectDirectory.dir("src/shared/java")
val factorySources = rootProject.projectDir.resolve("../app/src/main/java")

val syncSharedSources by tasks.registering(Sync::class) {
    from(factorySources) {
        include(
            "**/domain/**",
            "**/engine/ActionExecutor.kt",
            "**/engine/ActionRegistry.kt",
            "**/engine/Debouncer.kt",
            "**/data/JsonMappers.kt",
            "**/platform/PlatformActionContext.kt",
            "**/platform/PermissionHelper.kt",
            "**/platform/MediaUriHelper.kt",
            "**/platform/MiniAppConfigStore.kt",
            "**/platform/ConfigSyncContract.kt",
            "**/platform/RuntimeEffectTracker.kt",
            "**/platform/media/**",
            "**/platform/handlers/**",
            "**/ui/icons/AppIconCatalog.kt",
            "**/ui/editor/ActionDisplay.kt",
            "**/ui/media/MediaControlBar.kt",
        )
    }
    into(sharedSrcDir)
}

android {
    namespace = "com.example.minirunner"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        // 模板包名；导出时由应用工厂改写为每个小应用独立包名
        applicationId = "com.example.minirunner"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release { isMinifyEnabled = false }
        debug { isMinifyEnabled = false }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    sourceSets {
        named("main") {
            kotlin.directories.add("src/shared/java")
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn(syncSharedSources)
}

// 即使有人手动 -p runner install，也禁止装到手机
tasks.configureEach {
    if (name.startsWith("install")) {
        enabled = false
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation("androidx.media:media:1.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
