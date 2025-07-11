import com.klyx.Configs

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(21)

    androidLibrary {
        namespace = "com.klyx.editor"
        compileSdk = Configs.Android.COMPILE_SDK_VERSION
        minSdk = Configs.Android.MIN_SDK_VERSION
    }

    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.ktreesitter)
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
                implementation(project.dependencies.platform("io.github.rosemoe:editor-bom:0.23.7"))
                implementation("io.github.rosemoe:editor")
                implementation("io.github.rosemoe:editor-lsp")
                implementation("io.github.rosemoe:language-treesitter")
                implementation("com.itsaky.androidide.treesitter:tree-sitter-json:4.3.1")
            }
        }
    }
}
