import com.android.build.gradle.internal.api.ApkVariantOutputImpl

plugins {
    alias(libs.plugins.android.application)
    kotlin("android")
    kotlin("plugin.compose")
}

android {
    namespace = "com.klyx"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.klyx"
        minSdk = 26
        targetSdk = 36
        versionName = "1.3.0-beta.1"
        versionCode = calculateVersionCode(versionName!!)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        // required by sora-editor language-textmate
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // required by sora-editor language-textmate
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.google.fonts)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.documentfile)

    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.core)

    implementation(project(":core"))
    implementation(project(":editor"))
    implementation(project(":kwasm"))
    implementation(project(":extension-api"))

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

fun calculateVersionCode(versionName: String): Int {
    val semverRegex = Regex("""(\d+)\.(\d+)\.(\d+)(?:-(alpha|beta|rc)\.(\d+))?""")
    val match = semverRegex.matchEntire(versionName) ?: error("Invalid semver format: $versionName")

    val (majorStr, minorStr, patchStr, preReleaseType, preReleaseNumStr) = match.destructured

    val major = majorStr.toInt()
    val minor = minorStr.toInt()
    val patch = patchStr.toInt()
    val preReleaseNum = preReleaseNumStr.toIntOrNull() ?: 0

    val preReleaseOffset = when (preReleaseType) {
        "" -> 3_000 // stable
        "rc" -> 2_000
        "beta" -> 1_000
        "alpha" -> 0
        else -> error("Unknown pre-release type: $preReleaseType")
    }

    return major * 10_000_000 +
            minor * 100_000 +
            patch * 1_000 +
            preReleaseOffset + preReleaseNum
}

