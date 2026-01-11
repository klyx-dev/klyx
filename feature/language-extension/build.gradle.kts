plugins {
    alias(libs.plugins.klyx.multiplatform)
}

kotlin {
    android { namespace = "com.klyx.language.extension" }

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core)
                implementation(projects.feature.extension)
                implementation(projects.editor.language)
            }
        }
    }
}
