package com.klyx.plugin

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klyx.api.plugin.PluginDescriptor
import com.klyx.api.plugin.PluginInfo
import com.klyx.core.unsafe.GlobalApp
import com.klyx.core.unsafe.UnsafeGlobalAccess
import com.klyx.event.UiEvent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

data class PluginLoadingState(
    val desc: PluginDescriptor? = null,
    val message: String? = null,
)

data class PluginUiState(
    val plugins: ImmutableList<PluginInfo> = persistentListOf(),
    val loadingState: PluginLoadingState? = null,
    val isUnloading: Boolean = false,
) {
    val isLoading: Boolean
        get() = loadingState != null
}

@KoinViewModel
class PluginViewModel : ViewModel() {

    @OptIn(UnsafeGlobalAccess::class)
    private val pluginManager: PluginManager
        get() = GlobalApp.global<PluginManager>()

    private val _uiState = MutableStateFlow(PluginUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    init {
        loadInstalledPlugins()
    }

    private fun loadInstalledPlugins() {
        viewModelScope.launch {
            refresh()
        }
    }

    fun loadPluginBundle(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(loadingState = PluginLoadingState(desc = null)) }
            try {
                pluginManager.loadPluginBundle(uri) { step ->
                    _uiState.update { state ->
                        if (step.desc != null) {
                            state.copy(loadingState = PluginLoadingState(message = step.message, desc = step.desc))
                        } else {
                            state.copy(loadingState = state.loadingState?.copy(message = step.message))
                        }
                    }
                }
                _uiState.update { it.copy(plugins = pluginManager.loadedPlugins.toImmutableList()) }
                //_events.send(UiEvent.ShowMessage("Plugin loaded successfully"))
            } catch (e: PluginLoadException) {
                Log.e("PluginViewModel", "Failed to load plugin bundle", e)
                _events.emit(UiEvent.ShowError(e.message ?: "Failed to load plugin bundle"))
            } catch (e: Exception) {
                Log.e("PluginViewModel", "Unknown error loading plugin bundle", e)
                _events.emit(UiEvent.ShowError("Unknown error loading plugin bundle: ${e.localizedMessage}"))
            } finally {
                _uiState.update { it.copy(loadingState = null) }
            }
        }
    }

    fun unloadPlugin(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUnloading = true) }
            try {
                pluginManager.unloadPlugin(id)
                _uiState.update { it.copy(plugins = pluginManager.loadedPlugins.toImmutableList()) }
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowError(e.message ?: "Error unloading plugin"))
            } finally {
                _uiState.update { it.copy(isUnloading = false) }
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(plugins = pluginManager.loadedPlugins.toImmutableList()) }
    }
}
