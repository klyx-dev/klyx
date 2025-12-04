import com.klyx.Configs

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.ksp)
}

kotlin {

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
        }

        androidMain.get().dependsOn(commonAndroidJvm)
        jvmMain.get().dependsOn(commonAndroidJvm)

        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)

                implementation(libs.chasm)
                implementation(libs.bindings.chasm.wasip1)
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
    add("kspCommonMainMetadata", projects.wasmKsp)
    add("kspAndroid", projects.wasmKsp)
    add("kspJvm", projects.wasmKsp)
}
