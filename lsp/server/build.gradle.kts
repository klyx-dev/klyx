plugins {
    alias(libs.plugins.klyx.multiplatform)
    alias(libs.plugins.kotlinx.atomicfu)
}

kotlin {
    android { namespace = "com.klyx.lsp.server" }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("reflect"))

                api(libs.kotlinx.io.core)
                api(projects.lsp.api)
            }
        }
    }
}
