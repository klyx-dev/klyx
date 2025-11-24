package com.klyx.core.terminal.internal

import android.annotation.SuppressLint
import android.content.Context
import com.klyx.core.terminal.klyxFilesDir
import com.klyx.core.terminal.sandboxDir
import java.io.File

@SuppressLint("SdCardPath")
context(ctx: Context)
fun buildProotArgs(
    user: String? = null,
    loginUser: Boolean = true,
    commands: List<String> = emptyList()
): List<String> {

    val targetUser = user ?: "root"
    val args = mutableListOf<String>()

    args += listOf(
        "--kill-on-exit",
        "-0",
        "--sysvipc",
        "--link2symlink",
        "-r", sandboxDir.absolutePath,
        "-w", "/"
    )

    val binds = listOf(
        "/dev", "/proc", "/sys",
        "/system", "/vendor", "/product",
        klyxFilesDir.absolutePath
    )

    for (p in binds) {
        if (File(p).exists()) args += listOf("-b", p)
    }

    if (commands.isEmpty()) {
        args += if (loginUser) {
            listOf("su", "-", targetUser)
        } else {
            listOf("/bin/bash")
        }
        return args
    }

    if (loginUser) {
        val cmd = commands.joinToString(" ")
        args += listOf("su", "-", targetUser, "-c", "bash -lc \"$cmd\"")
    } else {
        args += listOf("/bin/bash", "-lc", commands.joinToString(" "))
    }

    return args
}
