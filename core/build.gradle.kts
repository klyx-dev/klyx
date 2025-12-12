import com.klyx.Configs

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.atomicfu)
    alias(libs.plugins.kotest)
    alias(libs.plugins.ksp)
}

kotlin {
    jvmToolchain(21)

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
            dependencies {
                api(libs.koin.compose)

                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.material.icons.extended)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.ui.tooling.preview)

                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.androidx.lifecycle.runtime.compose)

                api(libs.kotlinx.serialization.json)
                implementation(libs.json5k)
                implementation(libs.ktoml.core)
                api(libs.ktor.client.core)
                api(libs.ktor.client.cio)
                api(libs.ktor.client.content.negotiation)
                api(libs.ktor.serialization.kotlinx.json)
                implementation(libs.commons.compress)
                api(libs.koin.core)
                api(libs.kmp.process)

                implementation(kotlin("reflect"))
                api(libs.kotlinx.io.core)
                api(libs.kotlinx.io.okio)
                api(libs.okio)
                api(libs.kotlinx.coroutines.core)

                api(libs.anyhowkt)
                api(libs.filekit.dialogs)
                api(libs.filekit.dialogs.compose)

                api(libs.arrow.core)
                api(libs.arrow.fx.coroutines)

                api(libs.kfswatch)
                implementation(libs.semver)
                implementation(libs.multiplatform.settings.no.arg)

                implementation(projects.shared)
                implementation(projects.wasm)
            }
        }

        commonJvmAndroid.dependencies {

        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotest.framework.engine)
            implementation(libs.kotest.assertions.core)
        }

        jvmTest.dependencies {
            implementation(libs.kotest.runner.junit5)
        }

        androidMain {
            dependencies {
                api(libs.utilcodex)
                api(libs.androidx.documentfile)
                implementation(libs.koin.android)
                implementation(libs.kotlinx.coroutines.android)
                implementation(libs.material)
                implementation(libs.ktor.client.okhttp)

                implementation(projects.terminal.terminalEmulator)
                implementation(projects.terminal.terminalView)
                implementation(projects.terminal.termuxShared)
            }
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
    filter {
        isFailOnNoMatchingTests = false
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
