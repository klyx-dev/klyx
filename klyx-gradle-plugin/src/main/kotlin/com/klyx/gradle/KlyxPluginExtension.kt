package com.klyx.gradle

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class KlyxPluginExtension @Inject constructor(private val project: Project) {
    /** Path to the plugin descriptor JSON. Defaults to project root 'plugin.json' */
    abstract val pluginJsonFile: RegularFileProperty

    /** Path to the plugin icon. Auto-matches case-insensitive 'icon.png' or 'icon.jpg' */
    abstract val icon: RegularFileProperty

    /** Path to the plugin documentation. Auto-matches case-insensitive 'readme.md' */
    abstract val readme: RegularFileProperty

    /** Path to the plugin release log. Auto-matches case-insensitive 'changelog.md' */
    abstract val changelog: RegularFileProperty

    /** Collection of extra files or directories to pack into the root of the bundle */
    abstract val extraFiles: ConfigurableFileCollection

    /** Alternative name for the output bundle. Defaults to project name */
    abstract val outputFileName: Property<String>

    /** Target folder for built bundles. Defaults to 'build/klyx/' */
    abstract val outputDirectory: DirectoryProperty

    /** Whether to automatically push the bundle to device's klyx/plugins directory using adb */
    abstract val autoPushToDevice: Property<Boolean>

    fun enableCompose() {
        project.pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        project.plugins.withId("com.android.application") { configureAndroidCompose() }
        project.plugins.withId("com.android.library") { configureAndroidCompose() }
    }

    private fun configureAndroidCompose() {
        project.extensions.configure(CommonExtension::class.java) { android ->
            android.buildFeatures.compose = true
        }
    }
}
