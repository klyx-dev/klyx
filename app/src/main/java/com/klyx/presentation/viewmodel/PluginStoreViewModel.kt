package com.klyx.presentation.viewmodel

import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klyx.api.data.log.LogEntry
import com.klyx.api.data.fs.Paths
import com.klyx.api.service.Logger
import com.klyx.api.util.humanBytes
import com.klyx.core.unsafe.GlobalApp
import com.klyx.core.unsafe.UnsafeGlobalAccess
import com.klyx.data.fs.downloadFile
import com.klyx.event.UiEvent
import com.klyx.network.fetchBody
import com.klyx.plugin.PluginLoadException
import com.klyx.plugin.PluginManager
import io.ktor.client.content.ProgressListener
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.koin.core.annotation.KoinViewModel
import java.io.File
import java.util.Locale
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


data class PluginInstallState(
    val plugin: StorePlugin,
    val progress: Float = 0f,
    val message: String? = null,
    val logs: List<LogEntry> = emptyList()
)

data class PluginStoreUiState(
    val storePlugins: ImmutableList<StorePlugin> = persistentListOf(),
    val storeLoading: Boolean = false,
    val installState: PluginInstallState? = null,
)

@KoinViewModel
class PluginStoreViewModel(
    private val logger: Logger
) : ViewModel() {

    @OptIn(UnsafeGlobalAccess::class)
    private val pluginManager: PluginManager
        get() = GlobalApp.global<PluginManager>()

    private val _uiState = MutableStateFlow(PluginStoreUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

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
                _events.emit(UiEvent.ShowError("Failed to fetch plugins: ${e.localizedMessage}"))
            } finally {
                _uiState.update { it.copy(storeLoading = false) }
            }
        }
    }

    fun installPlugin(plugin: StorePlugin, onCompletion: () -> Unit) {
        if (_uiState.value.installState != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(installState = PluginInstallState(plugin)) }

            // Collect logs for this plugin while installing
            val logJob = launch {
                logger.entries.collect { entries ->
                    val pluginLogs = entries.filter { it.sourcePluginId == plugin.id }
                    _uiState.update { state ->
                        state.copy(
                            installState = state.installState?.copy(
                                logs = pluginLogs
                            )
                        )
                    }
                }
            }

            try {
                val bundleFile = withContext(Dispatchers.IO) {
                    downloadBundle(
                        url = plugin.downloadUrl,
                        onDownload = { bytesSentTotal, contentLength ->
                            val progress = contentLength?.let {
                                bytesSentTotal.toFloat() / it.toFloat()
                            }

                            val message = if (progress != null) {
                                "Downloading (${String.format(Locale.ROOT, "%.1f", progress * 100)}%)"
                            } else {
                                "Downloading (${bytesSentTotal.humanBytes()})"
                            }

                            _uiState.update { state ->
                                state.copy(
                                    installState = state.installState?.copy(
                                        progress = progress ?: 0f,
                                        message = message
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
                //refresh()
                _events.emit(UiEvent.ShowMessage("${plugin.name} installed successfully"))
            } catch (e: PluginLoadException) {
                Log.e("PluginStoreViewModel", "Failed to install ${plugin.name}", e)
                _events.emit(UiEvent.ShowError(e.message ?: "Failed to install ${plugin.name}"))
            } catch (e: Exception) {
                Log.e("PluginStoreViewModel", "Failed to install ${plugin.name}", e)
                _events.emit(UiEvent.ShowError("Failed to install ${plugin.name}: ${e.localizedMessage}"))
            } finally {
                logJob.cancel()
                _uiState.update { it.copy(installState = null) }
            }
            onCompletion()
        }
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
