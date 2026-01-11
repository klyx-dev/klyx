package com.klyx.extension.host

import arrow.core.raise.context.bind
import arrow.core.raise.context.ensure
import arrow.core.raise.context.raise
import arrow.core.raise.context.result
import com.klyx.core.Notifier
import com.klyx.core.app.App
import com.klyx.core.event.EventBus
import com.klyx.core.file.resolve
import com.klyx.core.file.toKxFile
import com.klyx.core.io.copyRecursively
import com.klyx.core.io.isFile
import com.klyx.core.io.okioFs
import com.klyx.core.logging.log
import com.klyx.core.logging.logerror
import com.klyx.core.noderuntime.NodeRuntime
import com.klyx.editor.language.LanguageConfig
import com.klyx.editor.language.LanguageQueries
import com.klyx.editor.language.LoadedLanguage
import com.klyx.editor.language.QUERY_FILENAME_PREFIXES
import com.klyx.extension.CompileExtensionOptions
import com.klyx.extension.Event
import com.klyx.extension.ExtensionBuilder
import com.klyx.extension.ExtensionHostProxy
import com.klyx.extension.ExtensionIndex
import com.klyx.extension.ExtensionIndexEntry
import com.klyx.extension.ExtensionIndexIconThemeEntry
import com.klyx.extension.ExtensionIndexLanguageEntry
import com.klyx.extension.ExtensionIndexThemeEntry
import com.klyx.extension.ExtensionLibraryKind
import com.klyx.extension.ExtensionManifest
import com.klyx.extension.ExtensionOperation
import com.klyx.extension.ExtensionStillInstalledException
import com.klyx.extension.native.ExtensionLoadException
import com.klyx.extension.types.SlashCommand
import com.klyx.util.Ok
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import net.peanuuutz.tomlkt.Toml
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import kotlin.time.TimeSource

@OptIn(ExperimentalSerializationApi::class)
private val json = Json {
    explicitNulls = false
    encodeDefaults = true
    namingStrategy = JsonNamingStrategy.SnakeCase
    ignoreUnknownKeys = true
}

private const val DEV_EXTENSION_MARKER_FILE_NAME = ".klyx_dev_extension"

