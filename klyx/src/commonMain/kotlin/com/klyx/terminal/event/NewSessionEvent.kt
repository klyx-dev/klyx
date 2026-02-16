package com.klyx.terminal.event

import com.klyx.terminal.emulator.TerminalSession
import kotlin.uuid.Uuid

data class NewSessionEvent(val id: Uuid, val session: TerminalSession)
