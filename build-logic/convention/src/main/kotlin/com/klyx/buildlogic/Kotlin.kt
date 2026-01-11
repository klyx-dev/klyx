package com.klyx.buildlogic

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Configure base Kotlin options
 */
internal inline fun <reified T : KotlinBaseExtension> Project.configureKotlin() = configure<T> {
    when (this) {
        is KotlinAndroidExtension -> compilerOptions
        is KotlinJvmExtension -> compilerOptions
        is KotlinMultiplatformExtension -> compilerOptions
        else -> TODO("Unsupported project extension $this ${T::class}")
    }.apply {
        freeCompilerArgs.addAll(
            // Enable experimental coroutines APIs, including Flow
            //"-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            // Enable experimental UUID APIs
            "-opt-in=kotlin.uuid.ExperimentalUuidApi",
            // Enable experimental contracts APIs
            "-opt-in=kotlin.contracts.ExperimentalContracts",
            // Enable experimental unsigned types APIs
            "-opt-in=kotlin.ExperimentalUnsignedTypes",
            // Enable context parameters feature for Kotlin
            "-Xcontext-parameters",
            // Enable unused return value checker
            "-Xreturn-value-checker=check",
            "-Xexplicit-backing-fields"
        )
    }

    when (this) {
        is KotlinMultiplatformExtension -> {
            compilerOptions.freeCompilerArgs.add(
                // Disable expect/actual classes warning for Kotlin Multiplatform
                "-Xexpect-actual-classes"
            )
        }
    }
}

/**
 * Configure base Kotlin options for JVM (non-Android)
 */
internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    configureKotlin<KotlinJvmProjectExtension>()
}