class ExtensionStore private constructor(
    extensionsDirectory: Path,
    private val installedDirectory: Path,
    private val workdir: Path,
    private val indexPath: Path,
    private val proxy: ExtensionHostProxy,
    private val nodeRuntime: NodeRuntime,
    private val cx: App,
    private val buildDirectory: Path
) : SynchronizedObject(), AutoCloseable {

    @Volatile
    private var extensionIndex = ExtensionIndex()
    private val runningOperations = mutableMapOf<String, ExtensionOperation>()
    private val modifiedExtensions = mutableSetOf<String>()

    private val wasmHost = WasmHost(workdir.toString(), proxy, nodeRuntime)
    private val wasmExtensions = mutableListOf<Pair<ExtensionManifest, WasmExtension>>()

    @Volatile
    private var isReloadInProgress = false
    private val reloadMutex = Mutex()

    private val notifier: Notifier get() = cx.get()
    private val builder = ExtensionBuilder(buildDirectory)

    init {
        // The extensions store maintains an index file, which contains a complete
        // list of the installed extensions and the resources that they provide.
        // This index is loaded synchronously on startup.
        val (indexContent, indexMetadata, extensionsMetadata) = runBlocking {
            withContext(Dispatchers.IO) {
                Triple(
                    runCatching { okioFs.source(indexPath).buffer() }.getOrNull(),
                    okioFs.metadataOrNull(extensionsDirectory),
                    okioFs.metadataOrNull(installedDirectory)
                )
            }
        }

        // Normally, there is no need to rebuild the index. But if the index file
        // is invalid or is out-of-date according to the filesystem mtimes, then
        // it must be asynchronously rebuilt.
        var extensionIndex = ExtensionIndex()
        var extensionIndexNeedsRebuild = true

        indexContent?.let { source ->
            runCatching {
                json.decodeFromString(ExtensionIndex.serializer(), source.use { it.readUtf8() })
            }.onSuccess {
                log.debug { "loaded extension index" }
                extensionIndex = it
                val indexMtime = indexMetadata?.lastModifiedAtMillis ?: Long.MAX_VALUE
                val extensionsMtime = extensionsMetadata?.lastModifiedAtMillis ?: Long.MAX_VALUE
                if (indexMtime > extensionsMtime) {
                    extensionIndexNeedsRebuild = false
                }
            }
        }

        // Immediately load all of the extensions in the initial manifest. If the
        // index needs to be rebuild, then enqueue
        val loadInitialExtensions = extensionsUpdated(extensionIndex)

        if (extensionIndexNeedsRebuild) {
            cx.backgroundScope.launch {
                reload(null)
            }
        }

        cx.backgroundScope.launch {
            log.debug { "waiting for initial extension load" }
            loadInitialExtensions.await()
            log.debug { "initial extension load complete" }
        }
    }

    fun extensionManifestForId(extensionId: String) = extensionIndex.extensions[extensionId]?.manifest

    fun devExtensions() = extensionIndex.extensions.values.mapNotNull { if (it.dev) it.manifest else null }

    fun isDevExtension(extensionId: String) = extensionIndex.extensions[extensionId]?.dev ?: false

    fun installedExtensions(): Map<String, ExtensionIndexEntry> = extensionIndex.extensions

    /**
     * Returns the names of themes provided by extensions.
     */
    fun extensionThemes(extensionId: String) = extensionIndex.themes.mapNotNull { (name, theme) ->
        if (theme.extension == extensionId) name else null
    }

    /**
     * Returns the path to the theme file within an extension, if there is an
     * extension that provides the theme.
     */
    fun pathToExtensionTheme(themeName: String): Path? {
        val entry = extensionIndex.themes[themeName] ?: return null
        return installedDirectory / entry.extension / entry.path
    }

    /**
     * Returns the names of icon themes provided by extensions.
     */
    fun extensionIconThemes(extensionId: String) = extensionIndex.iconThemes.mapNotNull { (name, iconTheme) ->
        if (iconTheme.extension == extensionId) name else null
    }

    /**
     * Returns the path to the icon theme file within an extension, if there is
     * an extension that provides the icon theme.
     */
    fun pathToExtensionIconTheme(iconThemeName: String): Pair<Path, Path>? {
        val entry = extensionIndex.iconThemes[iconThemeName] ?: return null
        val iconThemePath = installedDirectory / entry.extension / entry.path
        val iconRootPath = installedDirectory / entry.extension
        return iconThemePath to iconRootPath
    }

    suspend fun reload(modifiedExtension: String?) {
        reloadMutex.withLock {
            if (isReloadInProgress) return
            isReloadInProgress = true
        }

        try {
            EventBus.INSTANCE.tryPost(Event.StartedReloading)
            modifiedExtension?.let { modifiedExtensions.add(it) }

            val index = rebuildExtensionIndex().await()
            extensionsUpdated(index).await()
        } finally {
            reloadMutex.withLock {
                isReloadInProgress = false
            }
        }
    }

    fun rebuildExtensionIndex(): Deferred<ExtensionIndex> {
        val workdir = wasmHost.workDir().toPath()
        return cx.backgroundSpawn {
            val startTime = TimeSource.Monotonic.markNow()
            val index = ExtensionIndex()
            log.debug { "rebuilding extension index" }

            okioFs.createDirectories(workdir)
            okioFs.createDirectories(installedDirectory)

            runCatching {
                for (extensionPath in okioFs.list(installedDirectory)) {
                    if (extensionPath.name == ".DS_Store") continue

                    addExtensionToIndex(extensionPath, index, proxy).getOrThrow()
                }
            }.logerror()

            runCatching {
                json.encodeToString(index)
            }.onSuccess { indexJson ->
                okioFs.sink(indexPath).buffer().use { it.writeUtf8(indexJson) }
            }.logerror()

            log.info { "rebuilt extension index in ${startTime.elapsedNow()}" }
            index
        }
    }

    fun uninstallExtension(extensionId: String): Deferred<Result<Unit>> {
        val extensionDir = installedDirectory.resolve(extensionId)
        val workdir = wasmHost.workDir().toPath().resolve(extensionId)
        if (runningOperations.containsKey(extensionId)) return CompletableDeferred(Ok(Unit))
        runningOperations[extensionId] = ExtensionOperation.Remove

        return cx.backgroundSpawn {
            okioFs.deleteRecursively(extensionDir, mustExist = false)
            reload(null)

            // There's a race between wasm extension fully stopping and the directory removal.
            // On Windows, it's impossible to remove a directory that has a process running in it.
            for (i in 0..<3) {
                delay(i * 300L)
                result { okioFs.deleteRecursively(workdir, mustExist = false) }
                    .onSuccess { break }
                    .onFailure {
                        if (i == 2) {
                            log.error(it) { "Failed to remove extension work dir $workdir" }
                        }
                    }
            }

            EventBus.INSTANCE.post(Event.ExtensionUninstalled(extensionId))
            runningOperations.remove(extensionId)
            Ok(Unit)
        }
    }

    fun installDevExtension(extensionSourcePath: Path) = installExtension(extensionSourcePath, true)
    fun installExtension(extensionSourcePath: Path) = installExtension(extensionSourcePath, false)

    private fun installExtension(extensionSourcePath: Path, dev: Boolean) = cx.backgroundSpawn {
        result {
            val manifest = ExtensionManifest.load(extensionSourcePath).bind()
            val extensionId = manifest.id
            log.debug { "installing extension $extensionId" }

            extensionIndex.extensions[extensionId]?.let { indexEntry ->
                if (!indexEntry.dev) {
                    uninstallExtension(extensionId).await().logerror()
                }
            }

            if (runningOperations.containsKey(extensionId)) {
                return@result
            } else {
                runningOperations[extensionId] = ExtensionOperation.Install
            }

            val outputPath = installedDirectory.resolve(extensionId)
            okioFs.createDirectories(outputPath.parent!!)
            okioFs.metadataOrNull(outputPath)?.let { metadata ->
                ensure(metadata.isDirectory) { ExtensionStillInstalledException(extensionId) }
                okioFs.deleteRecursively(outputPath)
            }

            try {
                okioFs.atomicMove(extensionSourcePath, outputPath)
            } catch (_: Throwable) {
                okioFs.copyRecursively(extensionSourcePath, outputPath)
            }

            if (dev) {
                log.debug { "building dev extension..." }
                okioFs.sink(outputPath.resolve(DEV_EXTENSION_MARKER_FILE_NAME)).buffer().use {
                    it.writeUtf8("DO NOT DELETE THIS FILE")
                }

                cx.backgroundSpawn {
                    try {
                        val now = TimeSource.Monotonic.markNow()
                        log.debug { "compiling dev extension" }
                        builder.compileExtension(outputPath, manifest, CompileExtensionOptions(release = false))
                        log.debug { "compiled dev extension in ${now.elapsedNow()}" }
                    } catch (e: Throwable) {
                        launch { uninstallExtension(extensionId).await() }
                        if (e.message?.contains("rustc: command not found") == true) {
                            error("Rust is not installed. Please install Rust and try again.")
                        }
                        raise(e)
                    }
                }.await()
                log.debug { "done building dev extension" }
            } else {
                val wasmFiles = listOf("src", "lib")
                    .flatMap { subDir ->
                        outputPath.toKxFile().resolve(subDir)
                            .listFiles { it.extension == "wasm" }
                            ?.toList().orEmpty()
                    }
                if (wasmFiles.isEmpty()) {
                    ensure(okioFs.exists(outputPath.resolve("extension.wasm"))) {
                        RuntimeException("extension.wasm not found")
                    }
                }
            }

            log.debug { "reloading extension after install" }
            reload(null)
            log.debug { "reloaded extension" }
            EventBus.INSTANCE.post(Event.ExtensionInstalled(extensionId))
            runningOperations.remove(extensionId)
        }
    }

    /**
     * Updates the set of installed extensions.
     *
     * First, this unloads any themes, languages, or grammars that are
     * no longer in the manifest, or whose files have changed on disk.
     * Then it loads any themes, languages, or grammars that are newly
     * added to the manifest, or whose files have changed on disk.
     */
    fun extensionsUpdated(newIndex: ExtensionIndex): Deferred<Unit> {
        val oldIndex = extensionIndex

        // Determine which extensions need to be loaded and unloaded, based
        // on the changes to the manifest and the extensions that we know have been
        // modified.
        val extensionsToUnload = mutableListOf<String>()
        val extensionsToLoad = mutableListOf<String>()

        val oldMap = oldIndex.extensions
        val newMap = newIndex.extensions

        val oldKeys = oldMap.keys
        val newKeys = newMap.keys

        // Removed
        for (key in oldKeys - newKeys) {
            extensionsToUnload.add(key)
        }

        // Added
        for (key in newKeys - oldKeys) {
            extensionsToLoad.add(key)
        }

        // Present in both
        for (key in oldKeys intersect newKeys) {
            if (oldMap[key] != newMap[key] || modifiedExtensions.contains(key)) {
                extensionsToUnload.add(key)
                extensionsToLoad.add(key)
            }
        }

        modifiedExtensions.clear()

        if (extensionsToLoad.isEmpty() && extensionsToUnload.isEmpty()) {
            return CompletableDeferred(Unit)
        }

        val reloadCount = extensionsToUnload.count { extensionsToLoad.contains(it) }
        log.info { "extensions updated. loading ${extensionsToLoad.size - reloadCount}, reloading $reloadCount, unloading ${extensionsToUnload.size - reloadCount}" }

        val extensionIds = extensionsToLoad.mapNotNull { id ->
            newIndex.extensions[id]?.manifest?.version?.let { id to it }
        }
        log.debug { "extensionIds: $extensionIds" }

        val themesToRemove = oldIndex.themes.mapNotNull { (name, entry) ->
            if (extensionsToUnload.contains(entry.extension)) name else null
        }
        val iconThemesToRemove = oldIndex.iconThemes.mapNotNull { (name, entry) ->
            if (extensionsToUnload.contains(entry.extension)) name else null
        }
        val languagesToRemove = oldIndex.languages.mapNotNull { (name, entry) ->
            if (extensionsToUnload.contains(entry.extension)) name else null
        }

        val grammarsToRemove = mutableListOf<String>()
        val serverRemovalTasks = mutableListOf<Result<Job>>()
        for (extensionId in extensionsToUnload) {
            val extension = oldIndex.extensions[extensionId] ?: continue
            grammarsToRemove.addAll(extension.manifest.grammars.keys)

            for ((languageServerName, config) in extension.manifest.languageServers) {
                for (language in config.languages()) {
                    serverRemovalTasks.add(proxy.removeLanguageServer(language, languageServerName, cx))
                }
            }

            for (serverId in extension.manifest.contextServers.keys) {
                proxy.unregisterContextServer(serverId, cx)
            }
            for (commandName in extension.manifest.slashCommands.keys) {
                proxy.unregisterSlashCommand(commandName)
            }
        }

        wasmExtensions.removeAll { (extension, _) -> extensionsToUnload.contains(extension.id) }
        proxy.removeUserThemes(themesToRemove)
        proxy.removeIconThemes(iconThemesToRemove)
        proxy.removeLanguages(languagesToRemove, grammarsToRemove)

        val grammarsToAdd = mutableMapOf<String, Path>()
        val themesToAdd = mutableListOf<Path>()
        val iconThemesToAdd = mutableMapOf<Path, Path>()
        val snippetsToAdd = mutableListOf<Path>()
        for (extensionId in extensionsToLoad) {
            val extension = newIndex.extensions[extensionId] ?: continue

            grammarsToAdd.putAll(extension.manifest.grammars.keys.map { grammarName ->
                val grammarPath = installedDirectory / extensionId / "grammars" / "$grammarName.wasm"
                grammarName to grammarPath
            })

            themesToAdd.addAll(extension.manifest.themes.map { themePath ->
                installedDirectory / extensionId / themePath
            })

            iconThemesToAdd.putAll(extension.manifest.iconThemes.map { iconThemePath ->
                val path = installedDirectory / extensionId / iconThemePath
                val iconRootPath = installedDirectory / extensionId
                path to iconRootPath
            })

            extension.manifest.snippets?.let { path ->
                snippetsToAdd.add(installedDirectory / extensionId / path)
            }
        }

        proxy.registerGrammars(grammarsToAdd.toList())
        val languagesToAdd = newIndex.languages
            .filter { (_, entry) -> extensionsToLoad.contains(entry.extension) }

        for ((languageName, language) in languagesToAdd) {
            val languagePath = installedDirectory / language.extension / language.path
            proxy.registerLanguage(languageName, language.grammar, language.matcher, language.hidden) {
                result {
                    val configToml = okioFs.source(languagePath.resolve("config.toml")).buffer().use { it.readUtf8() }
                    val config: LanguageConfig = Toml {
                        explicitNulls = false
                        ignoreUnknownKeys = true
                    }.decodeFromString(configToml)
                    val queries = loadPluginQueries(languagePath)
                    LoadedLanguage(config, queries, null, null)
                }
            }
        }

        val rootDir = installedDirectory
        val extensionEntries = extensionsToLoad.mapNotNull { newIndex.extensions[it] }
        this.extensionIndex = newIndex
        EventBus.INSTANCE.tryPost(Event.ExtensionsUpdated)

        return cx.spawn(Dispatchers.IO) { cx ->
            cx.backgroundSpawn {
                serverRemovalTasks.mapNotNull { it.getOrNull() }.joinAll()
                for (themePath in themesToAdd) {
                    proxy.loadUserTheme(themePath).await().logerror()
                }
                for ((iconThemePath, iconsRootPath) in iconThemesToAdd) {
                    proxy.loadIconTheme(iconThemePath, iconsRootPath).await().logerror()
                }
                for (snippetPath in snippetsToAdd) {
                    result {
                        okioFs.source(snippetPath).buffer().use { it.readUtf8() }
                    }.onSuccess { snippetsContents ->
                        proxy.registerSnippet(snippetPath, snippetsContents).logerror()
                    }.onFailure {
                        log.error(it) { "cannot load snippets: ${it.message}" }
                    }
                }
            }.await()

            val wasmExtensions = mutableMapOf<ExtensionManifest, WasmExtension>()
            for (extension in extensionEntries) {
                if (extension.manifest.lib.kind == null) continue

                val extensionPath = rootDir.resolve(extension.manifest.id)
                result {
                    log.debug { "WASM load start: ${extension.manifest.id}" }
                    val now = TimeSource.Monotonic.markNow()
                    val wasm = WasmExtension.load(extensionPath, extension.manifest, wasmHost)
                    log.debug { "WASM load end: ${extension.manifest.id}, completed in ${now.elapsedNow()}" }
                    wasm
                }.onSuccess {
                    wasmExtensions[extension.manifest] = it
                }.onFailure {
                    log.error(it) { "failed to load extension: ${extension.manifest.id}, ${it.message}" }

                    if (it is ExtensionLoadException.InternalException) {
                        notifier.error(
                            title = "failed to load extension: ${extension.manifest.id}",
                            message = it.details,
                            canUserDismiss = true,
                            durationMillis = 5000,
                        )

                        notifier.toast("removing extension ${extension.manifest.id}")
                        uninstallExtension(extension.manifest.id).await()
                        notifier.toast("removed extension ${extension.manifest.id}")
                    }
                }
            }

            for ((manifest, extension) in wasmExtensions) {
                for ((languageServerId, config) in manifest.languageServers) {
                    for (language in config.languages()) {
                        proxy.registerLanguageServer(extension, languageServerId, language)
                    }
                }

                for ((slashCommandName, slashCommand) in manifest.slashCommands) {
                    proxy.registerSlashCommand(
                        extension = extension,
                        command = SlashCommand(
                            name = slashCommandName,
                            description = slashCommand.description,
                            tooltipText = "",
                            requiresArgument = slashCommand.requiresArgument
                        )
                    )
                }

                for (id in manifest.contextServers.keys) {
                    proxy.registerContextServer(extension, id, cx)
                }
            }

            this@ExtensionStore.wasmExtensions.addAll(wasmExtensions.toList())
            proxy.setExtensionsLoaded()
            proxy.reloadCurrentTheme(cx)
            proxy.reloadCurrentIconTheme(cx)
        }
    }

    override fun close() {
        wasmHost.close()
    }

    companion object {
        internal fun create(
            installedDirectory: Path,
            extensionsDirectory: Path,
            workdir: Path,
            indexPath: Path,
            proxy: ExtensionHostProxy,
            nodeRuntime: NodeRuntime,
            buildDirectory: Path,
            cx: App
        ): ExtensionStore {
            return ExtensionStore(
                installedDirectory = installedDirectory,
                extensionsDirectory = extensionsDirectory,
                workdir = workdir,
                indexPath = indexPath,
                proxy = proxy,
                nodeRuntime = nodeRuntime,
                cx = cx,
                buildDirectory = buildDirectory
            )
        }

        fun global(cx: App) = cx.global<GlobalExtensionStore>().store
        fun globalOrNull(cx: App) = cx.globalOrNull<GlobalExtensionStore>()?.store
    }
}

