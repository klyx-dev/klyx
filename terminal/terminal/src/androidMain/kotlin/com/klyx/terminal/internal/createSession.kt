package com.klyx.terminal.internal

import android.content.Context
import android.system.Os
import com.klyx.core.Process
import com.klyx.core.generateId
import com.klyx.terminal.klyxBinDir
import com.klyx.terminal.klyxCacheDir
import com.klyx.terminal.klyxFilesDir
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import java.io.File

typealias TerminalSessionId = String

val linker = if (Process.is64Bit()) "/system/bin/linker64" else "/system/bin/linker"

context(context: Context)
fun createSession(
    user: String,
    client: TerminalSessionClient,
    cwd: File = klyxFilesDir,
    sessionId: TerminalSessionId = generateId()
) = run {
    val tmpDir = File(klyxCacheDir, "terminal/$sessionId").apply {
        if (exists()) deleteRecursively()
        mkdirs()
    }

    val env = mutableMapOf(
        "PROOT_TMP_DIR" to tmpDir.absolutePath,
        "COLORTERM" to "truecolor",
        "TERM" to "xterm-256color",
        "USER" to user,
        "CACHE_GID" to (10000 + Os.getgid()).toString(),
        "EXT_GID" to (40000 + Os.getgid()).toString()
    )

    TerminalSession(
        klyxBinDir.absolutePath + "/proot",
        cwd.absolutePath,
        buildProotArgs(user),
        env.map { "${it.key}=${it.value}" }.toTypedArray(),
        TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS,
        client
    )
}
