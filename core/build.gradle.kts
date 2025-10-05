import com.klyx.Configs
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxAtomicfu)
}

kotlin {
    jvmToolchain(21)

    compilerOptions {
        freeCompilerArgs.addAll("-Xcontext-parameters")
    }

    androidLibrary {
        namespace = "com.klyx.core"
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
            baseName = "core"
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonJvmAndroid by creating {
            dependsOn(commonMain.get())
        }

        androidMain.get().dependsOn(commonJvmAndroid)
        jvmMain.get().dependsOn(commonJvmAndroid)

        commonMain {
            languageSettings {
                @OptIn(ExperimentalKotlinGradlePluginApi::class)
                compilerOptions {
                    freeCompilerArgs.addAll("-Xexpect-actual-classes")
                }
            }

            dependencies {
                api(libs.koin.compose)

                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.navigation.compose)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.androidx.lifecycle.runtimeCompose)

                api(libs.kotlinx.serialization.json)
                implementation(libs.json5k)
                implementation(libs.ktoml.core)
                api(libs.ktor.client.core)
                api(libs.ktor.client.cio)
                api(libs.ktor.client.content.negotiation)
                api(libs.ktor.serialization.kotlinx.json)
                implementation(libs.commons.compress)
                api(libs.koin.core)

                implementation(kotlin("reflect"))
                api(libs.kotlinx.io.core)
                api(libs.kotlinx.io.okio)
                api(libs.okio)
                implementation(libs.kotlinx.coroutines.core)

                api(libs.kotlin.result)
                api(libs.filekit.dialogs)
                api(libs.filekit.dialogs.compose)

                api(libs.arrow.core)
                api(libs.arrow.fx.coroutines)

                api(libs.kfswatch)
                implementation(libs.multiplatform.settings.no.arg)

                implementation(projects.shared)
                implementation(projects.wasm)
            }
        }

        commonJvmAndroid.dependencies {

        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        androidMain {
            dependencies {
                implementation(libs.androidx.activity.compose)

                api(libs.utilcodex)
                api(libs.androidx.documentfile)
                implementation(libs.koin.android)
                implementation(libs.kotlinx.coroutines.android)
            }
        }
    }
}

atomicfu {
    transformJvm = false
}

compose.resources {
    publicResClass = true
    packageOfResClass = "${Configs.KLYX_NAMESPACE}.core.res"
    generateResClass = auto
}
