package com.klyx.terminal

import android.content.Context
import com.klyx.terminal.internal.currentUser
import java.io.File

context(context: Context)
val sandboxHomeDir get() = sandboxDir.resolve("home")

context(context: Context)
val sandboxDir
    get() = File(context.filesDir, "sandbox").also { if (!it.exists()) it.mkdirs() }

context(context: Context)
val userHomeDir get() = if (currentUser == null) null else sandboxHomeDir.resolve(currentUser!!)

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

context(context: Context)
val localDir: File get() = klyxFilesDir.resolve("usr")

context(context: Context)
fun isTerminalInstalled(): Boolean {
    val rootfs = sandboxDir.listFiles()?.filter {
        it.absolutePath != sandboxDir.resolve("tmp").absolutePath
    } ?: emptyList()

    return localDir.resolve(".terminal_setup_ok_DO_NOT_REMOVE").exists() && rootfs.isNotEmpty()
}
