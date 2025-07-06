import com.klyx.Configs
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidLibrary {
        namespace = "com.klyx.editor"
        compileSdk = Configs.Android.COMPILE_SDK_VERSION
        minSdk = Configs.Android.MIN_SDK_VERSION

        compilations.configureEach {
            compilerOptions.configure {
                jvmTarget.set(JvmTarget.JVM_21)
            }
        }
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
            }
        }
    }
}
