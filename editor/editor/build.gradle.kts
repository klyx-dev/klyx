import com.klyx.Configs

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xcontext-parameters", "-Xcontext-sensitive-resolution")
    }

    jvmToolchain(21)

    androidTarget()
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
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)

                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.androidx.lifecycle.runtimeCompose)

                implementation(kotlin("reflect"))

                implementation(projects.core)
                implementation(projects.shared)
                implementation(projects.terminal.terminal)
                implementation(projects.extensionApi)

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
                implementation(libs.androidx.material3)

                api(project.dependencies.platform(libs.sora.editor.bom))
                api(libs.sora.editor)
                api(projects.editor.lsp)

                implementation(libs.sora.language.textmate)

                implementation(libs.lsp4j)
                implementation(libs.lsp4j.jsonrpc)

                //implementation(libs.ktreesitter)
            }
        }
    }
}

android {
    namespace = "com.klyx.editor"
    compileSdk = Configs.Android.COMPILE_SDK_VERSION

    defaultConfig {
        minSdk = Configs.Android.MIN_SDK_VERSION

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }
}
