import com.android.build.api.dsl.androidLibrary
import com.klyx.Configs

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xcontext-parameters")
    }

    jvmToolchain(21)

    @Suppress("UnstableApiUsage")
    androidLibrary {
        namespace = "com.klyx.extension"
        compileSdk = Configs.Android.COMPILE_SDK_VERSION
        minSdk = Configs.Android.MIN_SDK_VERSION
    }

    jvm()

    sourceSets {
        val commonMain by getting
        val jvmMain by getting
        val androidMain by getting

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)

            implementation(libs.kotlinx.datetime)

            implementation(projects.core)
            implementation(projects.shared)
            implementation(projects.wasm)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        androidMain.dependencies {
            implementation(libs.androidx.documentfile)
        }
    }
}
