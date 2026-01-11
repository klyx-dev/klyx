plugins {
    alias(libs.plugins.klyx.multiplatform)
}

kotlin {
    android { namespace = "com.klyx.project" }

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core)
                api(projects.lsp.server)
                implementation(projects.editor.language)
            }
        }
    }
}
