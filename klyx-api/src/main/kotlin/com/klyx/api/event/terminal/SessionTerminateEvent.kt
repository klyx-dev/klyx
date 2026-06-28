package com.klyx.api.event.terminal

import kotlin.uuid.Uuid

data class SessionTerminateEvent(val id: Uuid)

data object TerminateAllSessionEvent
