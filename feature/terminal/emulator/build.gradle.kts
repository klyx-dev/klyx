import gobley.gradle.GobleyHost
import gobley.gradle.cargo.dsl.android
import gobley.gradle.cargo.dsl.jvm
import gobley.gradle.cargo.profiles.CargoProfile
import gobley.gradle.rust.targets.RustPosixTarget
import gobley.gradle.rust.targets.RustWindowsTarget

plugins {
    //alias(libs.plugins.klyx.multiplatform)
    alias(libs.plugins.klyx.rust)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.klyx.multiplatform.compose)
    alias(libs.plugins.kotest)
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.klyx.terminal.emulator"
    compileSdk = 36
    ndkVersion = libs.versions.ndkVersion.get()

    defaultConfig {
        minSdk = 26
        ndk.abiFilters += setOf("arm64-v8a", "x86_64")
    }

    buildTypes {
        debug {
            isJniDebuggable = true
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }
}

cargo {
    builds.jvm {
        if (GobleyHost.Platform.Windows.isCurrent) {
            when (rustTarget) {
                RustWindowsTarget.X64 -> embedRustLibrary = false
                RustPosixTarget.MinGWX64 -> embedRustLibrary = true
                else -> {}
            }
        } else {
            embedRustLibrary = rustTarget == GobleyHost.current.rustTarget
        }
    }

    builds.android {
        debug.buildTaskProvider.configure {
            additionalEnvironment.put("RUST_BACKTRACE", 1)
            additionalEnvironment.put("RUSTFLAGS", "-C debuginfo=2")
        }
    }
}

uniffi {
    generateFromLibrary {
        packageName = "com.klyx.terminal.native"
    }
}

kotlin {
    //android { namespace = "com.klyx.extension" }
    @Suppress("DEPRECATION")
    androidTarget()

    jvm()

    if (GobleyHost.Platform.MacOS.isCurrent) {
        iosArm64()
        iosSimulatorArm64()
        iosX64()
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlin.logging)
                implementation(projects.util)
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotest.framework.engine)
            implementation(libs.kotest.assertions.core)
        }
    }

    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.uuid.ExperimentalUuidApi",
            "-opt-in=kotlin.contracts.ExperimentalContracts",
            "-opt-in=kotlin.ExperimentalUnsignedTypes",
            "-Xcontext-parameters",
            "-Xreturn-value-checker=check",
            "-Xexplicit-backing-fields",
            "-Xexpect-actual-classes"
        )
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
    filter {
        isFailOnNoMatchingTests = false
    }
}
