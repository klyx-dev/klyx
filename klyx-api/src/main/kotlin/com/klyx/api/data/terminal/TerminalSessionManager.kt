package com.klyx.api.data.terminal

import androidx.compose.runtime.Immutable
import com.klyx.core.Global
import com.klyx.terminal.emulator.TerminalEmulator
import com.klyx.terminal.emulator.TerminalSession
import com.klyx.terminal.emulator.TerminalSessionClient
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow
import kotlin.uuid.Uuid

@Immutable
data class TerminalSessionEntry(
    val id: Uuid,
    val session: TerminalSession
)

interface TerminalSessionManager : Global {
    val sessions: StateFlow<ImmutableList<TerminalSessionEntry>>
    val currentSessionId: StateFlow<Uuid?>
    val currentSession: StateFlow<TerminalSessionEntry?>

    suspend fun newSession(
        client: TerminalSessionClient,
        id: Uuid = Uuid.generateV7(),
        transcriptRows: Int = TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS,
        showMotd: Boolean = true,
    ): TerminalSession

    suspend fun getOrCreateSession(
        id: Uuid,
        client: TerminalSessionClient,
        transcriptRows: Int = TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS,
    ): TerminalSession

    suspend fun currentSessionOrNewSession(
        client: TerminalSessionClient,
        transcriptRows: Int = TerminalEmulator.DEFAULT_TERMINAL_TRANSCRIPT_ROWS,
    ): TerminalSession

    fun switchTo(id: Uuid)
    suspend fun terminate(id: Uuid)
    suspend fun terminateCurrentSession()
    suspend fun terminateAll()
}
