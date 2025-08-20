import com.klyx.Configs
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.ksp)
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

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "wasm"
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonAndroidJvm by creating {
            dependsOn(commonMain.get())

            dependencies {
                implementation(libs.chicory.runtime)
                implementation(libs.chicory.wasi)
                implementation(libs.chicory.annotations)

                implementation(libs.wasip1.bindings.chicory)
            }
        }

        androidMain.get().dependsOn(commonAndroidJvm)
        jvmMain.get().dependsOn(commonAndroidJvm)

        commonMain {
            languageSettings {
                @OptIn(ExperimentalKotlinGradlePluginApi::class)
                compilerOptions {
                    freeCompilerArgs.addAll("-Xexpect-actual-classes")
                }
            }

            dependencies {
                implementation(libs.kotlinx.coroutines.core)

                implementation(libs.wasip1.host)

                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.io.core)

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

dependencies {
    add("kspCommonMainMetadata", project(":wasm-ksp"))
    add("kspAndroid", project(":wasm-ksp"))
    add("kspJvm", project(":wasm-ksp"))
}
