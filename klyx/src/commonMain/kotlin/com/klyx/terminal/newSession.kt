package com.klyx.terminal

import com.klyx.core.KlyxBuildConfig
import com.klyx.core.io.Paths
import com.klyx.core.io.androidExternalFilesDir
import com.klyx.core.io.androidNativeLibraryDir
import com.klyx.core.platform.Os
import com.klyx.core.platform.currentOs
import com.klyx.core.process.systemEnv
import com.klyx.core.unsupported
import com.klyx.core.util.join
import com.klyx.terminal.emulator.TerminalSession
import com.klyx.terminal.emulator.TerminalSessionClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.files.SystemFileSystem
import kotlin.uuid.Uuid

suspend fun newSession(
    user: String,
    client: TerminalSessionClient,
    id: Uuid = Uuid.generateV7()
): TerminalSession = withContext(Dispatchers.Default) {
    when (currentOs()) {
        Os.Android -> {
            val filesDir = Paths.dataDir.join("files")
            val tmpDir = Paths.tempDir.join("terminal/${id.toHexString()}").also(SystemFileSystem::createDirectories)
            val sandboxDir = Paths.dataDir.join("sandbox").also(SystemFileSystem::createDirectories)

            val cwd = "/home/$user"

            val env = mutableListOf(
                "PROOT_TMP_DIR=$tmpDir",
                "COLORTERM=truecolor",
                "TERM=xterm-256color",
                "USER=$user",
                "TZ=UTC",
                "WKDIR=$cwd",
                "PUBLIC_HOME=${Paths.androidExternalFilesDir}",
                "LANG=C.UTF-8",
                "DEBUG=${KlyxBuildConfig.IS_DEBUG}",
                "LOCAL=${filesDir.join("usr")}",
                "SANDBOX_DIR=$sandboxDir",
                "LD_LIBRARY_PATH=${filesDir.join("usr/lib")}",
                "PROMPT_DIRTRIM=2",
                "LINKER=/system/bin/linker64",
                "NATIVE_LIB_DIR=${Paths.androidNativeLibraryDir}",
                "PROOT=${Paths.androidNativeLibraryDir.join("libproot.so")}",
                "TMP_DIR=${Paths.tempDir}",
                "TMPDIR=${Paths.tempDir}",
                "DATADIR=${Paths.dataDir}",
                "DOTNET_GCHeapHardLimit=1C0000000",
                "PENDING_CMD=false",
                "DISPLAY=:0"
            )

            env += systemEnv().map { (k, v) -> "$k=$v" }

            val bin = filesDir.join("usr/bin")
            val sandboxSH = bin.join("sandbox")
            val setupSH = bin.join("setup")

            val argsv = arrayOf("-c", sandboxSH.toString())
            val shell = "/system/bin/sh"
            val args = if (TerminalManager.isSandboxExtractionNeeded) {
                listOf("-c", setupSH.toString(), *argsv)
            } else {
                listOf("-c", sandboxSH.toString())
            }

            TerminalSession(
                shellPath = shell,
                cwd = filesDir.toString(),
                args = args,
                env = env,
                client = client
            )
        }

        else -> unsupported("Not supported on this device")
    }
}
