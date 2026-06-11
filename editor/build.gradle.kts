plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotest)
}

android {
    namespace = "com.klyx.editor"

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

    packaging {
        resources {
            excludes += "META-INF/AL2.0"
            excludes += "META-INF/LGPL2.1"
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
            "-Xreturn-value-checker=full",
        )
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.sora.editor)
    implementation(libs.ktreesitter)

    rootProject.project("languages").subprojects.forEach {
        implementation(project(":languages:${it.name}"))
    }

    testImplementation(libs.junit)
    testImplementation(libs.bundles.kotest.unit)

    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.bundles.kotest.android)
}
