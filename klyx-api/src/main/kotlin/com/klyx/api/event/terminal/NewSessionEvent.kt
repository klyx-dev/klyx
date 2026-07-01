package com.klyx.api.event.terminal

import com.klyx.terminal.emulator.TerminalSession
import kotlin.uuid.Uuid

/**
 * Event published when a new terminal session is created and started.
 *
 * @property id The unique identifier of the new session.
 * @property session The underlying [TerminalSession] instance.
 */
data class NewSessionEvent(val id: Uuid, val session: TerminalSession)
