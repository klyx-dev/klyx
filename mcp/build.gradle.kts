plugins {
    alias(libs.plugins.klyx.multiplatform)
    alias(libs.plugins.klyx.android.lint)
}

kotlin {
    android { namespace = "com.klyx.mcp" }

    sourceSets {
        commonMain {
            dependencies {
                implementation("io.modelcontextprotocol:kotlin-sdk-client:0.7.4")
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
