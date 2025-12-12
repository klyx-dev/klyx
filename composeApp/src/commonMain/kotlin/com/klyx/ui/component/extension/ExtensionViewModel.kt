package com.klyx.ui.component.extension

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klyx.core.Notifier
import com.klyx.core.extension.ExtensionFilter
import com.klyx.core.extension.ExtensionId
import com.klyx.core.extension.ExtensionInfo
import com.klyx.core.extension.fetchAllExtensions
import com.klyx.core.file.KxFile
import com.klyx.core.logging.logger
import com.klyx.core.util.string
import com.klyx.core.util.value
import com.klyx.extension.ExtensionManager
import com.klyx.res.Res.string
import com.klyx.res.extension_install_failed
import com.klyx.res.extension_install_success
import com.willowtreeapps.fuzzywuzzy.diffutils.FuzzySearch
import io.github.z4kn4fein.semver.toVersion
import io.itsvks.anyhow.fold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExtensionListState(
    // Extensions available from remote (github)
    val extensionInfos: List<ExtensionInfo> = emptyList(),
    // Extensions currently installed on the system
    val installedExtensions: List<ExtensionInfo> = emptyList(),
    // UI state
    val isLoading: Boolean = false,
    val filter: ExtensionFilter = ExtensionFilter.All,
    val searchQuery: String = "",
)

class ExtensionViewModel(private val notifier: Notifier) : ViewModel() {
    private val _extensionListState = MutableStateFlow(ExtensionListState())
    val extensionListState = _extensionListState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            loadExtensions()
        }
    }

    fun loadExtensions() {
        _extensionListState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                ExtensionManager.loadExtensions()
                val installed = ExtensionManager.installedExtensions.map { it.info }
                _extensionListState.update { it.copy(installedExtensions = installed) }

                val remote = fetchAllExtensions()
                _extensionListState.update {
                    it.copy(
                        extensionInfos = remote,
                        installedExtensions = installed
                    )
                }
            } catch (err: Throwable) {
                logger().error(err) { "Error fetching extensions: ${err.message}" }
            }

            _extensionListState.update { it.copy(isLoading = false) }
        }
    }

    fun getExtensionInfo(id: ExtensionId): ExtensionInfo? {
        // Installed version always takes priority
        return _extensionListState.value.installedExtensions.firstOrNull { it.id == id }
            ?: _extensionListState.value.extensionInfos.firstOrNull { it.id == id }
    }

    fun isUpdateAvailable(extensionId: ExtensionId): Boolean {
        val installedVersion = _extensionListState.value.installedExtensions
            .firstOrNull { it.id == extensionId }
            ?.version
            ?.toVersion()

        val remoteVersion = _extensionListState.value.extensionInfos
            .firstOrNull { it.id == extensionId }
            ?.version
            ?.toVersion()

        return installedVersion != null && remoteVersion != null && remoteVersion > installedVersion
    }

    fun refreshInstalledExtensions() {
        val installed = ExtensionManager.installedExtensions.map { it.info }

        _extensionListState.update { current ->
            current.copy(
                installedExtensions = installed,
                // Also update remote metadata to include updated versions
                extensionInfos = current.extensionInfos.map { remoteInfo ->
                    installed.firstOrNull { it.id == remoteInfo.id } ?: remoteInfo
                }
            )
        }
    }

    fun onFilterChanged(filter: ExtensionFilter) {
        _extensionListState.update { it.copy(filter = filter) }
    }

    fun onSearchQueryChanged(query: String) {
        _extensionListState.update { it.copy(searchQuery = query) }
    }

    fun getFilteredExtensions(state: ExtensionListState = _extensionListState.value): List<ExtensionInfo> {
        val installedExtensions = state.installedExtensions
        val installedIds = installedExtensions.map { it.id }.toSet()

        val baseList: List<ExtensionInfo> = when (state.filter) {
            ExtensionFilter.All -> installedExtensions + state.extensionInfos.filter { it.id !in installedIds }
            ExtensionFilter.Installed -> installedExtensions
            ExtensionFilter.NotInstalled -> state.extensionInfos.filter { it.id !in installedIds }
        }

        return baseList
            .map { ext ->
                ext to FuzzySearch.partialRatio(
                    state.searchQuery.lowercase(),
                    ext.name.lowercase()
                )
            }
            .filter { (_, score) ->
                state.searchQuery.isBlank() || score >= 60
            }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    /**
     * Normal extension installation (remote).
     */
    suspend fun installExtension(info: ExtensionInfo) {
        com.klyx.core.extension.installExtension(info).onSuccess { file ->
            ExtensionManager.installExtension(
                directory = file,
                isDevExtension = false
            ).fold(
                err = {
                    notifier.error(string(string.extension_install_failed, it))
                },
                ok = {
                    notifier.success(string.extension_install_success.value)
                    refreshInstalledExtensions()
                }
            )
        }.onFailure {
            notifier.error(
                string(
                    string.extension_install_failed,
                    it.message ?: it.stackTraceToString()
                )
            )
        }
    }

    suspend inline fun updateExtension(info: ExtensionInfo) = installExtension(info)

    /**
     * Dev install: from directory.
     */
    fun installDevFromDirectory(dir: KxFile) {
        viewModelScope.launch {
            ExtensionManager.installExtension(
                directory = dir,
                isDevExtension = true
            ).fold(
                err = {
                    notifier.error(string(string.extension_install_failed, it))
                },
                ok = {
                    notifier.success(string.extension_install_success.value)
                    refreshInstalledExtensions()
                }
            )
        }
    }

    /**
     * Dev install: from zip.
     */
    fun installDevFromZip(zip: KxFile) {
        viewModelScope.launch {
            ExtensionManager.installExtensionFromZip(
                zipFile = zip,
                isDevExtension = true
            ).fold(
                err = {
                    notifier.error(string(string.extension_install_failed, it))
                },
                ok = {
                    notifier.success(string.extension_install_success.value)
                    refreshInstalledExtensions()
                }
            )
        }
    }

    fun uninstallExtension(info: ExtensionInfo) {
        ExtensionManager.uninstallExtension(info)
        refreshInstalledExtensions()
    }
}
