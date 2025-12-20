plugins {
    alias(libs.plugins.klyx.multiplatform)
}

kotlin {
    android { namespace = "com.klyx.extension" }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.ktoml.core)

                implementation(projects.core)
                implementation(projects.wasm)
                implementation(projects.lsp.api)
            }
        }
    }
}
