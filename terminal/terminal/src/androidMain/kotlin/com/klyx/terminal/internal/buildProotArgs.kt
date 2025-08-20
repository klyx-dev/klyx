package com.klyx.terminal.internal

import android.annotation.SuppressLint
import android.content.Context
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.util.Log
import com.klyx.terminal.klyxBinDir
import com.klyx.terminal.klyxFilesDir
import com.klyx.terminal.ubuntuDir
import com.klyx.terminal.ubuntuHome
import java.io.File

@SuppressLint("SdCardPath", "SetWorldWritable", "SetWorldReadable")
context(context: Context)
fun buildProotArgs(
    user: String?,
    withInitScript: Boolean = true,
    loginUser: Boolean = true,
    vararg commands: String = emptyArray()
) = run {
    val home = File(ubuntuHome, user.orEmpty())

    val args = mutableListOf(
        "--kill-on-exit", "-w",
        //if (home.exists()) home.relativeToOrSelf(ubuntuDir).absolutePath else "/",
        "/"
    )

    val bind = { source: String, target: String? ->
        args += listOf(
            "-b",
            if (target != null) "$source:$target" else source
        )
    }

    val pathsToCheck = listOf(
        "/apex", "/odm", "/product", "/system_ext", "/vendor",
        "/sdcard", "/storage", "/dev", "/data", "/proc"
    )

    for (path in pathsToCheck) {
        val file = File(path)
        if (file.exists()) {
            val canAccess = try {
                Os.access(file.absolutePath, OsConstants.R_OK)
            } catch (error: ErrnoException) {
                if (error.errno == OsConstants.EACCES) {
                    Log.e("Terminal", "Cannot access $path (Permission denied)")
                }
                false
            }

            if (canAccess) {
                bind(file.canonicalPath, null)
            }
        }
    }

    bind("/dev/urandom", "/dev/random")
    bind(klyxFilesDir.absolutePath, null)
    bind("/dev", null)

    val fdPaths = listOf("0" to "stdin", "1" to "stdout", "2" to "stderr")
    fdPaths.forEach { (fd, name) ->
        val src = "/proc/self/fd/$fd"
        with(File(src)) {
            if (exists() && canRead() && canWrite()) {
                Log.i("Terminal", "$src -> /dev/$name")
                bind(canonicalPath, "/dev/$name")
            }
        }
    }

    args += listOf(
        "-r", ubuntuDir.absolutePath,
        "-0",
        "--link2symlink",
        "--sysvipc",
        "-L"
    )

    if (withInitScript) {
        args += listOf(
            "/bin/bash",
            klyxBinDir.absolutePath + "/init",
            "\"$@\""
        )
    } else if (loginUser) {
        args += listOf(
            "su", "-", user ?: "root",
            "-c", "bash -lc \"${commands.joinToString(" ")}\""
        )
        return args.toTypedArray()
    }

    args += commands
    args.toTypedArray()
}
