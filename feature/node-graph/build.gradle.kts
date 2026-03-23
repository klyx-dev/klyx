plugins {
    alias(libs.plugins.klyx.multiplatform)
    alias(libs.plugins.klyx.multiplatform.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android { namespace = "com.klyx.nodegraph" }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.serialization.protobuf)
            }
        }
    }
}
