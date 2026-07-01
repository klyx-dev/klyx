import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SourcesJar

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

mavenPublishing {
    coordinates(
        groupId = "io.github.klyx-dev",
        artifactId = "klyx-core",
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
