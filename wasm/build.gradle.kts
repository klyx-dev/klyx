plugins {
    alias(libs.plugins.klyx.multiplatform)
    alias(libs.plugins.ksp)
}

kotlin {
    android { namespace = "com.klyx.wasm" }

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
                implementation(libs.anyhowkt)

                implementation(projects.core)
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
