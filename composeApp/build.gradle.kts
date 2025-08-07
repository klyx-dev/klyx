import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import com.klyx.Configs
import com.klyx.Version
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xcontext-parameters")
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.documentfile)
            implementation(libs.androidx.google.fonts)
            implementation(libs.androidx.material3)
            implementation(libs.koin.android)

            implementation(libs.material)
            implementation(libs.sora.language.textmate)
            implementation("com.itsaky.androidide.treesitter:android-tree-sitter:4.3.1")

            implementation(projects.terminal.terminalView)
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.uiUtil)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.24.0")
            implementation("org.eclipse.lsp4j:org.eclipse.lsp4j.jsonrpc:0.24.0")

            implementation(projects.shared)
            implementation(projects.core)
            implementation(projects.editor.editor)
            implementation(projects.extensionApi)
            implementation(projects.wasm)
            implementation(projects.terminal.terminal)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

android {
    namespace = Configs.KLYX_NAMESPACE
    compileSdk = Configs.Android.COMPILE_SDK_VERSION

    defaultConfig {
        applicationId = Configs.Android.APPLICATION_ID
        minSdk = Configs.Android.MIN_SDK_VERSION
        targetSdk = Configs.Android.TARGET_SDK_VERSION
        versionCode = Version.AppVersionCode
        versionName = Version.VERSION_NAME
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    applicationVariants.all {
        val variant = this
        outputs.all {
            if (this is ApkVariantOutputImpl) {
                outputFileName = "klyx-${variant.name}-v${versionName}.apk"
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }

        getByName("debug") {
            versionNameSuffix = Version.DEBUG_VERSION_SUFFIX
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.klyx.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = Configs.KLYX_PACKAGE_NAME
            packageVersion = Version.STABLE_VERSION_NAME
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "${Configs.KLYX_NAMESPACE}.res"
    generateResClass = auto
}
