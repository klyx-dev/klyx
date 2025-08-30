import com.klyx.Configs

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xcontext-parameters", "-Xcontext-sensitive-resolution")
    }

    jvmToolchain(21)

    androidLibrary {
        namespace = "com.klyx.editor"
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
            baseName = "editor"
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.koin.core)
                implementation(libs.koin.compose)

                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)

                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.androidx.lifecycle.runtimeCompose)

                implementation(kotlin("reflect"))

                implementation(projects.core)
                implementation(projects.shared)
                implementation(projects.terminal.terminal)

//                rootProject.project("tree-sitter").subprojects.forEach {
//                    implementation(project(":tree-sitter:${it.name}"))
//                }
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                api(project.dependencies.platform(libs.sora.editor.bom))
                api(projects.editor.sora.editor)
                api(projects.editor.sora.editorLsp)

                implementation(libs.sora.language.textmate)
                implementation("io.github.rosemoe:language-treesitter")
                implementation("com.itsaky.androidide.treesitter:tree-sitter-json:4.3.1")

                implementation(libs.lsp4j)
                implementation(libs.lsp4j.jsonrpc)

                implementation(libs.ktreesitter)
            }
        }
    }
}
