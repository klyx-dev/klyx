package com.klyx.presentation.viewmodel

import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klyx.core.unsafe.GlobalApp
import com.klyx.core.unsafe.UnsafeGlobalAccess
import com.klyx.api.data.fs.Paths
import com.klyx.data.fs.downloadFile
import com.klyx.network.fetchBody
import com.klyx.plugin.PluginLoadException
import com.klyx.plugin.PluginManager
import io.ktor.client.content.ProgressListener
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.koin.core.annotation.KoinViewModel
import java.io.File
import java.util.UUID

private const val CDN = PluginManager.CDN
private const val API = PluginManager.API

@Serializable
private data class StoreIndexEntry(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val minAppVersion: String,
    val maxAppVersion: String? = null,
    val downloadCount: Int = 0,
)

sealed interface PluginStoreEvent {
    data class ShowError(val error: String) : PluginStoreEvent
    data class ShowMessage(val message: String) : PluginStoreEvent
}

data class PluginInstallState(
    val plugin: StorePlugin,
    val progress: Float = 0f,
    val message: String? = null,
)

data class PluginStoreUiState(
    val storePlugins: ImmutableList<StorePlugin> = persistentListOf(),
    val storeLoading: Boolean = false,
    val installState: PluginInstallState? = null,
)

@KoinViewModel
class PluginStoreViewModel : ViewModel() {

    @OptIn(UnsafeGlobalAccess::class)
    private val pluginManager: PluginManager
        get() = GlobalApp.global<PluginManager>()

    private val _uiState = MutableStateFlow(PluginStoreUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<PluginStoreEvent>()
    val events = _events.receiveAsFlow()

    init {
        fetchPlugins()
    }

    fun refresh() {
        fetchPlugins()
    }

    private fun fetchPlugins() {
        viewModelScope.launch {
            _uiState.update { it.copy(storeLoading = true) }
            try {
                val result = withContext(Dispatchers.IO) {
                    val entries = fetchBody<List<StoreIndexEntry>>("$CDN/index.json")
                    entries.map { it.toStorePlugin() }.toImmutableList()
                }
                _uiState.update { it.copy(storePlugins = result) }
            } catch (e: Exception) {
                Log.e("PluginStoreViewModel", "Failed to fetch plugins", e)
                _events.send(PluginStoreEvent.ShowError("Failed to fetch plugins: ${e.localizedMessage}"))
            } finally {
                _uiState.update { it.copy(storeLoading = false) }
            }
        }
    }

    fun installPlugin(plugin: StorePlugin, onCompletion: () -> Unit) {
        if (_uiState.value.installState != null) return
        _uiState.update { it.copy(installState = PluginInstallState(plugin)) }
        viewModelScope.launch {
            try {
                val bundleFile = withContext(Dispatchers.IO) {
                    downloadBundle(
                        url = plugin.downloadUrl,
                        onDownload = { bytesSentTotal, contentLength ->
                            val progress = bytesSentTotal.toFloat() / (contentLength?.toFloat() ?: 1f)
                            _uiState.update { state ->
                                state.copy(
                                    installState = state.installState?.copy(
                                        progress = progress,
                                        message = "Downloading ${plugin.name} (${progress * 100}%)"
                                    )
                                )
                            }
                        }
                    )
                }
                pluginManager.loadPluginBundle(bundleFile.toUri()) { step ->
                    _uiState.update { state ->
                        state.copy(
                            installState = state.installState?.copy(
                                progress = 0f,
                                message = step.message
                            )
                        )
                    }
                }
                refresh()
                _events.send(PluginStoreEvent.ShowMessage("${plugin.name} installed successfully"))
            } catch (e: PluginLoadException) {
                Log.e("PluginStoreViewModel", "Failed to install ${plugin.name}", e)
                _events.send(PluginStoreEvent.ShowError(e.message ?: "Failed to install ${plugin.name}"))
            } catch (e: Exception) {
                Log.e("PluginStoreViewModel", "Failed to install ${plugin.name}", e)
                _events.send(PluginStoreEvent.ShowError("Failed to install ${plugin.name}: ${e.localizedMessage}"))
            } finally {
                _uiState.update { it.copy(installState = null) }
            }
        }.invokeOnCompletion { onCompletion() }
    }

    private suspend fun downloadBundle(url: String, onDownload: ProgressListener): File {
        val dir = File(Paths.tempDir, "klyx-store-downloads")
        dir.mkdirs()
        val file = File(dir, "${UUID.randomUUID()}.klyx")
        downloadFile(
            url = url,
            outputPath = file.absolutePath,
            onDownload = onDownload
        )
        return file
    }
}

data class StorePlugin(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val version: String,
    val minAppVersion: String,
    val maxAppVersion: String?,
    val downloadCount: Int,
    val iconUrl: String,
    val downloadUrl: String,
) {
    val baseUrl get() = "$CDN/$id"
}

private fun StoreIndexEntry.toStorePlugin(): StorePlugin {
    return StorePlugin(
        id = id,
        name = name,
        description = description,
        author = author,
        version = version,
        minAppVersion = minAppVersion,
        maxAppVersion = maxAppVersion,
        downloadCount = downloadCount,
        iconUrl = "$CDN/$id/icon.png",
        downloadUrl = "$API/dl/$id/$version",
    )
}
