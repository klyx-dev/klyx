package com.klyx.presentation.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klyx.data.preferences.SettingsRepository
import com.klyx.data.terminal.FileDownloadState
import com.klyx.data.terminal.TerminalManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@Immutable
data class TerminalUiState(
    val needsDownload: Boolean = false,
    val isExtractingSandbox: Boolean = false,
    val files: List<FileDownloadState> = emptyList(),
    val error: String? = null
)

@KoinViewModel
class TerminalViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<TerminalUiState>
        field = MutableStateFlow(TerminalUiState())

    init {
        viewModelScope.launch {
            TerminalManager.environmentState.collect { envState ->
                uiState.update { currentState ->
                    currentState.copy(
                        needsDownload = envState.needsDownload,
                        isExtractingSandbox = envState.isSandboxExtractionNeeded,
                        files = envState.files,
                        error = envState.error?.message
                    )
                }
            }
        }

        viewModelScope.launch {
            TerminalManager.init()
        }
    }

    fun startDownloads() {
        viewModelScope.launch { TerminalManager.downloadRequiredFiles() }
    }

    fun refreshEnvironmentState() {
        viewModelScope.launch {
            TerminalManager.init()
        }
    }

    fun setUsername(name: String) {
        viewModelScope.launch {
            settingsRepository.updateTerminalSettings { it.copy(currentUser = name) }
        }
    }
}
