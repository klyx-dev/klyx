plugins {
    alias(libs.plugins.klyx.multiplatform)
}

kotlin {
    android { namespace = "com.klyx.lsp.server" }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.lsp.api)
            }
        }
    }
}
