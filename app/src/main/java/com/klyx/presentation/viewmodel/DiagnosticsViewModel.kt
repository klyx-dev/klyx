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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    val state: StateFlow<DiagnosticsState>
        field = MutableStateFlow(DiagnosticsState())

    init {
        loadDiagnostics()
    }

    private fun loadDiagnostics() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val deviceInfo = repository.getDeviceInfo()
                val displayCapabilities = repository.getDisplayCapabilities()
                val editorInfo = repository.getEditorInfo()
                val runtimeCapabilities = repository.getRuntimeCapabilities()
                val storageCapabilities = repository.getStorageCapabilities()

                state.value = DiagnosticsState(
                    deviceInfo = deviceInfo,
                    displayCapabilities = displayCapabilities,
                    editorInfo = editorInfo,
                    runtimeCapabilities = runtimeCapabilities,
                    storageCapabilities = storageCapabilities,
                    isLoading = false
                )
            }
        }
    }
}
