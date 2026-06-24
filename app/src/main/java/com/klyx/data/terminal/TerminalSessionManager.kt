package com.klyx.data.terminal

import androidx.compose.runtime.Immutable
import com.klyx.BuildConfig
import com.klyx.core.Global
import com.klyx.data.fs.Paths
import com.klyx.event.GlobalEventBus
import com.klyx.event.terminal.NewSessionEvent
import com.klyx.event.terminal.SessionTerminateEvent
import com.klyx.event.terminal.TerminateAllSessionEvent
import com.klyx.terminal.emulator.TerminalEmulator
import com.klyx.terminal.emulator.TerminalSession
import com.klyx.terminal.emulator.TerminalSessionClient
import com.klyx.terminal.home
import com.klyx.terminal.terminalArgs
import com.klyx.terminal.rootFs
import com.klyx.terminal.terminalEnv
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

/**
 * Single terminal session entry tracked by a [TerminalSessionManager].
 */
@Immutable
data class TerminalSessionEntry(
    val id: Uuid,
    val session: TerminalSession
)

/**
 * Responsible for managing the lifecycle of terminal sessions and
 * exposing them reactively to the UI.
 *
 * Implementations decide how sessions are spawned (e.g. shell process, mock,
 * remote) and how they are tracked. The default Android implementation is
 * [DefaultTerminalSessionManager].
 */
interface TerminalSessionManager : Global {
    /** All currently active sessions, in creation order. */
    val sessions: StateFlow<ImmutableList<TerminalSessionEntry>>

    /** Id of the currently selected session, or `null` if no session exists. */
    val currentSessionId: StateFlow<Uuid?>

    /** Currently selected session, or `null` if no session exists. */
    val currentSession: StateFlow<TerminalSessionEntry?>

    /** Creates a brand-new session and marks it as current. */
    suspend fun newSession(
        client: TerminalSessionClient,
        id: Uuid = Uuid.generateV7(),
        transcriptRows: Int = TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS,
        showMotd: Boolean = true,
    ): TerminalSession

    /** Returns the session for [id] if it is still running, otherwise creates a new one. */
    suspend fun getOrCreateSession(
        id: Uuid,
        client: TerminalSessionClient,
        transcriptRows: Int = TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS,
    ): TerminalSession

    /** Returns the [currentSession] if available, otherwise creates a new one. */
    suspend fun currentSessionOrNewSession(
        client: TerminalSessionClient,
        transcriptRows: Int = TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS,
    ): TerminalSession

    /** Selects an existing session as current. No-op if [id] is unknown. */
    fun switchTo(id: Uuid)

    /** Terminates a single session by [id]. */
    suspend fun terminate(id: Uuid)

    /** Terminates the [currentSession] and falls back to the next available one. */
    suspend fun terminateCurrentSession()

    /** Terminates all sessions. */
    suspend fun terminateAll()
}

/**
 * Default [TerminalSessionManager] implementation that spawns sessions backed
 * by the bundled shell binary and publishes lifecycle events to the
 * [GlobalEventBus].
 */
class DefaultTerminalSessionManager : TerminalSessionManager {

    private val lock = Mutex()

    private val _sessions = MutableStateFlow<PersistentList<TerminalSessionEntry>>(persistentListOf())
    override val sessions: StateFlow<ImmutableList<TerminalSessionEntry>> = _sessions.asStateFlow()

    private val _currentSessionId = MutableStateFlow<Uuid?>(null)
    override val currentSessionId: StateFlow<Uuid?> = _currentSessionId.asStateFlow()

    private val _currentSession = MutableStateFlow<TerminalSessionEntry?>(null)
    override val currentSession: StateFlow<TerminalSessionEntry?> = _currentSession.asStateFlow()

    private fun refreshCurrent() {
        val id = _currentSessionId.value
        _currentSession.value = _sessions.value.firstOrNull { it.id == id }
    }

    override suspend fun getOrCreateSession(
        id: Uuid,
        client: TerminalSessionClient,
        transcriptRows: Int
    ): TerminalSession {
        val existing = lock.withLock { _sessions.value.firstOrNull { it.id == id }?.session }
        if (existing != null && existing.isRunning.value) return existing
        return newSession(client, id, transcriptRows)
    }

    override suspend fun currentSessionOrNewSession(
        client: TerminalSessionClient,
        transcriptRows: Int
    ): TerminalSession {
        val id = _currentSessionId.value ?: Uuid.generateV7()
        return getOrCreateSession(id, client, transcriptRows)
    }

    override fun switchTo(id: Uuid) {
        if (_sessions.value.any { it.id == id }) {
            _currentSessionId.value = id
            refreshCurrent()
        }
    }

    override suspend fun newSession(
        client: TerminalSessionClient,
        id: Uuid,
        transcriptRows: Int,
        showMotd: Boolean
    ): TerminalSession = withContext(Dispatchers.IO) {
        val linker = "/system/bin/linker64"

        val env = terminalEnv().map { "${it.key}=${it.value}" }.toMutableList()
        val certPath = Paths.rootFs.resolve("etc/tls/cert.pem")
        if (certPath.exists()) {
            env += "SSL_CERT_FILE=${certPath.absolutePath}"
            env += "CURL_CA_BUNDLE=${certPath.absolutePath}"
        }

        if (BuildConfig.DEBUG) {
            env += "KLYX_DEBUG=true"
            env += "DEBUG=true"
        }

        TerminalSession(
            shellPath = linker,
            cwd = Paths.home.absolutePath,
            args = listOf(linker) + terminalArgs(showMotd),
            env = env,
            client = client,
            transcriptRows = transcriptRows
        ).also { session ->
            GlobalEventBus.publish(NewSessionEvent(id, session))
            lock.withLock {
                _sessions.update { it + TerminalSessionEntry(id, session) }
                _currentSessionId.value = id
                refreshCurrent()
            }
        }
    }

    override suspend fun terminate(id: Uuid) {
        val removed = lock.withLock {
            val entry = _sessions.value.firstOrNull { it.id == id } ?: return@withLock null
            _sessions.update { list -> list.mutate { mutable -> mutable.removeAll { it.id == id } } }
            if (_currentSessionId.value == id) {
                _currentSessionId.value = _sessions.value.firstOrNull()?.id
            }
            refreshCurrent()
            entry
        }
        removed?.session?.finishIfRunning()
        GlobalEventBus.publish(SessionTerminateEvent(id))
    }

    override suspend fun terminateCurrentSession() {
        val id = _currentSessionId.value ?: return
        terminate(id)
    }

    override suspend fun terminateAll() {
        val snapshot = lock.withLock {
            val list = _sessions.value
            _sessions.value = persistentListOf()
            _currentSessionId.value = null
            refreshCurrent()
            list
        }
        snapshot.forEach { it.session.finishIfRunning() }
        GlobalEventBus.publish(TerminateAllSessionEvent)
    }

    private fun getSeLinuxContext() = try {
        val process = Runtime.getRuntime().exec(arrayOf("/system/bin/cat", "/proc/self/attr/current"))
        process.inputStream.bufferedReader().readLine()?.trim() ?: ""
    } catch (_: Exception) {
        ""
    }

    private companion object {
        private const val TAG = "TerminalSessionManager"
    }
}
