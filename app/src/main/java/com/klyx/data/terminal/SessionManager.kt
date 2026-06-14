package com.klyx.data.terminal

import com.klyx.BuildConfig
import com.klyx.data.fs.Paths
import com.klyx.event.GlobalEventBus
import com.klyx.event.terminal.NewSessionEvent
import com.klyx.event.terminal.SessionTerminateEvent
import com.klyx.event.terminal.TerminateAllSessionEvent
import com.klyx.terminal.emulator.TerminalSession
import com.klyx.terminal.emulator.TerminalSessionClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

object SessionManager {

    private val lock = Mutex()
    val sessions = mutableMapOf<Uuid, TerminalSession>()
    var currentSessionId: Uuid? = null

    suspend fun getOrCreateSession(
        id: Uuid,
        user: String,
        client: TerminalSessionClient
    ): TerminalSession {
        return lock.withLock { sessions[id] }?.let { session ->
            if (session.isRunning.value) session else null
        } ?: newSession(user, client, id)
    }

    suspend fun currentSessionOrNewSession(
        user: String,
        client: TerminalSessionClient
    ): TerminalSession {
        return getOrCreateSession(currentSessionId ?: Uuid.generateV7(), user, client)
    }

    suspend fun newSession(
        user: String,
        client: TerminalSessionClient,
        id: Uuid = Uuid.generateV7()
    ) = withContext(Dispatchers.IO) {
        val filesDir = Paths.filesDir
        val tmpDir = Paths.tempDir.resolve("terminal/$id").also { it.mkdirs() }
        val sandboxDir = Paths.dataDir.resolve("sandbox").also { it.mkdirs() }

        val cwd = if (user == "root") "/" else "/home/$user"

        val envMap = mutableMapOf<String, String>()

        envMap["PROOT_NO_SECCOMP"] = "1"
        envMap["PROOT_FORCE_FOREIGN_BINARY"] = "0"
        envMap["LD_PRELOAD"] = ""
        envMap["PROOT_TMP_DIR"] = tmpDir.absolutePath
        envMap["COLORTERM"] = "truecolor"
        envMap["TERM"] = "xterm-256color"
        envMap["USER"] = user
        envMap["HOME"] = cwd
        envMap["TZ"] = "UTC"
        envMap["WKDIR"] = cwd
        envMap["PUBLIC_HOME"] = Paths.externalFilesDir.absolutePath
        envMap["LANG"] = "C.UTF-8"
        envMap["DEBUG"] = BuildConfig.DEBUG.toString()

        val usrDir = filesDir.resolve("usr")

        envMap["LOCAL"] = usrDir.absolutePath
        envMap["PROOT"] = Paths.nativeLibraryDir.resolve("libproot.so").absolutePath
        envMap["PROOT_LOADER"] = Paths.nativeLibraryDir.resolve("libloader.so").absolutePath

        envMap["PATH"] =
            "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/system/bin:/system/xbin"

        envMap["SANDBOX_DIR"] = sandboxDir.absolutePath
        envMap["PROMPT_DIRTRIM"] = "2"
        envMap["LINKER"] = "/system/bin/linker64"
        envMap["LINKER32"] = "/system/bin/linker"
        envMap["TMP_DIR"] = Paths.tempDir.absolutePath
        envMap["TMPDIR"] = Paths.tempDir.absolutePath
        envMap["DATADIR"] = Paths.dataDir.absolutePath
        envMap["PENDING_CMD"] = "false"
        envMap["DISPLAY"] = ":0"

        val env = envMap.map { "${it.key}=${it.value}" }

        val bin = filesDir.resolve("usr/bin")
        val sandboxSH = bin.resolve("sandbox")
        val setupSH = bin.resolve("setup")

        val argsv = arrayOf("-c", sandboxSH.toString())
        val shell = "/system/bin/sh"

        val args = if (TerminalManager.environmentState.value.isSandboxExtractionNeeded) {
            listOf("-c", setupSH.absolutePath, *argsv)
        } else {
            listOf("-c", sandboxSH.absolutePath)
        }

        withContext(Dispatchers.Main) {
            TerminalSession(
                shellPath = shell,
                cwd = filesDir.absolutePath,
                args = args,
                env = env,
                client = client
            ).also { session ->
                GlobalEventBus.publish(NewSessionEvent(id, session))
                lock.withLock {
                    sessions[id] = session
                    currentSessionId = id
                }
            }
        }
    }

    suspend fun terminate(id: Uuid) {
        val sessionToTerminate = lock.withLock { sessions.remove(id) }
        sessionToTerminate?.finishIfRunning()
        GlobalEventBus.publish(SessionTerminateEvent(id))
    }

    suspend fun terminateCurrentSession() {
        currentSessionId?.let { terminate(it) }
        lock.withLock { currentSessionId = sessions.keys.firstOrNull() }
    }

    suspend fun terminateAll() {
        val sessionsToTerminate = lock.withLock { sessions.values.toList() }
        sessionsToTerminate.forEach { it.finishIfRunning() }
        lock.withLock { sessions.clear() }
        GlobalEventBus.publish(TerminateAllSessionEvent)
    }
}
