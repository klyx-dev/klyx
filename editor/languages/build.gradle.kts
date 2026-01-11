plugins {
    alias(libs.plugins.klyx.multiplatform)
}

kotlin {
    android { namespace = "com.klyx.editor.languages" }

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core)
                implementation(projects.editor.language)
            }
        }
    }
}
