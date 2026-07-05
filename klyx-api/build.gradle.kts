plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.vanniktech.publish)
}

android {
    namespace = "com.klyx.api"

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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("String", "VERSION_NAME", "\"$${project.property("project.version")}\"")
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
    api(platform(libs.androidx.compose.bom))
    api(libs.bundles.androidx.compose)
    api(libs.androidx.ui.text.google.fonts)
    api(libs.androidx.material.icons.extended)

    api(platform(libs.koin.bom))
    api(libs.koin.core)
    api(libs.koin.android)

    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.collections.immutable)
    api(libs.androidx.documentfile)
    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.utilcodex)

    api(projects.core)
    api(projects.terminal)
    api(projects.lsp.api)
    api(projects.lsp.server)
}
