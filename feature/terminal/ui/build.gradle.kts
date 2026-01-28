plugins {
    alias(libs.plugins.klyx.multiplatform)
    alias(libs.plugins.klyx.multiplatform.compose)
}

kotlin {
    android { namespace = "com.klyx.terminal.ui" }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.navigationevent)
                implementation(libs.kotlin.logging)
                api(projects.feature.terminal.emulator)
                implementation(projects.util)
                implementation(projects.core)
            }
        }
    }
}
