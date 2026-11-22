import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.koinCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

dependencies {
    implementation(project(":app:shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.material3)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

val signingProperties = project.file("sign.properties").takeIf { it.isFile }?.let { file ->
    Properties().apply { file.inputStream().use(::load) }
}

android {
    namespace = "com.klyx"

    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    flavorDimensions += "design"

    productFlavors {
        create("classic") {
            dimension = "design"
            versionNameSuffix = "-classic"
        }

        create("m3") {
            dimension = "design"
            applicationIdSuffix = ".m3"
            versionNameSuffix = "-m3"
        }
    }

    defaultConfig {
        applicationId = "com.klyx"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = providers.gradleProperty("klyxVersionCode").map { it.toInt() }.getOrElse(1)
        versionName = providers.gradleProperty("klyxVersionName").getOrElse("0.1.0-alpha.1")

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    signingConfigs {
        create("release") {
            if (signingProperties != null) {
                storeFile = File(signingProperties.getProperty("storeFile"))
                storePassword = signingProperties.getProperty("storePassword")
                keyAlias = signingProperties.getProperty("keyAlias")
                keyPassword = signingProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}
