plugins {
    alias(libs.plugins.klyx.multiplatform)
    alias(libs.plugins.klyx.multiplatform.compose)
    alias(libs.plugins.kotlinx.atomicfu)
}

kotlin {
    android {
        namespace = "com.klyx.editor"

        androidResources {
            enable = true
        }
    }

    sourceSets {
        val nonAndroidMain by creating {
            dependsOn(commonMain.get())
        }

        iosMain.get().dependsOn(nonAndroidMain)
        jvmMain.get().dependsOn(nonAndroidMain)

        commonMain {
            dependencies {
                implementation(libs.koin.core)
                implementation(libs.koin.compose)

                implementation(libs.compose.material.icons.extended)
                implementation(libs.compose.components.resources)

                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.androidx.lifecycle.runtime.compose)

                implementation(libs.multiplatform.markdown.renderer)
                implementation(libs.multiplatform.markdown.renderer.m3)

                implementation(kotlin("reflect"))

                implementation(libs.androidx.collection)

                implementation(projects.core)
                implementation(projects.lsp.server)
                implementation(projects.extensionApi)

//                rootProject.project("tree-sitter").subprojects.forEach {
//                    implementation(project(":tree-sitter:${it.name}"))
//                }
            }
        }

        androidMain {
            dependencies {
                implementation(libs.androidx.material3)

                api(project.dependencies.platform(libs.sora.editor.bom))
                api(libs.sora.editor)
                implementation(libs.sora.oniguruma.native)
                implementation(libs.sora.language.textmate)

                implementation(libs.androidx.emoji2)

                //implementation(libs.ktreesitter)
            }
        }

        iosMain {
            dependencies {
                implementation(libs.jetbrains.skiko)
            }
        }

        jvmMain {
            dependencies {
                implementation(libs.jetbrains.skiko)
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

atomicfu {
    transformJvm = false
}
