package com.klyx.core.terminal.internal

import android.annotation.SuppressLint
import android.content.Context
import com.klyx.core.io.Paths
import com.klyx.core.io.androidNativeLibraryDir
import com.klyx.core.io.filesDir
import com.klyx.core.io.root
import com.klyx.core.process.KxProcess
import com.klyx.core.process.KxProcessBuilder
import com.klyx.core.process.process
import com.klyx.core.util.join
import com.klyx.core.withAndroidContext
import java.io.File
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@SuppressLint("SdCardPath")
context(ctx: Context)
fun buildProotArgs(
    user: String? = null,
    loginUser: Boolean = true,
    commands: List<String> = emptyList()
): List<String> {

    val klyxFilesDir = Paths.filesDir
    val sandboxDir = Paths.root

    val targetUser = user ?: "root"
    val args = mutableListOf<String>()

    args += listOf(
        "--kill-on-exit",
        "-0",
        "--sysvipc",
        "--link2symlink",
        "-r", sandboxDir.toString(),
        "-w", "/"
    )

    val binds = listOf(
        "/dev", "/proc", "/sys",
        "/system", "/vendor", "/product",
        klyxFilesDir.toString(),
        ctx.dataDir.absolutePath
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

/**
 * @return unspawned proot process
 */
@OptIn(ExperimentalContracts::class)
inline fun prootProcess(vararg args: String, block: KxProcessBuilder.() -> Unit = {}): KxProcess {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

    val prootBinary = Paths.androidNativeLibraryDir.join("libproot.so")
    val klyxLibDir = Paths.filesDir.join("usr/lib")

    return withAndroidContext {
        process(prootBinary.toString()) {
            environment {
                put("LD_LIBRARY_PATH", klyxLibDir.toString())
            }

            args(args.asList())
            block()
        }
    }
}
