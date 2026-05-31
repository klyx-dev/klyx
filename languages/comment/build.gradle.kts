plugins {
    alias(libs.plugins.ktreesitter)
    alias(libs.plugins.android.library)
}

val grammarDir = projectDir.resolve("tree-sitter-comment")
version = grammarDir.resolve("Makefile")
    .useLines { lines ->
        lines.first { it.startsWith("VERSION := ") }
            .removePrefix("VERSION := ")
    }

grammar {
    baseDir = grammarDir
    grammarName = project.name
    className = "TreeSitterComment"
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
        val src = generatedSrc.get().asFile
        val binding = src.resolve("jni/binding.c")
        val originalContent = binding.readText()
        val cleanedContent = originalContent.replace(
            "#include <tree-sitter-comment.h>",
            "#include <tree_sitter/tree-sitter-comment.h>"
        )

        if (originalContent != cleanedContent) binding.writeText(cleanedContent)

        val cmakeFile = src.resolveSibling("CMakeLists.txt")
        val original = cmakeFile.readText()

        val modified = original.replace(
            $$"include_directories(${JNI_INCLUDE_DIRS} ../../tree-sitter-comment/bindings/c)",
            $$"""
            include_directories(
                ${JNI_INCLUDE_DIRS}
                ../../tree-sitter-comment/bindings/c
                ../../tree-sitter-comment/src
            )
            """.trimIndent()
        )

        if (original != modified) {
            cmakeFile.writeText(modified)
        }
    }
}
