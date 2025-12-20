plugins {
    alias(libs.plugins.klyx.multiplatform)
}

kotlin {
    android { namespace = "com.klyx.lsp.api" }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.core)
            }
        }
    }
}
