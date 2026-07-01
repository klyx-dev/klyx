package com.klyx.presentation.viewmodel

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klyx.data.preferences.SettingsRepository
import com.klyx.terminal.InstallProgressListener
import com.klyx.terminal.TerminalInstaller
import com.klyx.api.util.humanBytes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@Immutable
data class TerminalUiState(
    val isChecking: Boolean = true,
    val isInstalled: Boolean = false,
    val isInstalling: Boolean = false,
    val currentStep: String = "Initializing...",
    val progress: Float = 0f,
    val progressText: String = "",
    val error: String? = null
)

@KoinViewModel
class TerminalViewModel(
    private val settingsRepository: SettingsRepository,
    private val terminalInstaller: TerminalInstaller
) : ViewModel() {

    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()

    init {
        checkInstallationState()
    }

    fun checkInstallationState() {
        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true, error = null) }
            try {
                if (terminalInstaller.isInstalled()) {
                    _uiState.update { it.copy(isChecking = false, isInstalled = true) }
                } else {
                    _uiState.update { it.copy(isChecking = false, isInstalled = false) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isChecking = false, error = "Failed to verify environment: ${e.message}")
                }
            }
        }
    }

    fun startInstallation() {
        if (_uiState.value.isInstalling) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(isInstalling = true, error = null, progress = 0f, progressText = "")
            }

            try {
                terminalInstaller.installLatest(object : InstallProgressListener {
                    private var currentPhase = ""

                    override fun step(label: String) {
                        currentPhase = label
                        _uiState.update { it.copy(currentStep = label) }
                    }

                    override fun progress(done: Long, total: Long) {
                        val percent = if (total > 0) done.toFloat() / total.toFloat() else 0f

                        val text = if (currentPhase.contains("Downloading", ignoreCase = true)) {
                            "${done.humanBytes()} / ${total.humanBytes()}"
                        } else {
                            "${done.humanBytes()} / ${total.humanBytes()} (${(percent * 100f).toInt()}%)"
                        }

                        _uiState.update { it.copy(progress = percent, progressText = text) }
                    }

                    override fun warn(message: String) {
                        Log.w("Terminal", message)
                    }
                })

                _uiState.update { it.copy(isInstalling = false, isInstalled = true) }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isInstalling = false, error = e.localizedMessage ?: "Installation failed")
                }
            }
        }
    }
}
