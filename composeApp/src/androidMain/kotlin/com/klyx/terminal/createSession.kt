package com.klyx.terminal

import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.klyx.BuildConfig
import com.klyx.activities.TerminalActivity
import com.klyx.core.generateId
import com.klyx.core.process.linker
import com.klyx.core.terminal.SetupNextStage
import com.klyx.core.terminal.klyxBinDir
import com.klyx.core.terminal.klyxCacheDir
import com.klyx.core.terminal.klyxFilesDir
import com.klyx.core.terminal.klyxLibDir
import com.klyx.core.terminal.localDir
import com.klyx.core.terminal.sandboxDir
import com.klyx.core.terminal.userHomeDir
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import java.io.File

typealias TerminalSessionId = String

var terminalSetupNextStage by mutableStateOf(SetupNextStage.None)

fun createSession(
    activity: TerminalActivity,
    user: String,
    client: TerminalSessionClient,
    sessionId: TerminalSessionId = generateId()
) = with(activity) {
    val tmpDir = File(klyxCacheDir, "terminal/$sessionId").apply {
        if (exists()) deleteRecursively()
        mkdirs()
    }

    val pendingCommand = if (intent.hasExtra("command")) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("command", TerminalCommand::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("command") as? TerminalCommand
        }
    } else {
        null
    }

    println("Pending: $pendingCommand")

    val workingDir = pendingCommand?.cwd
        ?: intent.getStringExtra("cwd")
        ?: userHomeDir?.relativeToOrNull(sandboxDir)?.absolutePath
        ?: "/"

    val env = mutableListOf(
        "PROOT_TMP_DIR=${tmpDir.absolutePath}",
        "COLORTERM=truecolor",
        "TERM=xterm-256color",
        "USER=$user",
        "TZ=UTC",
        "PROOT_TMP_DIR=${tmpDir.absolutePath}",
        "WKDIR=$workingDir",
        "PUBLIC_HOME=${getExternalFilesDir(null)?.absolutePath}",
        "COLORTERM=truecolor",
        "TERM=xterm-256color",
        "LANG=C.UTF-8",
        "DEBUG=${BuildConfig.DEBUG}",
        "LOCAL=${localDir.absolutePath}",
        "SANDBOX_DIR=${sandboxDir.absolutePath}",
        "PRIVATE_DIR=${filesDir.parentFile!!.absolutePath}",
        "LD_LIBRARY_PATH=${klyxLibDir.absolutePath}",
        "PROMPT_DIRTRIM=2",
        "LINKER=$linker",
        "NATIVE_LIB_DIR=${applicationInfo.nativeLibraryDir}",
        "TZ=UTC",
        "TMP_DIR=${cacheDir.absolutePath}",
        "TMPDIR=${cacheDir.absolutePath}",
        "DOTNET_GCHeapHardLimit=1C0000000",
        "SOURCE_DIR=${applicationInfo.sourceDir}",
        "PENDING_CMD=${if (pendingCommand == null) "false" else "true"}",
        "DISPLAY=:0"
    )

    env += System.getenv().map { "${it.key}=${it.value}" }

    pendingCommand?.env?.let { env.addAll(it) }

    val sandboxSH = klyxBinDir.resolve("sandbox")
    val setupSH = klyxBinDir.resolve("setup")

    val args: Array<String>
    val shell: String

    if (pendingCommand == null) {
        args = arrayOf(sandboxSH.absolutePath)
        shell = "/system/bin/sh"
    } else if (!pendingCommand.sandbox) {
        args = pendingCommand.args
        shell = pendingCommand.cmd
    } else {
        args = arrayOf(sandboxSH.absolutePath, pendingCommand.cmd, *pendingCommand.args)
        shell = "/system/bin/sh"
    }

    val actualArgs: Array<String>
    val actualShell: String
    if (terminalSetupNextStage == SetupNextStage.Extraction) {
        actualShell = "/system/bin/sh"
        actualArgs = arrayOf("-c", setupSH.absolutePath, *args)
    } else {
        actualShell = shell
        actualArgs = arrayOf("-c", *args)
    }

    TerminalSession(
        actualShell,
        klyxFilesDir.absolutePath,
        actualArgs,
        env.toTypedArray(),
        TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS,
        client
    )
}
