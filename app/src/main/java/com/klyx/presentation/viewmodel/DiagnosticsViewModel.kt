package com.klyx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klyx.data.diagnostics.DisplayCapabilities
import com.klyx.data.diagnostics.EditorInfo
import com.klyx.data.diagnostics.RuntimeCapabilities
import com.klyx.data.diagnostics.StorageCapabilities
import com.klyx.data.repository.DiagnosticsRepository
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel

data class DiagnosticsState(
    val deviceInfo: ImmutableMap<String, String> = persistentMapOf(),
    val displayCapabilities: DisplayCapabilities? = null,
    val editorInfo: EditorInfo? = null,
    val runtimeCapabilities: RuntimeCapabilities? = null,
    val storageCapabilities: StorageCapabilities? = null,
    val isLoading: Boolean = true
)

@KoinViewModel
class DiagnosticsViewModel(
    private val repository: DiagnosticsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DiagnosticsState())
    val state: StateFlow<DiagnosticsState> = _state

    init {
        loadDiagnostics()
    }

    private fun loadDiagnostics() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                withContext(Dispatchers.IO) {
                    coroutineScope {
                        val deviceInfoDeferred = async { repository.getDeviceInfo() }
                        val displayCapabilitiesDeferred = async { repository.getDisplayCapabilities() }
                        val editorInfoDeferred = async { repository.getEditorInfo() }
                        val runtimeCapabilitiesDeferred = async { repository.getRuntimeCapabilities() }
                        val storageCapabilitiesDeferred = async { repository.getStorageCapabilities() }

                        _state.update {
                            it.copy(
                                deviceInfo = deviceInfoDeferred.await(),
                                displayCapabilities = displayCapabilitiesDeferred.await(),
                                editorInfo = editorInfoDeferred.await(),
                                runtimeCapabilities = runtimeCapabilitiesDeferred.await(),
                                storageCapabilities = storageCapabilitiesDeferred.await(),
                                isLoading = false
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}
