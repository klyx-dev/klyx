plugins {
    alias(libs.plugins.android.library)
}

androidComponents {
    finalizeDsl {
        it.apply {
            namespace = "com.klyx.terminal.c"

            compileSdk {
                version = release(36)
            }

            defaultConfig {
                minSdk {
                    version = release(26)
                }

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
