import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.klyx.buildlogic.configureKotlin
import com.klyx.buildlogic.libs
import com.klyx.buildlogic.toPascalCase
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KotlinMultiplatformConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.multiplatform")
            apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
            apply(plugin = "com.android.kotlin.multiplatform.library")
            apply(plugin = "io.kotest")

            extensions.configure<KotlinMultiplatformExtension> {
                (this as ExtensionAware).extensions.configure(
                    "android",
                    Action<KotlinMultiplatformAndroidLibraryTarget> {
                        compileSdk = 36
                        minSdk = 26
                    }
                )

                jvm()

                listOf(
                    iosArm64(),
                    iosX64(),
                    iosSimulatorArm64()
                ).forEach { target ->
                    target.binaries.framework {
                        baseName = project.name.toPascalCase()
                        isStatic = true
                    }
                }
                applyDefaultHierarchyTemplate()

                sourceSets {
                    commonMain.dependencies {
                        implementation(libs.findLibrary("kotlinx-coroutines-core").get())
                    }

                    commonTest.dependencies {
                        implementation(libs.findLibrary("kotlinx-coroutines-test").get())
                        implementation(libs.findLibrary("kotest-framework-engine").get())
                        implementation(libs.findLibrary("kotest-assertions-core").get())
                    }

                    jvmTest.dependencies {
                        implementation(libs.findLibrary("kotest-runner-junit5").get())
                    }
                }
            }

            configureKotlin<KotlinMultiplatformExtension>()

            tasks.named<Test>("jvmTest") {
                useJUnitPlatform()
                filter {
                    isFailOnNoMatchingTests = false
                }
            }
        }
    }
}
