import com.android.build.gradle.internal.api.ApkVariantOutputImpl

plugins {
    alias(libs.plugins.klyx.android.application)
}

android {
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }

        debug {
            // signingConfig = signingConfigs.getByName("debug")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "x86_64")
            isUniversalApk = true
        }
    }

    defaultConfig {
        ndk.abiFilters += setOf("arm64-v8a", "x86_64")
    }

    applicationVariants.configureEach {
        outputs.configureEach {
            (this as? ApkVariantOutputImpl)?.let {
                outputFileName = outputFileName.replace(project.name, "klyx")
            }
        }
    }
}

dependencies {
    debugImplementation(libs.compose.ui.tooling.preview)

    implementation(projects.klyx)
}
