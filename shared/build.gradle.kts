plugins {
    alias(libs.plugins.klyx.multiplatform)
    alias(libs.plugins.klyx.multiplatform.compose)
    alias(libs.plugins.kotlinx.atomicfu)
}

kotlin {
    android { namespace = "com.klyx.shared" }

    sourceSets {
        val commonJvmAndroid by creating {
            dependsOn(commonMain.get())
        }

        androidMain.get().dependsOn(commonJvmAndroid)
        jvmMain.get().dependsOn(commonJvmAndroid)

        commonMain.dependencies {
            implementation(libs.koin.core)
        }
    }
}

atomicfu {
    transformJvm = false
}