fun ExtensionStore(
    extensionsDirectory: Path,
    proxy: ExtensionHostProxy,
    nodeRuntime: NodeRuntime,
    buildDirectory: Path?,
    cx: App
): ExtensionStore {
    val workdir = extensionsDirectory / "work"
    val buildDirectory = buildDirectory ?: (extensionsDirectory / "build")
    val installedDirectory = extensionsDirectory / "installed"
    val indexPath = extensionsDirectory / "index.json"

    log.debug { "initializing extension store" }
    val store = ExtensionStore.create(
        installedDirectory = installedDirectory,
        extensionsDirectory = extensionsDirectory,
        workdir = workdir,
        indexPath = indexPath,
        proxy = proxy,
        nodeRuntime = nodeRuntime,
        buildDirectory = buildDirectory,
        cx = cx
    )

    return store
}

private fun loadPluginQueries(rootPath: Path): LanguageQueries {
    val result = LanguageQueries()
    okioFs.listOrNull(rootPath)?.let { entries ->
        for (path in entries) {
            val remainder = path.toString().removePrefix(rootPath.toString())
            if (!remainder.endsWith(".scm")) continue

            for ((name, query) in QUERY_FILENAME_PREFIXES) {
                if (remainder.startsWith(name)) {
                    runCatching {
                        okioFs.source(path).buffer().use { it.readUtf8() }
                    }.onSuccess { contents ->
                        query(result).let { r ->
                            when (name) {
                                "highlights" -> result.highlights = (r ?: "") + contents
                                "brackets" -> result.brackets = (r ?: "") + contents
                                "outline" -> result.outline = (r ?: "") + contents
                                "indents" -> result.indents = (r ?: "") + contents
                                "embedding" -> result.embedding = (r ?: "") + contents
                                "injections" -> result.injections = (r ?: "") + contents
                                "overrides" -> result.overrides = (r ?: "") + contents
                                "redactions" -> result.redactions = (r ?: "") + contents
                                "runnables" -> result.runnables = (r ?: "") + contents
                                "debugger" -> result.debugger = (r ?: "") + contents
                                "textobjects" -> result.textObjects = (r ?: "") + contents
                                "imports" -> result.imports = (r ?: "") + contents
                            }
                        }
                    }.onFailure {
                        log.error(it) { "error loading query $name" }
                    }
                    break
                }
            }
        }
    }
    return result
}

