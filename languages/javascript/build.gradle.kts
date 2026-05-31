plugins {
    alias(libs.plugins.ktreesitter)
    alias(libs.plugins.android.library)
}

val grammarDir = projectDir.resolve("tree-sitter-javascript")
version = grammarDir.resolve("Makefile")
    .useLines { lines ->
        lines.first { it.startsWith("VERSION := ") }
            .removePrefix("VERSION := ")
    }

grammar {
    baseDir = grammarDir
    grammarName = project.name
    className = "TreeSitterJavascript"
    packageName = "com.klyx.languages.${grammarName.get()}"
}

val generateTask = tasks.generateGrammarFiles.get()

android {
    namespace = "com.klyx.languages.${grammar.grammarName.get()}"
    ndkVersion = property("ndk.version") as String

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 28
        resValue("string", "version", version as String)
    }

    externalNativeBuild {
        cmake {
            path = generateTask.cmakeListsFile.get().asFile
            buildStagingDirectory = file(".cmake")
            version = property("cmake.version") as String
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        resValues = true
    }

    sourceSets {
        getByName("main") {
            val generatedSrc = generateTask.generatedSrc.get().asFile
            kotlin.directories += generatedSrc.resolve("androidMain/kotlin").absolutePath
        }
    }
}

tasks.generateGrammarFiles.configure {
    doLast {
        val file = generatedSrc.get().asFile.resolve("jni/binding.c")
        val originalContent = file.readText()
        val cleanedContent = originalContent.replace(
            "#include <tree-sitter-javascript.h>",
            "#include <tree_sitter/tree-sitter-javascript.h>"
        )

        if (originalContent != cleanedContent) file.writeText(cleanedContent)
    }
}
