package com.klyx.terminal

import android.content.Context
import com.klyx.terminal.internal.currentUser
import java.io.File

context(context: Context)
val ubuntuDir get() = File(context.filesDir, "ubuntu")

context(context: Context)
val ubuntuHomeDir get() = ubuntuDir.resolve("home")

context(context: Context)
val userHomeDir get() = if (currentUser == null) null else ubuntuHomeDir.resolve(currentUser!!)

context(context: Context)
val klyxFilesDir: File get() = context.filesDir

context(context: Context)
val klyxCacheDir: File get() = context.cacheDir

context(context: Context)
val klyxLibDir: File get() = File(klyxFilesDir, "usr/lib")

context(context: Context)
val klyxLibExecDir: File get() = File(klyxFilesDir, "usr/libexec")

context(context: Context)
val klyxBinDir: File get() = File(klyxFilesDir, "usr/bin")
