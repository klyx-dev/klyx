plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.vanniktech.publish)
}

android {
    namespace = "com.klyx.lsp.server"

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

dependencies {
    api(libs.kotlinx.io.core)
    api(projects.lsp.api)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
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