private suspend fun addExtensionToIndex(extensionDir: Path, index: ExtensionIndex, proxy: ExtensionHostProxy) = result {
    val manifest = ExtensionManifest.load(extensionDir).getOrThrow()
    val extensionId = manifest.id

    // TODO: distinguish dev extensions more explicitly, by the absence
    // of a checksum file that we'll create when downloading normal extensions.
    val isDev = okioFs.exists(extensionDir.resolve(DEV_EXTENSION_MARKER_FILE_NAME))

    okioFs.listOrNull(extensionDir.resolve("languages"))?.forEach { languagePath ->
        val relativePath = languagePath.relativeTo(extensionDir)
        val metadata = okioFs.metadataOrNull(languagePath) ?: return@forEach
        if (!metadata.isDirectory) return@forEach

        val config: LanguageConfig = okioFs.source(languagePath.resolve("config.toml"))
            .buffer().use { Toml { ignoreUnknownKeys = true }.decodeFromString(it.readUtf8()) }
        if (!manifest.languages.contains(relativePath)) {
            manifest.languages.add(relativePath)
        }

        index.languages[config.name] = ExtensionIndexLanguageEntry(
            extension = extensionId,
            path = relativePath,
            matcher = config.matcher,
            grammar = config.grammar,
            hidden = config.hidden
        )
    }

    okioFs.listOrNull(extensionDir.resolve("themes"))?.forEach { themePath ->
        val relativePath = themePath.relativeTo(extensionDir)
        val themeFamilies = proxy.listThemeNames(themePath).await().logerror().getOrNull() ?: return@forEach

        if (!manifest.themes.contains(relativePath)) {
            manifest.themes.add(relativePath)
        }

        for (themeName in themeFamilies) {
            index.themes[themeName] = ExtensionIndexThemeEntry(extensionId, relativePath)
        }
    }

    okioFs.listOrNull(extensionDir.resolve("icon_themes"))?.forEach { iconThemePath ->
        val relativePath = iconThemePath.relativeTo(extensionDir)
        val iconThemeFamilies = proxy.listIconThemeNames(iconThemePath).await().logerror().getOrNull() ?: return@forEach

        if (!manifest.iconThemes.contains(relativePath)) {
            manifest.iconThemes.add(relativePath)
        }

        for (iconThemeName in iconThemeFamilies) {
            index.iconThemes[iconThemeName] = ExtensionIndexIconThemeEntry(extensionId, relativePath)
        }
    }

    val wasmFiles = listOf("src", "lib")
        .flatMap { subDir ->
            extensionDir.toKxFile().resolve(subDir)
                .listFiles { it.extension == "wasm" }
                ?.toList().orEmpty()
        }

    if (wasmFiles.any { it.isFile } || okioFs.isFile(extensionDir.resolve("extension.wasm"))) {
        if (manifest.lib.kind == null) {
            manifest.lib.kind = ExtensionLibraryKind.Rust
        }
    }

    index.extensions[extensionId] = ExtensionIndexEntry(manifest, isDev)
}
