plugins {
    alias(libs.plugins.klyx.multiplatform)
    alias(libs.plugins.klyx.multiplatform.compose)
    alias(libs.plugins.klyx.buildConfig)
    alias(libs.plugins.kotlinx.atomicfu)
    alias(libs.plugins.ksp)
}

kotlin {
    android { namespace = "com.klyx.core" }

    sourceSets {
        val androidMain by getting
        val desktopMain by getting

        val commonJvmAndroid by creating {
            dependsOn(commonMain.get())
        }

        androidMain.dependsOn(commonJvmAndroid)
        desktopMain.dependsOn(commonJvmAndroid)

        commonMain {
            dependencies {
                api(libs.koin.compose)

                implementation(libs.compose.material.icons.extended)
                implementation(libs.compose.components.resources)

                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.androidx.lifecycle.runtime.compose)

                api(libs.kotlinx.serialization.json)
                implementation(libs.json5k)
                api(libs.tomlkt)
                implementation(libs.kotlinx.datetime)
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

                api(libs.filekit.dialogs)
                api(libs.filekit.dialogs.compose)

                api(libs.arrow.core)
                api(libs.arrow.fx.coroutines)

                implementation(libs.kfswatch)
                api(libs.semver)
                implementation(libs.multiplatform.settings.no.arg)
            }
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

atomicfu {
    transformJvm = false
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.klyx.core.res"
    generateResClass = auto
}
