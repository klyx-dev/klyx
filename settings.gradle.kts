@file:Suppress("UnstableApiUsage")

rootProject.name = "Klyx"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("ktreesitter-plugin")

    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":composeApp")
include(":shared")
include(":core")
include(":editor")
include(":kwasm")
include(":extension-api")
include(":wasm")

//file("tree-sitter").listFiles { file -> file.isDirectory && file.name != "build" }?.forEach {
//    include(":tree-sitter:${it.name}")
//}
