plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val sharedSrcDir = layout.projectDirectory.dir("src/shared/java")

val syncSharedSources by tasks.registering(Sync::class) {
    from(project(":app").projectDir.resolve("src/main/java")) {
        include(
            "**/domain/**",
            "**/engine/ActionExecutor.kt",
            "**/engine/ActionRegistry.kt",
            "**/engine/Debouncer.kt",
            "**/data/JsonMappers.kt",
            "**/platform/PlatformActionContext.kt",
            "**/platform/PermissionHelper.kt",
            "**/platform/MediaUriHelper.kt",
            "**/platform/handlers/**",
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
        applicationId = "com.example.minirunner"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    // AGP 9：通过 kotlin.directories 引入同步后的共享内核源码
    sourceSets {
        named("main") {
            kotlin.directories.add("src/shared/java")
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn(syncSharedSources)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
