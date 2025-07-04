import com.android.build.api.dsl.androidLibrary
import com.klyx.Configs
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidLibrary {
        namespace = "com.klyx.core"
        compileSdk = Configs.Android.COMPILE_SDK_VERSION
        minSdk = Configs.Android.MIN_SDK_VERSION

        compilations.configureEach {
            compilerOptions.configure {
                jvmTarget.set(JvmTarget.JVM_21)
            }
        }
    }

    jvm()

    sourceSets {
        val commonMain by getting
        val jvmMain by getting
        val androidMain by getting

        val commonJvmMain by creating {
            dependsOn(commonMain)

            dependencies {
                implementation(kotlin("stdlib"))
            }
        }

        androidMain.dependsOn(commonJvmMain)
        jvmMain.dependsOn(commonJvmMain)

        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.json5k)
            implementation(libs.ktoml.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.commons.compress)
            api(libs.koin.core)
            api(libs.koin.compose)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(kotlin("reflect"))
            api(libs.kotlinx.datetime)
            api(libs.kotlinx.io.core)
            implementation(libs.okio)
            implementation(libs.kotlinx.coroutines.core)

            implementation(projects.shared)

            // circular dependency
            //implementation(projects.editor)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        androidMain {
            dependencies {
                api(libs.utilcodex)
                api(libs.androidx.documentfile)
                implementation(libs.koin.android)
                implementation(libs.kotlinx.coroutines.android)
            }
        }
    }
}
