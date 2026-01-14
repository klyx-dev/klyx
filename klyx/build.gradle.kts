plugins {
    alias(libs.plugins.klyx.multiplatform)
    alias(libs.plugins.klyx.multiplatform.compose)
    alias(libs.plugins.klyx.compose.desktop)
    alias(libs.plugins.kotlinx.atomicfu)
}

kotlin {
    android {
        namespace = "com.klyx"

        androidResources {
            enable = true
        }
    }

    sourceSets {
        val commonMain by getting
        val desktopMain by getting

        androidMain.dependencies {
            api(libs.androidx.activityCompose)
            api(libs.androidx.documentfile)
            api(libs.androidx.google.fonts)
            api(libs.androidx.material3)
            api(libs.koin.android)

            api(libs.material)
            api(libs.sora.language.textmate)

            api(projects.terminal.terminalView)
            api(projects.terminal.termuxShared)
        }

        commonMain.dependencies {
            implementation(libs.compose.components.resources)
            implementation(libs.androidx.material3.adaptive)
            implementation(libs.androidx.material3.adaptive.nav3)
            implementation(libs.fuzzywuzzy.kotlin)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.lifecycle.viewmodel.nav3)
            //implementation(libs.androidx.nav3.runtime)
            implementation(libs.androidx.nav3.ui)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.coil.compose)
            implementation(libs.coil.svg)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs)
            implementation(libs.filekit.dialogs.compose)
            implementation(libs.multiplatform.settings.no.arg)
            implementation(libs.semver)

            api(projects.core)
            api(projects.icons)
            api(projects.project)
            api(projects.lsp.server)
            api(projects.feature.mcp)
            api(projects.feature.extension)
            api(projects.feature.languageExtension)
            api(projects.editor.editor)
            api(projects.editor.language)
            api(projects.editor.languages)
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

compose {
    desktop.application {
        mainClass = "com.klyx.MainKt"
    }

    resources {
        generateResClass = always
        publicResClass = true
        packageOfResClass = "com.klyx.resources"
    }
}

tasks.register("assembleAllTargets") {
    group = "build"

    dependsOn(
        "assembleRelease",
        "packageDistributionForCurrentOS"
    )

    kotlin.targets.forEach { target ->
        if (target.name.contains("native") ||
            target.name.contains("linux") ||
            target.name.contains("mingw") ||
            target.name.contains("macos")
        ) {
            target.compilations.forEach { compilation ->
                if (compilation.name == "main") {
                    dependsOn("${target.name}MainBinaries")
                }
            }
        }
    }
}

tasks.register("prepareArtifacts") {
    group = "distribution"

    dependsOn("assembleAllTargets")
}

atomicfu {
    transformJvm = false
    jvmVariant = "VH"
}
