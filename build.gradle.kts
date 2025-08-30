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
    alias(libs.plugins.detekt)
}

val detektFormatting = libs.detekt.formatting

allprojects {
    apply {
        plugin("io.gitlab.arturbosch.detekt")
    }

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

    detekt {
        config.from(rootProject.files("config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
        ignoreFailures = false
    }

    tasks.withType<Detekt>().configureEach {
        exclude("**/build/**")
        exclude {
            it.file.relativeTo(projectDir).startsWith("build")
        }
    }

//    dependencies {
//        detektPlugins(detektFormatting)
//    }
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

tasks.register("detektAll") {
    allprojects {
        this@register.dependsOn(tasks.withType<Detekt>())
    }
}
