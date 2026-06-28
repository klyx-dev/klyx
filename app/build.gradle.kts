import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.koin.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotest)
}

android {
    namespace = "com.klyx"
    ndkVersion = property("ndk.version") as String

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.klyx"
        minSdk = 28
        targetSdk = 37
        versionCode = (property("project.versionCode") as String).toInt()
        versionName = property("project.version") as String

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        @Suppress("UnstableApiUsage")
        externalNativeBuild {
            cmake {
                cppFlags += ""
            }
        }

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }

        buildConfigField("String", "COMPOSE_VERSION", "\"${libs.versions.compose.bom.get()}\"")
        buildConfigField("String", "TREESITTER_VERSION", "\"${libs.versions.ktreesitter.get()}\"")
    }

    signingConfigs {
        create("release") {
            val isCI = System.getenv("GITHUB_ACTIONS")?.toBoolean() ?: false

            val propPath = if (isCI) {
                "/tmp/sign.properties"
            } else {
                "${System.getenv("HOME")}/klyx/key/sign.properties"
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
    }

    buildTypes {
        val releaseConfig = signingConfigs.getByName("release")

        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = releaseConfig
        }

        debug {
            //applicationIdSuffix = ".debug"
            signingConfig = releaseConfig
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "Klyx [D]")
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "x86_64")
            isUniversalApk = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }

    packaging {
        resources {
            excludes += "META-INF/AL2.0"
            excludes += "META-INF/LGPL2.1"
        }

        jniLibs {
            useLegacyPackaging = true
        }
    }

    externalNativeBuild {
        cmake {
            path = file("CMakeLists.txt")
            version = property("cmake.version") as String
        }
    }
}

ksp {
    arg("room.schemaLocation", layout.buildDirectory.file("generated/room/schemas").get().asFile.absolutePath)
    arg("room.incremental", "true")
    arg("room.generateKotlin", "true")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xreturn-value-checker=check",
            "-Xcontext-sensitive-resolution",
            "-opt-in=kotlin.contracts.ExperimentalContracts",
            "-opt-in=kotlin.uuid.ExperimentalUuidApi",
            "-opt-in=io.github.rosemoe.sora.compose.ExperimentalEditorApi",
        )
    }

    jvmToolchain(21)
}

koinCompiler {
    userLogs = true
    debugLogs = false
    unsafeDslChecks = true
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.androidx.compose)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.bundles.navigation3)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.material)

    // Icons
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)

    implementation(platform(libs.koin.bom))
    implementation(libs.bundles.koin)

    implementation(libs.kotlinx.collections.immutable)

    implementation(libs.bundles.room)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.documentfile)
    implementation(libs.utilcodex)

    implementation(libs.bundles.sora.editor)
    implementation(libs.bundles.ktor)

    implementation(libs.bundles.arrow)
    implementation(libs.apache.commons.compress)
    implementation(libs.xz)

    implementation(libs.smooth.corner.rect.android.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.zoomable)

    implementation(projects.klyxApi)
    implementation(projects.core)
    implementation(projects.terminal)
    implementation(projects.editor)

    testImplementation(libs.junit)
    testImplementation(libs.bundles.kotest.unit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.bundles.kotest.android)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
