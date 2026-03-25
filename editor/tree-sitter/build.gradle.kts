plugins {
    alias(libs.plugins.android.library)
}

androidComponents {
    finalizeDsl {
        it.apply {
            namespace = "com.klyx.editor.treesitter"
            compileSdk = 36

            defaultConfig {
                minSdk {
                    version = release(26)
                }

                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                consumerProguardFiles("consumer-rules.pro")

                @Suppress("UnstableApiUsage")
                externalNativeBuild {
                    cmake {
                        cppFlags += ""
                    }
                }
            }

            buildTypes {
                release {
                    isMinifyEnabled = false
                    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
                }
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }

            externalNativeBuild {
                cmake {
                    path = file("src/main/cpp/CMakeLists.txt")
                    version = "3.22.1"
                }
            }
        }
    }
}

dependencies {
    api(libs.ktreesitter)

    implementation(platform(libs.sora.editor.bom))
    implementation(libs.sora.language.treesitter)
    implementation(libs.sora.editor)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
