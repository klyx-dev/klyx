import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.androidLint) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.kotlinAndroid) apply false

    id("io.gitlab.arturbosch.detekt") version "1.23.8" apply false
}

allprojects {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.matching { it.name == "commonMain" }.all {
                languageSettings {
                    @OptIn(ExperimentalKotlinGradlePluginApi::class)
                    compilerOptions {
                        freeCompilerArgs.addAll("-Xexpect-actual-classes")
                    }
                }
            }
        }
    }
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    tasks.withType<Detekt>().configureEach {
        config = files("$rootDir/config/detekt/detekt.yml")
        buildUponDefaultConfig = true

        ignoreFailures = false
        jvmTarget = "21"

        reports {
            html.required.set(true)
            xml.required.set(true)
            txt.required.set(true)
            sarif.required.set(true)
            md.required.set(true)
        }
    }

    tasks.matching { it.name.startsWith("compile") }.configureEach {
        dependsOn("detekt")
    }
}

tasks.register("printTargets") {
    doLast {
        val targets = mutableListOf<String>()
        subprojects {
            plugins.withId("org.jetbrains.kotlin.multiplatform") {
                extensions.findByType<KotlinMultiplatformExtension>()?.let {
                    it.targets.forEach { target ->
                        targets.add(target.name)
                    }
                }
            }
        }
        println(targets.joinToString(","))
    }
}
