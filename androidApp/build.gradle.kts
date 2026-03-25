import com.android.build.gradle.internal.api.ApkVariantOutputImpl

plugins {
    alias(libs.plugins.klyx.android.application)
}

androidComponents {
    finalizeDsl { application ->
        application.apply {
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

            packaging {
                jniLibs {
                    useLegacyPackaging = true
                }
            }
        }
    }

    onVariants {
        it.outputs.forEach { output ->
            (output as? ApkVariantOutputImpl)?.let {
                output.outputFileName = output.outputFileName.replace(project.name, "klyx")
            }
        }
    }
}

dependencies {
    implementation(projects.klyx)
}
