import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.klyx.AppVersioning
import com.klyx.Configs
import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
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

    linuxX64 {
        binaries {
            executable {
                entryPoint = "main"
                baseName = "klyx"
            }
        }
    }

    mingwX64 {
        binaries {
            executable {
                entryPoint = "main"
                baseName = "klyx"
            }
        }
    }

    macosX64 {
        binaries {
            executable {
                entryPoint = "main"
                baseName = "klyx"
            }
        }
    }

    macosArm64 {
        binaries {
            executable {
                entryPoint = "main"
                baseName = "klyx"
            }
        }
    }

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
            implementation(projects.terminal.termuxShared)
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

            implementation(libs.lsp4j)
            implementation(libs.lsp4j.jsonrpc)

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

        versionCode = AppVersioning.versionCode
        versionName = AppVersioning.resolveVersionName("release")
    }

    signingConfigs {
        create("release") {
            val isCI = System.getenv("GITHUB_ACTIONS")?.toBoolean() ?: false

            val propPath = if (isCI) {
                "/tmp/sign.properties"
            } else {
                "/home/vivek/klyx/key/sign.properties"
            }

            val propFile = File(propPath)
            if (propFile.exists()) {
                val properties = Properties().also {
                    it.load(propFile.inputStream())
                }

                keyAlias = properties.getProperty("keyAlias")
                keyPassword = properties.getProperty("keyPassword")
                storeFile = if (isCI) {
                    File("/tmp/klyx.keystore")
                } else {
                    File(properties.getProperty("storeFile"))
                }
                storePassword = properties.getProperty("storePassword")
            } else {
                println("Sign properties file not found at $propPath")
            }
        }

        getByName("debug") {
            storeFile = file(rootProject.file("composeApp/testkey.keystore"))
            storePassword = "testkey"
            keyAlias = "testkey"
            keyPassword = "testkey"
        }
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
            signingConfig = signingConfigs.getByName("release")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        getByName("debug") {
            versionNameSuffix = "+" + AppVersioning.DEBUG_SUFFIX
            applicationIdSuffix = AppVersioning.DEBUG_SUFFIX

            signingConfig = signingConfigs.getByName("debug")

            resValue("string", "app_name", "klyx - ${AppVersioning.DEBUG_SUFFIX}")
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

tasks.register<ShadowJar>("shadowJar") {
    archiveBaseName.set("klyx")
    archiveClassifier.set("all")
    archiveVersion.set(AppVersioning.stableVersionName)

    from(kotlin.jvm("desktop").compilations.getByName("main").output)
    configurations = listOf(
        project.configurations.getByName("desktopRuntimeClasspath")
    )

    manifest {
        attributes["Main-Class"] = "${Configs.KLYX_PACKAGE_NAME}.MainKt"
    }

    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    mergeServiceFiles()
}

compose.desktop {
    application {
        mainClass = "${Configs.KLYX_PACKAGE_NAME}.MainKt"

        nativeDistributions {
            targetFormats(
                TargetFormat.Dmg,
                TargetFormat.Msi,
                TargetFormat.Deb,
                TargetFormat.Rpm,
                TargetFormat.AppImage,
                TargetFormat.Exe
            )
            packageName = "klyx"
            packageVersion = AppVersioning.stableVersionName
            description = "Klyx - Kotlin Multiplatform Code Editor"
            copyright = "Klyx"
            vendor = "Klyx"

            linux {
                iconFile.set(project.file("src/commonMain/resources/icon.png"))
                packageName = "klyx"
                menuGroup = "Development"
                appCategory = "Development"
            }

            windows {
                iconFile.set(project.file("src/commonMain/resources/icon.ico"))
                packageName = "Klyx"
                dirChooser = true
                perUserInstall = true
                menuGroup = "Development"
            }

            macOS {
                iconFile.set(project.file("src/commonMain/resources/icon.icns"))
                packageName = "Klyx"
                dmgPackageVersion = AppVersioning.stableVersionName
                packageBuildVersion = AppVersioning.versionCode.toString()
                appCategory = "public.app-category.developer-tools"
            }
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "${Configs.KLYX_NAMESPACE}.res"
    generateResClass = auto
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/config/detekt/detekt.yml")
    baseline = file("$projectDir/config/detekt/baseline.xml")
}

tasks.withType<Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(true)
        sarif.required.set(true)
        md.required.set(true)
    }
}

tasks.register("assembleAllTargets") {
    group = "build"

    dependsOn(
        "assembleRelease",
        "shadowJar",
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

tasks.register<Zip>("createPortableArchive") {
    group = "distribution"

    dependsOn("shadowJar")

    from(tasks.named("shadowJar"))
    from("README.md") {
        into("docs")
    }
    from("LICENSE") {
        into("docs")
    }

    archiveBaseName.set("klyx")
    archiveVersion.set(AppVersioning.stableVersionName)
    archiveClassifier.set("portable")
    destinationDirectory.set(file("${layout.buildDirectory}/distributions"))
}

tasks.register("prepareArtifacts") {
    group = "distribution"

    dependsOn(
        "assembleAllTargets",
        "createPortableArchive"
    )
}
