@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "Knox"

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

include(":app", ":terminal", ":editor", ":core", ":knox-api")
include(":knox-gradle-plugin", ":knox-compiler-plugin")
include(":lsp:api", ":lsp:server")
include(":i18n:processor", ":i18n:runtime", ":i18n:strings")

// Map new logical project names to existing directories so the repo remains buildable
project(":knox-api").projectDir = file("klyx-api")
project(":knox-gradle-plugin").projectDir = file("klyx-gradle-plugin")
project(":knox-compiler-plugin").projectDir = file("klyx-compiler-plugin")

file("languages").listFiles()?.filter { it.isDirectory && it.name.startsWith("tree-sitter-") }?.forEach { repoDir ->
    val innerGrammars = repoDir.listFiles { f -> f.isDirectory && (f.name == "typescript" || f.name == "tsx") }

    if (innerGrammars.isNullOrEmpty()) {
        include(":languages:${'$'}{repoDir.name}")
    } else {
        innerGrammars.forEach { subFolder ->
            val projectName = if (subFolder.name == "typescript") repoDir.name else "tree-sitter-${'$'}{subFolder.name}"
            include(":languages:$projectName")
            project(":languages:$projectName").projectDir = subFolder
        }
    }
}
