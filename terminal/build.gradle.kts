plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotest)
    alias(libs.plugins.vanniktech.publish)
}

android {
    namespace = "com.klyx.terminal"

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

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "META-INF/AL2.0"
            excludes += "META-INF/LGPL2.1"
        }
    }

    externalNativeBuild {
        cmake {
            path = file("CMakeLists.txt")
            version = property("cmake.version") as String
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xcontext-parameters",
            "-Xexplicit-backing-fields",
            "-Xreturn-value-checker=check",
        )
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.androidx.compose)

    implementation(libs.kotlinx.io.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.bundles.kotest.unit)

    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.bundles.kotest.android)
}
