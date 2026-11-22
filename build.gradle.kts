import io.kotest.framework.gradle.KotestGradleExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.koin.compiler.plugin.KoinGradleExtension

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.koinCompiler) apply false
    alias(libs.plugins.kotest) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.buildkonfig) apply false
}

subprojects {
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
        compilerOptions {
            allWarningsAsErrors = true

            freeCompilerArgs.addAll(
                "-Wextra",
                "-Xcollection-literals",
                "-Xconsistent-data-class-copy-visibility", // generates copy() with the same visibility as the constructor
                "-Xexplicit-context-arguments",
                "-Xintrinsic-const-evaluation",
                "-Xname-based-destructuring=complete",
                "-Xrender-internal-diagnostic-names",
                "-Xreturn-value-checker=full",
                "-Xwarning-level=OVERRIDE_DEPRECATION:error",
                "-Xwarning-level=REDUNDANT_ELSE_IN_WHEN:error",
                "-Xwarning-level=UNCHECKED_CAST:error", // At least suppress + explain why it's safe (where the check happened)
                "-Xwarning-level=UNUSED_VARIABLE:error",
                "-Xwarning-level=REDUNDANT_VISIBILITY_MODIFIER:disabled"
            )

            optIn.addAll(
                "kotlin.contracts.ExperimentalContracts",
                "kotlin.time.ExperimentalTime"
            )
        }
    }

    pluginManager.withPlugin("io.kotest") {
        pluginManager.apply(libs.plugins.ksp.get().pluginId)

        extensions.configure<KotestGradleExtension> {
            enablePowerAssert = true
        }

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets {
                named("commonTest") {
                    dependencies {
                        implementation(libs.kotest.framework.engine)
                        implementation(libs.kotest.assertions.core)
                    }
                }

                matching { it.name == "jvmTest" }.configureEach {
                    dependencies {
                        implementation(libs.kotest.runner.junit5)
                    }
                }
            }
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            filter {
                isFailOnNoMatchingTests = false
            }
        }

        tasks.withType<AbstractTestTask>().configureEach {
            outputs.upToDateWhen { false }
        }
    }

    pluginManager.withPlugin("io.insert-koin.compiler.plugin") {
        extensions.configure<KoinGradleExtension> {
            versionCheckSeverity = "info"
            logSeverity = "info"
        }
    }
}
