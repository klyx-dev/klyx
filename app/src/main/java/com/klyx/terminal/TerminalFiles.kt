package com.klyx.terminal

import com.klyx.data.fs.Paths
import com.klyx.api.util.applicationContext
import java.io.File

val Paths.rootFs by lazy { Paths.dataDir.canonicalFile.resolve("rootfs") }

val Paths.home get() = filesDir.canonicalFile.resolve("home")
val Paths.projects get() = home.resolve("projects")

val Paths.versionFile get() = rootFs.resolve(".bootstrap-version")

fun prootFile() = File(applicationContext().applicationInfo.nativeLibraryDir, "libproot.so")
fun prootLoaderFile() = File(applicationContext().applicationInfo.nativeLibraryDir, "libloader.so")
