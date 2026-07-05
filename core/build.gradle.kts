plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.vanniktech.publish)
}

android {
    namespace = "com.klyx.core"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 28

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "IS_DEBUG_MODE", "true")
        }

        release {
            buildConfigField("boolean", "IS_DEBUG_MODE", "false")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xreturn-value-checker=check",
        )
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.androidx.compose)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(kotlin("reflect"))

    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
    testImplementation(libs.bundles.kotest.unit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.bundles.kotest.android)
}
