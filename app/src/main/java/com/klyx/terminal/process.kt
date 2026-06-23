package com.klyx.terminal

import android.annotation.SuppressLint
import com.klyx.BuildConfig
import com.klyx.data.fs.Paths

fun terminalEnv() = mapOf(
    "TERM" to "xterm-256color",
    "TERM_PROGRAM" to "klyx",
    "TERM_PROGRAM_VERSION" to BuildConfig.VERSION_NAME,
    "COLORTERM" to "truecolor",
    "HOSTNAME" to "klyx",
    "TMPDIR" to Paths.tempDir.absolutePath,
    "LANG" to "C.UTF-8",
    "LC_ALL" to "C.UTF-8",
) + processEnv()

fun processEnv(): Map<String, String> {

    val tmpDir = Paths.tempDir.resolve("term-process").also { if (!it.exists()) it.mkdirs() }

    val env = mutableMapOf(
        "PROOT_LOADER" to prootLoaderFile().absolutePath,
        "PROOT_TMP_DIR" to tmpDir.absolutePath,
        "DEBUG" to "${BuildConfig.DEBUG}",
        "HOME" to "/root",
        "ROOTFS" to Paths.rootFs.absolutePath,
        "PATH" to "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
    )

    env += listOf(
        "ANDROID_ART_ROOT",
        "ANDROID_ASSETS",
        "ANDROID_DATA",
        "ANDROID_I18N_ROOT",
        "ANDROID_ROOT",
        "ANDROID_RUNTIME_ROOT",
        "ANDROID_STORAGE",
        "ANDROID_TZDATA_ROOT",
        "ASEC_MOUNTPOINT",
        "BOOTCLASSPATH",
        "DEX2OATBOOTCLASSPATH",
        "EXTERNAL_STORAGE",
        "LOOP_MOUNTPOINT",
        "SYSTEMSERVERCLASSPATH",
    ).mapNotNull { key ->
        System.getenv(key)?.let { value -> key to value }
    }

    return env
}

@SuppressLint("SdCardPath")
fun terminalArgs() = listOf(
    prootFile().absolutePath,

    "-0",
    "--kill-on-exit",
    "--link2symlink",
    "--sysvipc",
    "-L",

    "-r", Paths.rootFs.absolutePath,

    "-w", "/root",

    "-b", "/dev",
    "-b", "/proc",
    "-b", "/sys",

    "-b", "/sdcard",
    "-b", "/storage",
    "-b", Paths.dataDir.canonicalPath,
    "-b", "${Paths.home.absolutePath}:/root",

    "/bin/sh", "-c",
    "cat /etc/motd; /bin/bash --login"
)
