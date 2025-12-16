import com.codingfeline.buildkonfig.compiler.FieldSpec.Type
import com.klyx.AppVersioning
import com.klyx.Configs

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.atomicfu)
    alias(libs.plugins.kotest)
    alias(libs.plugins.ksp)
    alias(libs.plugins.buildkonfig)
}

kotlin {
    jvmToolchain(21)

    androidLibrary {
        namespace = "com.klyx.core"
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
            baseName = "core"
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonJvmAndroid by creating {
            dependsOn(commonMain.get())
        }

        androidMain.get().dependsOn(commonJvmAndroid)
        jvmMain.get().dependsOn(commonJvmAndroid)

        commonMain {
            dependencies {
                api(libs.koin.compose)

                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.material.icons.extended)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.ui.tooling.preview)

                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.androidx.lifecycle.runtime.compose)

                api(libs.kotlinx.serialization.json)
                implementation(libs.json5k)
                implementation(libs.ktoml.core)
                api(libs.ktor.client.core)
                api(libs.ktor.client.cio)
                api(libs.ktor.client.content.negotiation)
                api(libs.ktor.serialization.kotlinx.json)
                implementation(libs.commons.compress)
                api(libs.koin.core)
                api(libs.kmp.process)

                implementation(kotlin("reflect"))
                api(libs.kotlinx.io.core)
                api(libs.kotlinx.io.okio)
                api(libs.okio)
                api(libs.kotlinx.coroutines.core)

                api(libs.anyhowkt)
                api(libs.filekit.dialogs)
                api(libs.filekit.dialogs.compose)

                api(libs.arrow.core)
                api(libs.arrow.fx.coroutines)

                api(libs.kfswatch)
                implementation(libs.semver)
                implementation(libs.multiplatform.settings.no.arg)

                implementation(projects.shared)
                implementation(projects.wasm)
            }
        }

        commonJvmAndroid.dependencies {

        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotest.framework.engine)
            implementation(libs.kotest.assertions.core)
        }

        jvmTest.dependencies {
            implementation(libs.kotest.runner.junit5)
        }

        androidMain {
            dependencies {
                api(libs.utilcodex)
                api(libs.androidx.documentfile)
                implementation(libs.koin.android)
                implementation(libs.kotlinx.coroutines.android)
                implementation(libs.material)

                implementation(projects.terminal.terminalEmulator)
                implementation(projects.terminal.terminalView)
                implementation(projects.terminal.termuxShared)
            }
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
    filter {
        isFailOnNoMatchingTests = false
    }
}

atomicfu {
    transformJvm = false
}

compose.resources {
    publicResClass = true
    packageOfResClass = "${Configs.KLYX_NAMESPACE}.core.res"
    generateResClass = auto
}

val isReleaseBuild = gradle.startParameter.taskNames.any { task ->
    task.contains("Release", ignoreCase = true)
}

val buildType = if (isReleaseBuild) "RELEASE" else "DEBUG"

fun env(name: String): String? = System.getenv(name)

buildkonfig {
    packageName = "${Configs.KLYX_NAMESPACE}.core"
    objectName = "KlyxBuildConfig"
    exposeObjectWithName = "KlyxBuildConfig"

    defaultConfigs {
        buildConfigField(Type.STRING, "APP_NAME", "Klyx", const = true)
        buildConfigField(
            Type.STRING,
            "VERSION_NAME",
            project.findProperty("versionName") as? String ?: AppVersioning.resolveVersionName(buildType.lowercase()),
            const = true
        )
        buildConfigField(Type.INT, "VERSION_CODE", AppVersioning.versionCode.toString(), const = true)
        buildConfigField(Type.STRING, "APPLICATION_ID", Configs.KLYX_NAMESPACE, const = true)

        buildConfigField(Type.STRING, "BUILD_TYPE", buildType, const = true)
        buildConfigField(Type.BOOLEAN, "IS_DEBUG", (!isReleaseBuild).toString(), const = true)

        buildConfigField(Type.BOOLEAN, "CI", (env("CI") == "true").toString(), const = true)

        buildConfigField(Type.STRING, "CI_PROVIDER", env("CI_PROVIDER"), nullable = true)
        buildConfigField(Type.STRING, "CI_WORKFLOW", env("CI_WORKFLOW"), nullable = true)
        buildConfigField(Type.STRING, "CI_JOB", env("CI_JOB"), nullable = true)

        buildConfigField(Type.LONG, "CI_RUN_ID", env("CI_RUN_ID"), nullable = true)
        buildConfigField(Type.INT, "CI_RUN_NUMBER", env("CI_RUN_NUMBER"), nullable = true)
        buildConfigField(Type.INT, "CI_ATTEMPT", env("CI_ATTEMPT"), nullable = true)

        buildConfigField(Type.STRING, "CI_ACTOR", env("CI_ACTOR"), nullable = true)
        buildConfigField(Type.STRING, "CI_EVENT", env("CI_EVENT"), nullable = true)

        buildConfigField(Type.STRING, "GIT_COMMIT", env("GIT_COMMIT"), nullable = true)
        buildConfigField(Type.STRING, "GIT_COMMIT_SHORT", env("GIT_COMMIT")?.take(7), nullable = true)
        buildConfigField(Type.STRING, "GIT_BRANCH", env("GIT_BRANCH"), nullable = true)
        buildConfigField(Type.STRING, "GIT_REF", env("GIT_REF"), nullable = true)
        buildConfigField(Type.STRING, "GIT_REF_TYPE", env("GIT_REF_TYPE"), nullable = true)

        buildConfigField(Type.STRING, "GIT_REPO", env("GIT_REPO"), nullable = true)
        buildConfigField(Type.STRING, "GIT_REPO_OWNER", env("GIT_REPO_OWNER"), nullable = true)
        buildConfigField(Type.STRING, "GIT_REPO_NAME", env("GIT_REPO")?.substringAfter("/"), nullable = true)

        buildConfigField(Type.BOOLEAN, "IS_PULL_REQUEST", (env("IS_PULL_REQUEST") == "true").toString(), const = true)

        buildConfigField(
            Type.LONG,
            "BUILD_TIMESTAMP",
            (env("BUILD_TIMESTAMP") ?: System.currentTimeMillis().toString()),
            const = true
        )

        buildConfigField(Type.STRING, "CI_OS", env("CI_OS"), nullable = true)
        buildConfigField(Type.STRING, "CI_ARCH", env("CI_ARCH"), nullable = true)

        buildConfigField(Type.STRING, "KOTLIN_VERSION", libs.versions.kotlin.get(), const = true)

        buildConfigField(Type.BOOLEAN, "ENABLE_STRICT_MODE", (env("ENABLE_STRICT_MODE") != "false").toString(), const = true)
        buildConfigField(Type.BOOLEAN, "ENABLE_EXPERIMENTAL", (env("ENABLE_EXPERIMENTAL") == "true").toString(), const = true)
    }
}
