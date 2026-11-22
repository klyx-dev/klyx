import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotest)
    alias(libs.plugins.koinCompiler)
    alias(libs.plugins.buildkonfig)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()
    jvm()

    android {
        namespace = "com.klyx.core"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }

        androidResources {
            enable = true
        }

        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.foundation)
            api(libs.compose.material3.adaptive)
            implementation(libs.compose.components.resources)

            api(platform(libs.koin.bom))
            api(libs.koin.core)
            api(libs.koin.annotations)
            api(libs.koin.compose)
            api(libs.koin.composeViewmodel)

            api(libs.kotlinx.io.core)
            api(libs.filekit.dialogs)

            implementation(libs.kotlinx.serialization.json)
        }
    }
}

fun platform(dependencyProvider: Provider<MinimalExternalModuleDependency>) =
    project.dependencies.platform(dependencyProvider)

compose.resources {
    packageOfResClass = "com.klyx.resources"
    nameOfResClass = "Resources"
    publicResClass = true
}

fun git(vararg args: String) = providers.exec {
    workingDir = rootProject.projectDir
    commandLine("git", *args)
}.standardOutput.asText.get().trim().takeIf { it.isNotBlank() }

buildkonfig {
    packageName = "com.klyx"
    objectName = "BuildInfo"
    exposeObjectWithName = "BuildInfo"

    fun escape(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")

    val commit = git("rev-parse", "HEAD")
        ?: System.getenv("GITHUB_SHA")
        ?: System.getenv("CI_COMMIT_SHA")
        ?: "unknown"

    val branch = git("branch", "--show-current")
        ?: System.getenv("GITHUB_REF_NAME")
        ?: System.getenv("CI_COMMIT_REF_NAME")
        ?: "unknown"

    defaultConfigs {
        buildConfigField(STRING, "COMMIT", escape(commit), const = true)
        buildConfigField(STRING, "SHORT_COMMIT", escape(git("rev-parse", "--short", "HEAD") ?: "unknown"), const = true)
        buildConfigField(STRING, "BRANCH", escape(branch), const = true)
    }
}
