plugins {
    alias(libs.plugins.klyx.multiplatform)
    alias(libs.plugins.kotlinx.atomicfu)
}

kotlin {
    android { namespace = "com.klyx.editor.language" }

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.util)
                implementation(projects.core)
                api(projects.feature.settings)
            }
        }
    }
}

atomicfu {
    transformJvm = false
    jvmVariant = "VH"
}
