import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SourcesJar

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

mavenPublishing {
    coordinates(
        groupId = "io.github.klyx-dev",
        artifactId = "klyx-api",
        version = property("project.version") as String
    )
}

configure<MavenPublishBaseExtension> {
    pomFromGradleProperties()
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
    configure(
        AndroidSingleVariantLibrary(
            variant = "release",
            sourcesJar = SourcesJar.Sources(),
            javadocJar = JavadocJar.None()
        )
    )
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
}
