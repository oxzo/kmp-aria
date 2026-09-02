plugins {
    // Applied in subprojects. Declared here to pin the versions once.
    // compose-compiler plugin version must equal Kotlin's. Fallback pair if the toolchain
    // fights: Kotlin 2.3.20 (what Compose Multiplatform 1.12.0 was built against).
    kotlin("multiplatform") version "2.4.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
    id("org.jetbrains.compose") version "1.12.0" apply false
}
