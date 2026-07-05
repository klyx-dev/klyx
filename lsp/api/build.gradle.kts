plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.vanniktech.publish)
}

android {
    namespace = "com.klyx.lsp.api"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 28
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xreturn-value-checker=check",
            "-opt-in=kotlin.contracts.ExperimentalContracts",
            "-opt-in=kotlin.uuid.ExperimentalUuidApi",
        )
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    api(libs.kotlinx.io.core)
}
