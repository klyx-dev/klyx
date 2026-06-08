package com.klyx.event.terminal

import com.klyx.terminal.emulator.TerminalSession
import kotlin.uuid.Uuid

data class NewSessionEvent(val id: Uuid, val session: TerminalSession)
