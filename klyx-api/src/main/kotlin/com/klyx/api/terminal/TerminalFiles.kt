package com.klyx.api.terminal

import com.klyx.api.data.fs.Paths
import com.klyx.api.util.applicationContext
import java.io.File

/**
 * The root filesystem (rootfs) directory for the terminal environment.
 */
val Paths.rootFs by lazy { Paths.dataDir.canonicalFile.resolve("rootfs") }

/**
 * The home directory for the terminal.
 */
val Paths.home get() = filesDir.canonicalFile.resolve("home")

/**
 * The directory where user projects are stored, located within the terminal home.
 */
val Paths.projects get() = home.resolve("projects")

/**
 * The file that stores the current version of the terminal bootstrap.
 */
val Paths.versionFile get() = rootFs.resolve(".bootstrap-version")

/**
 * Returns the file path to the native PRoot executable.
 */
fun prootFile() = File(applicationContext().applicationInfo.nativeLibraryDir, "libproot.so")

/**
 * Returns the file path to the native PRoot loader library.
 */
fun prootLoaderFile() = File(applicationContext().applicationInfo.nativeLibraryDir, "libloader.so")
