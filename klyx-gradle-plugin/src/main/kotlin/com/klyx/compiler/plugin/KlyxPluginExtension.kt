package com.klyx.compiler.plugin

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class KlyxPluginExtension @Inject constructor(private val project: Project) {
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

    abstract val library: Property<Boolean>
    abstract val compose: Property<Boolean>
}
