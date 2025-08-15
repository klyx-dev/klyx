import com.klyx.Configs

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xcontext-parameters")
    }

    androidLibrary {
        namespace = "com.klyx.wasm"
        compileSdk = Configs.Android.COMPILE_SDK_VERSION
        minSdk = Configs.Android.MIN_SDK_VERSION
    }

    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.chicory.runtime)
                implementation(libs.chicory.wasi)
                implementation(libs.chicory.annotations)
                implementation(libs.jimfs)

                implementation(projects.shared)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {

            }
        }
    }
}
