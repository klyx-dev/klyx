package com.klyx.ui.component.extension

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klyx.core.Notifier
import com.klyx.core.event.EventBus
import com.klyx.core.extension.ExtensionEntry
import com.klyx.core.extension.ExtensionFetchException
import com.klyx.core.extension.ExtensionFilter
import com.klyx.core.extension.ExtensionInstallException
import com.klyx.core.extension.ExtensionsIndex
import com.klyx.core.extension.extractRepoZip
import com.klyx.core.extension.parseRepoInfo
import com.klyx.core.fetchBody
import com.klyx.core.fetchText
import com.klyx.core.file.KxFile
import com.klyx.core.file.toKxFile
import com.klyx.core.file.toOkioPath
import com.klyx.core.io.Paths
import com.klyx.core.logging.logger
import com.klyx.core.util.join
import com.klyx.core.util.string
import com.klyx.core.util.value
import com.klyx.extension.Event
import com.klyx.extension.ExtensionManifest
import com.klyx.extension.host.ExtensionStore
import com.klyx.resources.Res.string
import com.klyx.resources.extension_install_failed
import com.klyx.resources.extension_install_success
import com.willowtreeapps.fuzzywuzzy.diffutils.FuzzySearch
import io.github.z4kn4fein.semver.toVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.peanuuutz.tomlkt.Toml

data class ExtensionListState(
    // Extensions available from remote (github)
    val remoteExtensions: List<ExtensionManifest> = emptyList(),
    // Extensions currently installed on the system
    val installedExtensions: List<ExtensionManifest> = emptyList(),
    // UI state
    val isLoading: Boolean = false,
    val filter: ExtensionFilter = ExtensionFilter.All,
    val searchQuery: String = "",
)

class ExtensionViewModel(private val notifier: Notifier) : ViewModel() {
    private val _extensionListState = MutableStateFlow(ExtensionListState())
    val extensionListState = _extensionListState.asStateFlow()

    init {
        EventBus.INSTANCE.subscribe<Event.ExtensionUninstalled> { event ->
            _extensionListState.update { state ->
                state.copy(
                    installedExtensions = state.installedExtensions.filter { it.id != event.extensionId }
                )
            }
        }
    }

    fun loadExtensions(store: ExtensionStore) {
        _extensionListState.update { it.copy(isLoading = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val installed = store.installedExtensions().map { it.value.manifest }
                _extensionListState.update { it.copy(installedExtensions = installed) }

                val remote = fetchAllExtensions()
                _extensionListState.update {
                    it.copy(
                        remoteExtensions = remote,
                        installedExtensions = installed
                    )
                }
            } catch (err: Throwable) {
                logger().error(err) { "Error fetching extensions: ${err.message}" }
            }

            _extensionListState.update { it.copy(isLoading = false) }
        }
    }

    fun getExtensionInfo(extensionId: String): ExtensionManifest? {
        // Installed version always takes priority
        return _extensionListState.value.installedExtensions.firstOrNull { it.id == extensionId }
            ?: _extensionListState.value.remoteExtensions.firstOrNull { it.id == extensionId }
    }

    fun isUpdateAvailable(extensionId: String): Boolean {
        val installedVersion = _extensionListState.value.installedExtensions
            .firstOrNull { it.id == extensionId }
            ?.version
            ?.toVersion()

        val remoteVersion = _extensionListState.value.remoteExtensions
            .firstOrNull { it.id == extensionId }
            ?.version
            ?.toVersion()

        return installedVersion != null && remoteVersion != null && remoteVersion > installedVersion
    }

