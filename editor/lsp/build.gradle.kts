import com.klyx.Configs
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinxAtomicfu)
}

android {
    namespace = "com.klyx.editor.lsp"
    compileSdk = Configs.Android.COMPILE_SDK_VERSION

    defaultConfig {
        minSdk = Configs.Android.MIN_SDK_VERSION

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
        freeCompilerArgs.addAll("-Xcontext-parameters")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

//    implementation(platform(libs.sora.editor.bom))
//    implementation(libs.sora.editor)

    implementation(libs.lsp4j)
    implementation(libs.lsp4j.jsonrpc)

    implementation(projects.core)
    implementation(projects.editor.sora.editor)

    implementation(projects.extensionApi)
    implementation(projects.terminal.terminal)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

atomicfu {
    transformJvm = true
}
