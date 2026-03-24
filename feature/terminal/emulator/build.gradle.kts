import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.klyx.multiplatform)
    alias(libs.plugins.klyx.multiplatform.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlinx.atomicfu)
    alias(libs.plugins.ksp)
}

kotlin {
    android { namespace = "com.klyx.terminal.emulator" }

    targets.withType<KotlinNativeTarget> {
        compilations.getByName("main") {
            cinterops.create("terminal") {
                definitionFile.set(project.file("src/nativeInterop/cinterop/terminal.def"))
                includeDirs("src/nativeInterop/include")

                extraOpts("-Xsource-compiler-option", "-Isrc/nativeInterop/include")
                project.file("src/nativeInterop/cpp").listFiles()?.filter { it.extension == "c" }?.forEach {
                    extraOpts("-Xcompile-source", it.absolutePath)
                }
            }
        }
    }

    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(projects.feature.terminal.android)
            }
        }

        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlin.logging)
                implementation(projects.util)
            }
        }
    }
}

atomicfu {
    transformJvm = false
}

val jvmNativeBuildDir = layout.buildDirectory.dir("jvmNative").get().asFile

val configureJvmNative = tasks.register<Exec>("configureJvmNative") {
    group = "build native"
    description = "Configures CMake for Desktop"

    doFirst {
        jvmNativeBuildDir.mkdirs()
    }

    workingDir = jvmNativeBuildDir
    commandLine("cmake", "../../src/nativeInterop/cpp", "-B", ".")
}

val buildJvmNative = tasks.register<Exec>("buildJvmNative") {
    group = "build native"
    description = "Compiles C code for Desktop via CMake"

    dependsOn(configureJvmNative)

    workingDir = jvmNativeBuildDir
    commandLine("cmake", "--build", ".", "--config", "Release")
}

val copyJvmNativeBinaries = tasks.register<Copy>("copyJvmNativeBinaries") {
    group = "build native"
    description = "Copies compiled native libraries to JVM resources"

    dependsOn(buildJvmNative)

    from(jvmNativeBuildDir) {
        include("**/*.dll", "**/*.so", "**/*.dylib")
    }
    into(layout.buildDirectory.dir("processedResources/jvm/main"))
}

tasks.named("jvmProcessResources") {
    dependsOn(copyJvmNativeBinaries)
}
