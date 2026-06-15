package com.klyx.data.terminal

import android.util.Log
import com.klyx.BuildConfig
import com.klyx.data.fs.Paths
import com.klyx.event.GlobalEventBus
import com.klyx.event.terminal.NewSessionEvent
import com.klyx.event.terminal.SessionTerminateEvent
import com.klyx.event.terminal.TerminateAllSessionEvent
import com.klyx.platform.currentArchitecture
import com.klyx.terminal.bin
import com.klyx.terminal.emulator.TerminalSession
import com.klyx.terminal.emulator.TerminalSessionClient
import com.klyx.terminal.home
import com.klyx.terminal.prefix
import com.klyx.terminal.rootFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.uuid.Uuid

object SessionManager {

    private val lock = Mutex()
    val sessions = mutableMapOf<Uuid, TerminalSession>()
    var currentSessionId: Uuid? = null

    suspend fun getOrCreateSession(
        id: Uuid,
        client: TerminalSessionClient
    ): TerminalSession {
        return lock.withLock { sessions[id] }?.let { session ->
            if (session.isRunning.value) session else null
        } ?: newSession(client, id)
    }

    suspend fun currentSessionOrNewSession(
        client: TerminalSessionClient
    ): TerminalSession {
        return getOrCreateSession(currentSessionId ?: Uuid.generateV7(), client)
    }

    suspend fun newSession(
        client: TerminalSessionClient,
        id: Uuid = Uuid.generateV7()
    ) = withContext(Dispatchers.IO) {
        val prefix = Paths.prefix
        val home = Paths.home.absolutePath
        val bash = prefix.resolve("bin/bash").absolutePath
        val linker = "/system/bin/linker64"

        val env = mutableListOf(
            "HOME=$home",
            "PREFIX=${prefix.absolutePath}",
            "TERMUX__ROOTFS=${Paths.rootFs.absolutePath}",
            "TERMUX__PREFIX=${prefix.absolutePath}",
            "TERMUX__HOME=$home",
            "TERMUX_APP__DATA_DIR=${Paths.dataDir.absolutePath}",
            "TERMUX_APP__LEGACY_DATA_DIR=${Paths.dataDir.absolutePath}",
            "TERMUX_APP__PACKAGE_NAME=com.klyx",
            "TMPDIR=${prefix.resolve("tmp").absolutePath}",
            "TERM=xterm-256color",
            "LANG=en_US.UTF-8",
            "COLORTERM=truecolor",
            "SHELL=$bash",
            "TERMUX_PACKAGE_MANAGER=apt",
            "TERMUX_PACKAGE_ARCH=${currentArchitecture()}",
            "TERMUX__SE_PROCESS_CONTEXT=${getSeLinuxContext()}",
            "TERMUX_EXEC__PROC_SELF_EXE=$bash",
            "TERMUX_EXEC__SYSTEM_LINKER_EXEC__MODE=force",
            "LD_PRELOAD=${prefix.resolve("lib/libtermux-exec-linker-ld-preload.so").absolutePath}",
            "PATH=${prefix.absolutePath}/bin:${System.getenv("PATH").orEmpty()}",
            "TERM_PROGRAM=klyx",
            "KLYX_DEBUG=${BuildConfig.DEBUG}",
            "TERM_PROGRAM_VERSION=${BuildConfig.VERSION_NAME}"
        )

        val certPath = prefix.resolve("etc/tls/cert.pem")
        if (certPath.exists()) {
            env += "SSL_CERT_FILE=${certPath.absolutePath}"
            env += "CURL_CA_BUNDLE=${certPath.absolutePath}"
        }

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
            System.getenv(key)?.let { "$key=$it" }
        }

        TerminalSession(
            shellPath = linker,
            cwd = home,
            args = listOf(linker, bash),
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

    private fun findExecutable(): File {
        var executable: File? = null

        val shellFile = Paths.bin.resolve("bash")
        if (shellFile.isFile) {
            if (!shellFile.canExecute()) {
                if (!shellFile.setExecutable(true)) {
                    Log.e("SessionManager", "Cannot set executable: ${shellFile.absolutePath}")
                }
            }
            executable = shellFile
        } else {
            Log.e("SessionManager", "bin/bash not found")
        }

        return executable ?: File("/system/bin/sh")
    }

    private fun getSeLinuxContext() = try {
        val process = Runtime.getRuntime().exec(arrayOf("/system/bin/cat", "/proc/self/attr/current"))
        process.inputStream.bufferedReader().readLine()?.trim() ?: ""
    } catch (_: Exception) {
        ""
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
