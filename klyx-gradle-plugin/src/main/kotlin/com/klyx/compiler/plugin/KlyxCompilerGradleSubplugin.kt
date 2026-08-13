package com.klyx.compiler.plugin

import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.klyx.compiler.plugin.BuildConfig.KLYX_API_LIBRARY_COORDINATES
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlinx.serialization.gradle.SerializationGradleSubplugin
import java.io.File
import java.util.concurrent.TimeUnit

@Suppress("unused")
class KlyxCompilerGradleSubplugin : KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        val pluginManager = target.pluginManager
        pluginManager.apply(SerializationGradleSubplugin::class.java)

        val extension = target.extensions.create("klyx", KlyxPluginExtension::class.java, target)

        extension.library.convention(false)
        extension.compose.convention(true)

        val pluginJson = target.layout.buildDirectory.file("klyx/generated/plugin.json")
        extension.outputFileName.convention(target.provider { target.rootProject.name })
        extension.outputDirectory.convention(target.layout.buildDirectory.dir("klyx"))
        extension.autoPushToDevice.convention(false)

        if (extension.library.get()) {
            pluginManager.apply(LibraryPlugin::class.java)
        } else {
            pluginManager.apply(AppPlugin::class.java)
        }

        if (extension.compose.get()) {
            pluginManager.apply(ComposeCompilerGradleSubplugin::class.java)
        }

        fun Project.configureAndroidPlugin() {
            configureAndroid(this)

            target.afterEvaluate {
                if (extension.compose.get()) {
                    extensions.configure(CommonExtension::class.java) { android ->
                        android.buildFeatures.compose = true
                    }
                }
            }
        }

        val bundleDebug = target.tasks.register("klyxBundleDebug", Tar::class.java) { task ->
            task.group = "klyx"
            task.description = "Packages the debug variant into a valid .klyx distribution archive."
            task.dependsOn(target.provider { target.tasks.named("assembleDebug") })
            task.dependsOn(target.provider { target.tasks.named("compileDebugKotlin") })
            setupBaseTarProperties(task, extension)
        }

        val bundleRelease = target.tasks.register("klyxBundleRelease", Tar::class.java) { task ->
            task.group = "klyx"
            task.description = "Packages the release variant into a valid .klyx distribution archive."
            task.dependsOn(target.provider { target.tasks.named("assembleRelease") })
            task.dependsOn(target.provider { target.tasks.named("compileReleaseKotlin") })
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

        target.plugins.withType(AppPlugin::class.java) { target.configureAndroidPlugin() }
        target.plugins.withType(LibraryPlugin::class.java) { target.configureAndroidPlugin() }

        val projectDir = target.rootProject.layout.projectDirectory
        extension.readme.convention(
            target.provider {
                listOf("readme.md", "README.md", "Readme.md").map { projectDir.file(it) }
                    .firstOrNull { it.asFile.exists() }
            }
        )
        extension.changelog.convention(
            target.provider {
                listOf("changelog.md", "CHANGELOG.md", "Changelog.md").map { projectDir.file(it) }
                    .firstOrNull { it.asFile.exists() }
            }
        )

        listOf(bundleDebug, bundleRelease).forEach { taskProvider ->
            taskProvider.configure { task ->
                task.doFirst {
                    val jsonFile = pluginJson.get().asFile
                    if (!jsonFile.exists()) {
                        throw GradleException(
                            "Klyx: no plugin descriptor found at '${jsonFile.absolutePath}'. " +
                                    "Annotate your KlyxPlugin implementation with @PluginManifest(...); " +
                                    "the plugin.json is generated automatically and cannot be overridden."
                        )
                    }
                }

                task.from(target.provider { pluginJson.get().asFile })

                val iconTarget = target.provider {
                    val jsonFile = pluginJson.get().asFile
                    if (!jsonFile.exists()) return@provider emptyList<Pair<File, String>>()

                    val iconPath = readDescriptorIcon(jsonFile) ?: return@provider emptyList()
                    val projectRoot = target.rootProject.projectDir
                    val iconFile = projectRoot.resolve(iconPath)
                    val isInsideProject = iconFile.canonicalPath.startsWith(projectRoot.canonicalPath + File.separator)

                    if (iconPath.isBlank() || !iconFile.isFile || !isInsideProject) {
                        emptyList()
                    } else {
                        listOf(iconFile to iconPath)
                    }
                }

                task.from(iconTarget.map { list -> list.map { it.first } }) { copy ->
                    copy.eachFile { fileDetails ->
                        val iconPath = iconTarget.get().firstOrNull()?.second ?: return@eachFile
                        val dir = iconPath.substringBeforeLast('/', "")
                        if (dir.isNotEmpty() && dir != iconPath) {
                            fileDetails.path = "$dir/${fileDetails.name}"
                        }
                    }
                }

                task.from(extension.readme.map { listOf(it.asFile) }.orElse(emptyList())) { copy ->
                    copy.rename { "readme.md" }
                }

                task.from(extension.changelog.map { listOf(it.asFile) }.orElse(emptyList())) { copy ->
                    copy.rename { "changelog.md" }
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

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val descriptorDir = project.layout.buildDirectory.dir("klyx/generated")

        kotlinCompilation.defaultSourceSet.dependencies {
            compileOnly(KLYX_API_LIBRARY_COORDINATES)
        }

        if (BuildConfig.KOTLIN_PLUGIN_VERSION.contains("SNAPSHOT", ignoreCase = true)) {
            project.configurations.all {
                it.resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)
            }
        }

        kotlinCompilation.compileTaskProvider.configure {
            // Run this compiler plugin before Compose plugin.
            it.compilerOptions.freeCompilerArgs.add("-Xcompiler-plugin-order=${BuildConfig.KOTLIN_PLUGIN_ID}>androidx.compose.compiler.plugins.kotlin")
        }

        return project.provider {
            val descriptorIcon = project.rootProject.projectDir.listFiles()?.firstOrNull {
                it.name.lowercase() == "icon.png" || it.name.lowercase() == "icon.jpg"
            }?.name.orEmpty()

            listOf(
                SubpluginOption(
                    key = "descriptorOutputDir",
                    value = descriptorDir.get().asFile.absolutePath
                ),
                SubpluginOption(
                    key = "descriptorIcon",
                    value = descriptorIcon
                )
            )
        }
    }

    override fun getCompilerPluginId(): String = BuildConfig.KOTLIN_PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = BuildConfig.KOTLIN_PLUGIN_GROUP,
        artifactId = BuildConfig.KOTLIN_PLUGIN_NAME,
        version = BuildConfig.KOTLIN_PLUGIN_VERSION
    )

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

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

    private fun readDescriptorIcon(jsonFile: File): String? = try {
        Json.parseToJsonElement(jsonFile.readText())
            .jsonObject["icon"]
            ?.jsonPrimitive
            ?.contentOrNull
            ?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
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
