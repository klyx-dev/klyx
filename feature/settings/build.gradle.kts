plugins {
    alias(libs.plugins.klyx.multiplatform)
}

kotlin {
    android { namespace = "com.klyx.settings" }

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core)
            }
        }
    }
}
