import com.klyx.Configs
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinx.atomicfu)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {

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

            dependencies {
                implementation(libs.commons.compress)
            }
        }

        androidMain.get().dependsOn(commonJvmAndroid)
        jvmMain.get().dependsOn(commonJvmAndroid)

        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)

            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

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
    add("kspCommonMainMetadata", projects.wasmKsp)
}

atomicfu {
    transformJvm = false
}

kotlin.sourceSets.commonMain {
    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    dependsOn("kspCommonMainKotlinMetadata")
}
