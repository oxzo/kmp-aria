import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    jvm()
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("composeApp")
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":aria"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            // Material3 control routes: the framework's own widgets, separately versioned.
            implementation("org.jetbrains.compose.material3:material3:1.12.0-alpha03")
        }
        wasmJsMain.dependencies {
            // window.location.hash routing only.
            implementation("org.jetbrains.kotlinx:kotlinx-browser:0.5.0")
        }
        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.compose.ui:ui-test:1.12.0")
            implementation(compose.desktop.currentOs)
        }
    }
}
