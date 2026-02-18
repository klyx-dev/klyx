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

    suspend fun newSession(
        user: String,
        client: TerminalSessionClient
    ): TerminalSession {
        val id = Uuid.generateV7()

        return newSession(user, client, id).also {
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
