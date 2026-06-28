@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "Klyx"

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") {
            content {
                includeGroup("com.github.racra")
            }
        }
    }
}

includeBuild("external/sora-editor")

include(":app", ":terminal", ":editor", ":core", ":klyx-api")

file("languages").listFiles()?.filter { it.isDirectory && it.name.startsWith("tree-sitter-") }?.forEach { repoDir ->
    val innerGrammars = repoDir.listFiles { f -> f.isDirectory && (f.name == "typescript" || f.name == "tsx") }

    if (innerGrammars.isNullOrEmpty()) {
        include(":languages:${repoDir.name}")
    } else {
        innerGrammars.forEach { subFolder ->
            val projectName = if (subFolder.name == "typescript") repoDir.name else "tree-sitter-${subFolder.name}"
            include(":languages:$projectName")
            project(":languages:$projectName").projectDir = subFolder
        }
    }
}
