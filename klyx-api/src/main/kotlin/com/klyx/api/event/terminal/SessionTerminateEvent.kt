package com.klyx.api.event.terminal

import kotlin.uuid.Uuid

/**
 * Event published when a terminal session is terminated.
 *
 * @property id The unique identifier of the terminated session.
 */
data class SessionTerminateEvent(val id: Uuid)

/**
 * Event published when all active terminal sessions are terminated at once.
 */
data object TerminateAllSessionEvent
