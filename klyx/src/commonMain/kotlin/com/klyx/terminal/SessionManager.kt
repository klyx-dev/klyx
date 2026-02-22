package com.klyx.terminal

import com.klyx.core.event.EventBus
import com.klyx.terminal.emulator.TerminalSession
import com.klyx.terminal.emulator.TerminalSessionClient
import com.klyx.terminal.event.NewSessionEvent
import com.klyx.terminal.event.SessionTerminateEvent
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.uuid.Uuid

object SessionManager {
    private val lock = Mutex()
    val sessions = mutableMapOf<Uuid, TerminalSession>()
    var currentSessionId = Uuid.generateV7()

    suspend fun getOrCreateSession(id: Uuid, user: String, client: TerminalSessionClient): TerminalSession {
        return sessions[id]?.let { session ->
            if (session.isRunning.value) session else null
        } ?: newSession(user, client, id)
    }

    suspend fun newSession(
        user: String,
        client: TerminalSessionClient,
        id: Uuid = Uuid.generateV7()
    ): TerminalSession {
        return com.klyx.terminal.newSession(user, client, id).also {
            EventBus.INSTANCE.post(NewSessionEvent(id, it))
            lock.withLock { sessions[id] = it }
        }
    }

    suspend fun terminate(id: Uuid) {
        val sessionToTerminate = lock.withLock { sessions.remove(id) }
        sessionToTerminate?.finishIfRunning()
        EventBus.INSTANCE.post(SessionTerminateEvent(id))
    }

    fun terminateAll() {
        sessions.values.forEach {
            it.finishIfRunning()
        }
    }
}
