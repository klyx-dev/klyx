import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.detekt)
}

allprojects {
    apply {
        plugin("io.gitlab.arturbosch.detekt")
    }

    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.matching { it.name == "commonMain" }.all {
                languageSettings {
                    compilerOptions {
                        freeCompilerArgs.addAll("-Xexpect-actual-classes", "-Xcontext-parameters")
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

tasks.register("generateTsGrammar") {
    val tsks = subprojects.flatMap { sub ->
        sub.tasks.matching { it.name == "generateGrammarFiles" }
    }

    dependsOn(tsks)
}
