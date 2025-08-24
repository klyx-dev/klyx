import com.klyx.Configs

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xcontext-parameters", "-Xcontext-sensitive-resolution")
    }

    jvmToolchain(21)

    androidLibrary {
        namespace = "com.klyx.extension"
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
            baseName = "extensionApi"
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonJvmAndroid by creating {
            dependsOn(commonMain.get())
        }

        androidMain.get().dependsOn(commonJvmAndroid)
        jvmMain.get().dependsOn(commonJvmAndroid)

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)

            implementation(libs.kotlinx.datetime)

            implementation(libs.chasm)
            implementation(libs.bindings.chasm.wasip1)

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

dependencies {
    add("kspCommonMainMetadata", project(":wasm-ksp"))
    add("kspAndroid", project(":wasm-ksp"))
}
