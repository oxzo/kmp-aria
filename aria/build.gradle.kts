import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

// Karma launches Chrome through karma-chrome-launcher, which reads CHROME_BIN. Default to
// the Playwright-cached Chrome for Testing so the truth run needs no environment variable.
val chromeBin: String = System.getenv("CHROME_BIN")
    ?: "${System.getProperty("user.home")}/.cache/ms-playwright/chromium-1217/chrome-linux64/chrome"

kotlin {
    jvm()
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            testTask {
                environment("CHROME_BIN", chromeBin)
                useKarma {
                    useChromeHeadlessNoSandbox()
                }
            }
        }
        // Required: the compose plugin's checkComposeUiTestConfigurationForWasmJs fails without it.
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":stately"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.compose.ui:ui-test:1.12.0")
        }
        jvmTest.dependencies {
            // skiko native runtime for headless Compose UI tests on the JVM.
            implementation(compose.desktop.currentOs)
        }
    }
}
