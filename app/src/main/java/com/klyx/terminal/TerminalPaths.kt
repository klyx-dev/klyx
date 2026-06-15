package com.klyx.terminal

import android.annotation.SuppressLint
import com.klyx.data.fs.Paths
import com.klyx.util.applicationContext
import java.io.File

@delegate:SuppressLint("SdCardPath")
val Paths.rootFs by lazy { File("/data/data/${applicationContext().packageName}/files") }

val Paths.prefix get() = rootFs.resolve("usr")
val Paths.home get() = rootFs.resolve("home")
val Paths.projects get() = home.resolve("projects")
val Paths.bin get() = prefix.resolve("bin")
val Paths.tmp get() = prefix.resolve("tmp")

val Paths.versionFile get() = prefix.resolve(".bootstrap-version")
