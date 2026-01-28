plugins {
    alias(libs.plugins.klyx.multiplatform)
    alias(libs.plugins.klyx.multiplatform.compose)
}

kotlin {
    android { namespace = "com.klyx.util" }

    sourceSets {
        commonMain {
            dependencies {
                api(libs.arrow.core)
                api(libs.arrow.fx.coroutines)
            }
        }
    }
}
