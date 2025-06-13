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
include(":rope")

file("tree-sitter").listFiles { file -> file.isDirectory && file.name != "build" }?.forEach {
    include(":tree-sitter:${it.name}")
}
