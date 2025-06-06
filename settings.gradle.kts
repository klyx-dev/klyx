@file:Suppress("UnstableApiUsage")

pluginManagement {
    includeBuild("ktreesitter-plugin")

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
    plugins {
        kotlin("jvm") version "2.1.21"
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS

    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Klyx"
include(":app")
include(":core")
include(":editor")
include(":kwasm")
include(":extension-api")

file("tree-sitter").listFiles { file -> file.isDirectory }?.forEach {
    include(":tree-sitter:${it.name}")
}
