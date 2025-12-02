import com.klyx.Configs

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kotlinxAtomicfu)
}

kotlin {

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
        val nonAndroidMain by creating {
            dependsOn(commonMain.get())
        }

        iosMain.get().dependsOn(nonAndroidMain)
        jvmMain.get().dependsOn(nonAndroidMain)

        commonMain {
            dependencies {
                implementation(libs.koin.core)
                implementation(libs.koin.compose)

                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.materialIconsExtended)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.uiToolingPreview)

                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.androidx.lifecycle.runtimeCompose)

                implementation(libs.multiplatform.markdown.renderer)
                implementation(libs.multiplatform.markdown.renderer.m3)

                implementation(kotlin("reflect"))

                implementation(libs.androidx.collection)

                implementation(projects.core)
                implementation(projects.shared)
                implementation(projects.extensionApi)

//                rootProject.project("tree-sitter").subprojects.forEach {
//                    implementation(project(":tree-sitter:${it.name}"))
//                }
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.androidx.material3)

                api(project.dependencies.platform(libs.sora.editor.bom))
                api(libs.sora.editor)
                implementation(libs.sora.oniguruma.native)
                implementation(libs.sora.language.textmate)

                implementation(libs.lsp4j)
                implementation(libs.lsp4j.jsonrpc)

                implementation(libs.androidx.emoji2)

                //implementation(libs.ktreesitter)
            }
        }

        iosMain {
            dependencies {
                implementation("org.jetbrains.skiko:skiko:0.9.37.3")
            }
        }

        jvmMain {
            dependencies {
                implementation("org.jetbrains.skiko:skiko:0.9.37.3")
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

atomicfu {
    transformJvm = false
}
