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
        val desktopMain by getting

        val nonAndroidMain by creating {
            dependsOn(commonMain.get())
        }

        iosMain.get().dependsOn(nonAndroidMain)
        desktopMain.dependsOn(nonAndroidMain)

        commonMain {
            dependencies {
                implementation(libs.compose.components.resources)

                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.androidx.lifecycle.runtime.compose)

                implementation(libs.multiplatform.markdown.renderer)
                implementation(libs.multiplatform.markdown.renderer.m3)

                implementation(kotlin("reflect"))

                implementation(libs.androidx.collection)

                implementation(projects.core)
                implementation(projects.icons)
                implementation(projects.lsp.server)
                implementation(projects.project)
                implementation(projects.editor.language)
                implementation(projects.feature.extension)

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
                implementation(projects.feature.extension)

                //implementation(libs.ktreesitter)
            }
        }

        iosMain {
            dependencies {
                implementation(libs.jetbrains.skiko)
            }
        }

        desktopMain.dependencies {
            implementation(libs.jetbrains.skiko)
        }

        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

atomicfu {
    transformJvm = false
}

compose.resources {
    packageOfResClass = "com.klyx.editor.resources"
    generateResClass = auto
}
