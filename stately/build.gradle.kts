import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        // Pure Kotlin state: tests run under Node, not Karma. The browser run is :aria's.
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            // Snapshot state only (mutableStateOf); no compose compiler plugin, no UI.
            // The analogue of react-stately depending on React hooks.
            api("org.jetbrains.compose.runtime:runtime:1.12.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