    fun refreshInstalledExtensions(store: ExtensionStore) {
        val installed = store.installedExtensions().map { it.value.manifest }

        _extensionListState.update { current ->
            current.copy(
                installedExtensions = installed,
                // Also update remote metadata to include updated versions
                remoteExtensions = current.remoteExtensions.map { remote ->
                    installed.firstOrNull { it.id == remote.id } ?: remote
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

    fun getFilteredExtensions(state: ExtensionListState = _extensionListState.value): List<ExtensionManifest> {
        val installedExtensions = state.installedExtensions
        val installedIds = installedExtensions.map { it.id }.toSet()

        val baseList: List<ExtensionManifest> = when (state.filter) {
            ExtensionFilter.All -> installedExtensions + state.remoteExtensions.filter { it.id !in installedIds }
            ExtensionFilter.Installed -> installedExtensions
            ExtensionFilter.NotInstalled -> state.remoteExtensions.filter { it.id !in installedIds }
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
    suspend fun installExtension(manifest: ExtensionManifest, store: ExtensionStore) {
        installExtension(manifest).onSuccess { file ->
            store.installExtension(file.toOkioPath()).await()
                .fold(
                    onSuccess = {
                        notifier.success(string.extension_install_success.value)
                        refreshInstalledExtensions(store)
                    },
                    onFailure = {
                        logger.error(it) { "Failed to install extension: ${it.message}" }
                        notifier.error(string(string.extension_install_failed, it))
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

    suspend fun updateExtension(manifest: ExtensionManifest, store: ExtensionStore) =
        installExtension(manifest, store)

    /**
     * Dev install: from directory.
     */
    suspend fun installDevFromDirectory(dir: KxFile, store: ExtensionStore) {
        store.installDevExtension(dir.toOkioPath()).await()
            .fold(
                onSuccess = {
                    notifier.success(string.extension_install_success.value)
                    refreshInstalledExtensions(store)
                },
                onFailure = {
                    logger.error(it) { "failed to install extension: ${it.message}" }
                    notifier.error(string(string.extension_install_failed, it))
                }
            )
    }

    suspend fun uninstallExtension(extensionId: String, store: ExtensionStore) {
        store.uninstallExtension(extensionId).await()
        refreshInstalledExtensions(store)
    }
}

private const val BASE_RAW_URL = "https://raw.githubusercontent.com/klyx-dev/extensions/main"
const val EXTENSIONS_INDEX_URL = "$BASE_RAW_URL/extensions.toml"

private const val BASE_GITHUB_API_EXTENSIONS_URL = "https://api.github.com/repos/klyx-dev/extensions/contents"

private val logger = logger("GithubApi")

internal suspend fun downloadRepoZip(repo: String, branch: String = "main"): ByteArray {
    val url = "https://github.com/$repo/archive/refs/heads/$branch.zip"
    return fetchBody(url)
}

internal suspend fun fetchExtensionEntries(): ExtensionsIndex {
    val raw = fetchText(EXTENSIONS_INDEX_URL)
    return Toml.decodeFromString(raw)
}

internal suspend fun fetchAllExtensions() = coroutineScope {
    val extensionsIndex = fetchExtensionEntries()

    val extensions = extensionsIndex.map { (name, entry) ->
        async(Dispatchers.IO) {
            try {
                fetchSingleExtension(name, entry)
            } catch (e: Exception) {
                logger.error { "Failed to fetch extension $name in parallel: ${e.message}" }
                null
            }
        }
    }

    extensions.awaitAll().filterNotNull()
}

private suspend fun fetchSingleExtension(name: String, entry: ExtensionEntry): ExtensionManifest {
    val submoduleMetaUrl = "$BASE_GITHUB_API_EXTENSIONS_URL/${entry.submodule}"
    val submoduleInfoJson = fetchText(submoduleMetaUrl)
    val submoduleInfo = Json.parseToJsonElement(submoduleInfoJson).jsonObject

    val submoduleHtmlUrl = submoduleInfo["html_url"]?.jsonPrimitive?.contentOrNull
        ?: throw ExtensionFetchException("Missing 'html_url' in metadata for $name")

    val submoduleSha = submoduleInfo["sha"]?.jsonPrimitive?.contentOrNull
        ?: throw ExtensionFetchException("Missing 'sha' (commit hash) in metadata for $name")

    val (owner, repo) = parseRepoInfo(submoduleHtmlUrl)

    val tomlFileUrl = "https://raw.githubusercontent.com/$owner/$repo/$submoduleSha/extension.toml"

    val tomlContentRaw = fetchText(tomlFileUrl)
    return Toml { ignoreUnknownKeys = true }.decodeFromString(tomlContentRaw)
}

suspend fun installExtension(manifest: ExtensionManifest): Result<KxFile> = withContext(Dispatchers.IO) {
    if (manifest.repository.isNullOrBlank()) {
        return@withContext Result.failure(ExtensionInstallException("Extension repository is blank"))
    }

    val (username, reponame) = parseRepoInfo(manifest.repository!!)

    val zip = try {
        downloadRepoZip(repo = "$username/$reponame")
    } catch (e: Exception) {
        return@withContext Result.failure(e)
    }
    val internalDir = Paths.tempDir.join(manifest.id).toKxFile()

    zip.extractRepoZip(internalDir)
    Result.success(internalDir)
}
