import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val grammarDir = projectDir.resolve("tree-sitter-kotlin")

version = grammarDir.resolve("Makefile").readLines().first {
    it.startsWith("VERSION := ")
}.removePrefix("VERSION := ")

plugins {
    alias(libs.plugins.android.library)
    kotlin("android")
    id("com.klyx.ktreesitter")
}

grammar {
    baseDir = grammarDir
    grammarName = project.name
    className = "TreeSitterKotlin"
    packageName = "com.klyx.treesitter.kotlin"
    libraryName = "klyx-treesitter-${project.name}"
    includes = arrayOf("kotlin.h")
    files = arrayOf(
        grammarDir.resolve("src/scanner.c"),
        grammarDir.resolve("src/parser.c"),
    )
    includeDirs = arrayOf(
        grammarDir.resolve("bindings/c"),
        grammarDir.resolve("bindings/swift/TreeSitterKotlin"),
    )
}

val generateTask = tasks.generateGrammarFiles.get()

android {
    namespace = "com.klyx.treesitter.${grammar.grammarName.get()}"
    compileSdk = 36

    defaultConfig {
        minSdk = 26

        ndk {
            moduleName = grammar.libraryName.get()
            //noinspection ChromeOsAbiSupport
            abiFilters += setOf("x86_64", "arm64-v8a", "armeabi-v7a")
        }
        resValue("string", "version", version as String)
    }

    externalNativeBuild {
        cmake {
            path(generateTask.cmakeListsFile.get().asFile)

            buildStagingDirectory = file(".cmake")
            version = "3.22.1"
        }
    }

    sourceSets {
        named("main") {
            val generatedSrc = generateTask.generatedSrc.get()

            kotlin.srcDir(generatedSrc.dir("androidMain").dir("kotlin"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn(generateTask)
}
