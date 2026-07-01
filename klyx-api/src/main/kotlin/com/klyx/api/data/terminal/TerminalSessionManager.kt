package com.klyx.api.data.terminal

import androidx.compose.runtime.Immutable
import com.klyx.terminal.emulator.TerminalEmulator
import com.klyx.terminal.emulator.TerminalSession
import com.klyx.terminal.emulator.TerminalSessionClient
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow
import kotlin.uuid.Uuid

/**
 * Represents an active terminal session entry in the manager.
 *
 * @property id The unique identifier for this session.
 * @property session The underlying [TerminalSession] instance.
 */
@Immutable
data class TerminalSessionEntry(
    val id: Uuid,
    val session: TerminalSession
)

/**
 * Service for managing multiple terminal sessions.
 *
 * This manager handles the creation, termination, and state tracking of all
 * terminal sessions currently active in the application.
 */
interface TerminalSessionManager {

    /**
     * A [StateFlow] emitting the list of all currently active sessions.
     */
    val sessions: StateFlow<ImmutableList<TerminalSessionEntry>>

    /**
     * A [StateFlow] emitting the ID of the currently active/visible session.
     */
    val currentSessionId: StateFlow<Uuid?>

    /**
     * A [StateFlow] emitting the currently active session entry, or null if no session is active.
     */
    val currentSession: StateFlow<TerminalSessionEntry?>

    /**
     * Creates and starts a new terminal session.
     *
     * @param client The client implementation that will handle session output and events.
     * @param id The unique ID for the new session. Defaults to a new random UUID.
     * @param transcriptRows The number of rows to keep in the session's scrollback buffer.
     * @param showMotd Whether to display the "Message of the Day" upon session start.
     * @return The newly created [TerminalSession].
     */
    suspend fun newSession(
        client: TerminalSessionClient,
        id: Uuid = Uuid.generateV7(),
        transcriptRows: Int = TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS,
        showMotd: Boolean = true,
    ): TerminalSession

    /**
     * Returns an existing session with the given [id] if it is still running,
     * otherwise creates and starts a new one.
     */
    suspend fun getOrCreateSession(
        id: Uuid,
        client: TerminalSessionClient,
        transcriptRows: Int = TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS,
    ): TerminalSession

    /**
     * Returns the currently active session, or creates a new one if none exists.
     */
    suspend fun currentSessionOrNewSession(
        client: TerminalSessionClient,
        transcriptRows: Int = TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS,
    ): TerminalSession

    /**
     * Switches the active session to the one with the specified [id].
     */
    fun switchTo(id: Uuid)

    /**
     * Terminates and removes the session with the specified [id].
     */
    suspend fun terminate(id: Uuid)

    /**
     * Terminates and removes the currently active session.
     */
    suspend fun terminateCurrentSession()

    /**
     * Terminates and removes all active terminal sessions.
     */
    suspend fun terminateAll()
}
