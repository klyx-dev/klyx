plugins {
    alias(libs.plugins.klyx.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

kotlin {
    android { namespace = "com.klyx.extension" }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)

                implementation(projects.core)
                implementation(projects.util)
                implementation(projects.lsp.api)
                implementation(projects.editor.language)
                implementation(projects.feature.nodeGraph)
            }
        }
    }
}
