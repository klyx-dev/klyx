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
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.material3)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.ui.text.google.fonts)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)

    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.collections.immutable)
    api(libs.androidx.documentfile)
    api(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.utilcodex)

    api(projects.core)
    api(projects.terminal)
    api(projects.lsp.api)
    api(projects.lsp.server)
}
