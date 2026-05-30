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
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        @Suppress("UnstableApiUsage")
        externalNativeBuild {
            cmake {
                cppFlags += ""
            }
        }

        buildConfigField("String", "COMPOSE_VERSION", "\"${libs.versions.compose.bom.get()}\"")
        buildConfigField("String", "TREESITTER_VERSION", "\"${libs.versions.ktreesitter.get()}\"")
    }

    signingConfigs {
        val releaseConfig by creating {
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
        val releaseConfig by signingConfigs.getting

        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = releaseConfig
        }

        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "Klyx [D]")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true

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
            version = "3.22.1"
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.generateKotlin", "true")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xcontext-parameters",
            "-Xexplicit-backing-fields",
            "-Xreturn-value-checker=check",
            "-Xcontext-sensitive-resolution",
            "-opt-in=kotlin.contracts.ExperimentalContracts",
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
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.nav3.runtime)
    implementation(libs.androidx.nav3.ui)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.material)

    // Icons
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.annotations)

    implementation(libs.kotlinx.collections.immutable)

    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    implementation(libs.androidx.documentfile)
    implementation(libs.utilcodex)

    implementation(libs.sora.editor)
    implementation(libs.sora.editor.compose)
    implementation(libs.sora.oniguruma.native)

    implementation(libs.arrow.core)
    implementation(libs.arrow.fx.coroutines)

    implementation(libs.smooth.corner.rect.android.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.zoomable)

    implementation(projects.core)
    implementation(projects.editor)

    testImplementation(libs.junit)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.kotest.runner.junit4)
    androidTestImplementation(libs.kotest.assertions.core)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
