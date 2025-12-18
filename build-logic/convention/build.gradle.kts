import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
    alias(libs.plugins.android.lint)
}

group = "com.klyx.buildlogic"

// Configure the build-logic plugins to target JDK 21
// This matches the JDK used to build the project, and is not related to what is running on device.
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

dependencies {
    compileOnly(libs.android.gradleApiPlugin)
    compileOnly(libs.android.tools.common)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    implementation(libs.buildkonfig.gradlePlugin)
    compileOnly(libs.buildkonfig.compiler)
    implementation(libs.semver)
    lintChecks(libs.androidx.lint.gradle)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.klyx.android.application.get().pluginId
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.klyx.android.library.get().pluginId
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidLint") {
            id = libs.plugins.klyx.android.lint.get().pluginId
            implementationClass = "AndroidLintConventionPlugin"
        }
        register("jvmLibrary") {
            id = libs.plugins.klyx.jvmLibrary.get().pluginId
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("root") {
            id = libs.plugins.klyx.root.get().pluginId
            implementationClass = "RootPlugin"
        }
        register("kotlinMultiplatform") {
            id = libs.plugins.klyx.multiplatform.asProvider().get().pluginId
            implementationClass = "KotlinMultiplatformConventionPlugin"
        }
        register("composeMultiplatform") {
            id = libs.plugins.klyx.multiplatform.compose.get().pluginId
            implementationClass = "ComposeMultiplatformConventionPlugin"
        }
        register("composeDesktop") {
            id = libs.plugins.klyx.compose.desktop.get().pluginId
            implementationClass = "ComposeDesktopConventionPlugin"
        }
        register("buildConfig") {
            id = libs.plugins.klyx.buildConfig.get().pluginId
            implementationClass = "BuildConfigConventionPlugin"
        }
    }
}
