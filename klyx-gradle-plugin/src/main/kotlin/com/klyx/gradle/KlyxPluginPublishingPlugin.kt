package com.klyx.gradle

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar
import java.io.File

class KlyxPluginPublishingPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

        val extension = target.extensions.create("klyx", KlyxPluginExtension::class.java, target)

        extension.pluginJsonFile.convention(target.rootProject.layout.projectDirectory.file("plugin.json"))
        extension.outputFileName.convention(target.provider { target.rootProject.name })
        extension.outputDirectory.convention(target.layout.buildDirectory.dir("klyx"))
        extension.autoPushToDevice.convention(false)

        val bundleDebug = target.tasks.register("klyxBundleDebug", Tar::class.java) { task ->
            task.group = "klyx"
            task.description = "Packages the debug variant into a valid .klyx distribution archive."
            task.dependsOn("assembleDebug")
            setupBaseTarProperties(task, extension)
        }

        val bundleRelease = target.tasks.register("klyxBundleRelease", Tar::class.java) { task ->
            task.group = "klyx"
            task.description = "Packages the release variant into a valid .klyx distribution archive."
            task.dependsOn("assembleRelease")
            setupBaseTarProperties(task, extension)
        }

        target.tasks.register("klyxBundle") { task ->
            task.group = "klyx"
            task.description = "Convenience alias targeting the klyxBundleRelease distribution task flow."
            task.dependsOn(bundleRelease)
        }

        // Wire APK from build outputs using a file tree to handle signed/unsigned naming.
        listOf(
            "debug" to bundleDebug,
            "release" to bundleRelease,
        ).forEach { (variant, taskProvider) ->
            taskProvider.configure { task ->
                task.from(target.layout.buildDirectory.dir("outputs/apk/$variant")) { copy ->
                    copy.include("*.apk")
                    copy.rename { "plugin.apk" }
                }
            }
        }

        listOf(
            "Debug" to bundleDebug,
            "Release" to bundleRelease,
        ).forEach { (variant, bundleTaskProvider) ->
            val pushTask = target.tasks.register("klyxPush$variant") { task ->
                task.group = "klyx"
                task.description = "Pushes the $variant bundle to a connected device."
                task.doLast {
                    if (extension.autoPushToDevice.get()) {
                        pushBundleToDevice(target, bundleTaskProvider.get().archiveFile.get().asFile)
                    }
                }
            }
            bundleTaskProvider.configure { it.finalizedBy(pushTask) }
        }

        target.plugins.withId("com.android.application") { configureAndroid(target) }

        target.afterEvaluate {
            val rootFiles = target.rootProject.projectDir.listFiles() ?: emptyArray()
            if (!extension.readme.isPresent) {
                rootFiles.firstOrNull { it.name.equals("readme.md", ignoreCase = true) }
                    ?.let { extension.readme.set(it) }
            }
            if (!extension.changelog.isPresent) {
                rootFiles.firstOrNull { it.name.equals("changelog.md", ignoreCase = true) }
                    ?.let { extension.changelog.set(it) }
            }
            if (!extension.icon.isPresent) {
                rootFiles.firstOrNull {
                    it.name.equals("icon.png", ignoreCase = true) || it.name.equals("icon.jpg", ignoreCase = true)
                }?.let { extension.icon.set(it) }
            }

            listOf(bundleDebug, bundleRelease).forEach { taskProvider ->
                taskProvider.configure { task ->
                    val jsonFile = extension.pluginJsonFile.asFile.get()
                    if (!jsonFile.exists()) {
                        throw GradleException("Klyx compilation failed: Missing descriptor file mapping at '${jsonFile.absolutePath}'.")
                    }
                    task.from(jsonFile)

                    val iconFile = extension.icon.orNull?.asFile
                    if (iconFile != null && iconFile.exists()) {
                        task.from(iconFile) { copy ->
                            copy.rename { "icon.${iconFile.extension}" }
                        }
                    }

                    val readmeFile = extension.readme.orNull?.asFile
                    if (readmeFile != null && readmeFile.exists()) {
                        task.from(readmeFile) { copy -> copy.rename { "readme.md" } }
                    }

                    val changelogFile = extension.changelog.orNull?.asFile
                    if (changelogFile != null && changelogFile.exists()) {
                        task.from(changelogFile) { copy -> copy.rename { "changelog.md" } }
                    }

                    extension.extraFiles.files.forEach { file ->
                        if (file.exists()) {
                            if (file.isDirectory) {
                                task.from(file) { copy -> copy.into(file.name) }
                            } else {
                                task.from(file)
                            }
                        }
                    }
                }
            }
        }

        target.plugins.withId("com.android.library") { configureAndroid(target) }
    }

    private fun configureAndroid(project: Project) {
        project.extensions.configure(CommonExtension::class.java) { android ->
            android.apply {
                defaultConfig.apply {
                    minSdk {
                        version = release(28)
                    }
                }

                compileOptions.apply {
                    sourceCompatibility = JavaVersion.VERSION_21
                    targetCompatibility = JavaVersion.VERSION_21
                }
            }
        }
    }

    private fun setupBaseTarProperties(task: Tar, extension: KlyxPluginExtension) {
        task.archiveExtension.set("klyx")
        task.compression = Compression.GZIP
        task.archiveBaseName.set(extension.outputFileName)
        task.destinationDirectory.set(extension.outputDirectory)
    }

    private fun pushBundleToDevice(project: Project, bundleFile: File) {
        val adbExecutable = try {
            val android = project.extensions.findByName("android")
            val getAdbExecutable = android?.javaClass?.getMethod("getAdbExecutable")
            (getAdbExecutable?.invoke(android) as? File) ?: File("adb")
        } catch (_: Exception) {
            File("adb")
        }

        try {
            val devicesOutput = project.providers.exec { spec ->
                spec.commandLine(adbExecutable, "devices")
            }.standardOutput.asText.get()

            val devices = devicesOutput.lines()
                .map { it.trim() }
                .filter { it.endsWith("\tdevice") }

            if (devices.size == 1) {
                val deviceId = devices[0].split("\t")[0]
                project.logger.lifecycle("Klyx: Pushing ${bundleFile.name} to device $deviceId...")
                try {
                    project.providers.exec { spec ->
                        spec.commandLine(adbExecutable, "-s", deviceId, "shell", "mkdir", "-p", "/sdcard/klyx/plugins/")
                    }.result.get()
                    project.providers.exec { spec ->
                        spec.commandLine(
                            adbExecutable,
                            "-s",
                            deviceId,
                            "push",
                            bundleFile.absolutePath,
                            "/sdcard/klyx/plugins/"
                        )
                    }.result.get()
                } catch (e: Exception) {
                    project.logger.error("Klyx: Failed to push bundle to device: ${e.message}")
                }
            } else if (devices.size > 1) {
                project.logger.warn("Klyx: Multiple devices connected, skipping auto-push.")
            }
        } catch (e: Exception) {
            project.logger.warn("Klyx: Failed to run adb. Is it in your PATH?", e)
        }
    }
}
