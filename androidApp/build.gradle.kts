import com.android.build.gradle.internal.api.ApkVariantOutputImpl

plugins {
    alias(libs.plugins.klyx.android.application)
}

android {
    applicationVariants.all {
        val variant = this
        outputs.all {
            if (this is ApkVariantOutputImpl) {
                outputFileName = "klyx-${variant.name}-v${versionName}.apk"
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }

        debug {
            // signingConfig = signingConfigs.getByName("debug")
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

dependencies {
    debugImplementation(libs.compose.ui.tooling.preview)

    implementation(projects.klyxApp)
}
