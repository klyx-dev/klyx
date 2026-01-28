import gobley.gradle.GobleyHost
import gobley.gradle.cargo.dsl.jvm
import gobley.gradle.rust.targets.RustPosixTarget
import gobley.gradle.rust.targets.RustWindowsTarget

plugins {
    //alias(libs.plugins.klyx.multiplatform)
    alias(libs.plugins.klyx.rust)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotest)
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.klyx.extension"
    compileSdk = 36
    ndkVersion = libs.versions.ndkVersion.get()

    defaultConfig {
        minSdk = 26
        ndk.abiFilters += setOf("arm64-v8a", "x86_64")
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
}

uniffi {
    generateFromLibrary {
        packageName = "com.klyx.extension.native"

        customType("Version") {
            typeName = "Version"
            lift = "Version.parse({})"
            lower = "{}.toString()"
            imports = listOf("io.github.z4kn4fein.semver.Version")
        }
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
                implementation(libs.tomlkt)

                implementation(projects.core)
                implementation(projects.util)
                implementation(projects.lsp.api)
                implementation(projects.editor.language)
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
