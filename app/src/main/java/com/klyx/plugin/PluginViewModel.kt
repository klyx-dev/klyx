package com.klyx.plugin

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klyx.api.plugin.PluginDescriptor
import com.klyx.api.plugin.PluginInfo
import com.klyx.core.koin
import com.klyx.core.unsafe.GlobalApp
import com.klyx.core.unsafe.UnsafeGlobalAccess
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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
)

sealed interface PluginUiEvent {
    data class ShowError(val error: String) : PluginUiEvent
    data class ShowMessage(val message: String) : PluginUiEvent
}

@KoinViewModel
class PluginViewModel : ViewModel() {

    @OptIn(UnsafeGlobalAccess::class)
    private val pluginManager: PluginManager
        get() = GlobalApp.global<PluginManager>()

    private val _uiState = MutableStateFlow(PluginUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<PluginUiEvent>()
    val events = _events.receiveAsFlow()

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
                //_events.send(PluginUiEvent.ShowMessage("Plugin loaded successfully"))
            } catch (e: PluginLoadException) {
                Log.e("PluginViewModel", "Failed to load plugin bundle", e)
                _events.send(PluginUiEvent.ShowError(e.message ?: "Failed to load plugin bundle"))
            } catch (e: Exception) {
                Log.e("PluginViewModel", "Unknown error loading plugin bundle", e)
                _events.send(PluginUiEvent.ShowError("Unknown error loading plugin bundle: ${e.localizedMessage}"))
            } finally {
                _uiState.update { it.copy(loadingState = null) }
            }
        }
    }

    fun unloadPlugin(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(loadingState = PluginLoadingState()) }
            try {
                pluginManager.unloadPlugin(id)
                _uiState.update { it.copy(plugins = pluginManager.loadedPlugins.toImmutableList()) }
            } catch (e: Exception) {
                _events.send(PluginUiEvent.ShowError(e.message ?: "Error unloading plugin"))
            } finally {
                _uiState.update { it.copy(loadingState = null) }
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(plugins = pluginManager.loadedPlugins.toImmutableList()) }
    }
}
