import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.klyx.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinx.atomicfu)
}

kotlin {
    android { namespace = "com.klyx.extension" }

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
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation(projects.core)
            implementation(projects.shared)
            implementation(projects.wasm)
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
