package com.klyx.extension.nodegraph

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import com.klyx.core.app.Global
import com.klyx.core.io.Paths
import com.klyx.core.io.extensionsDir
import com.klyx.core.io.fs
import com.klyx.core.logging.KxLogger
import com.klyx.core.logging.logger
import com.klyx.nodegraph.GraphExecutionListener
import com.klyx.nodegraph.GraphState
import com.klyx.nodegraph.HeadlessGraph
import com.klyx.nodegraph.NodeRegistry
import com.klyx.nodegraph.PinType
import com.klyx.nodegraph.addNodes
import com.klyx.nodegraph.below
import com.klyx.nodegraph.connect
import com.klyx.nodegraph.execIn
import com.klyx.nodegraph.execOut
import com.klyx.nodegraph.extension.createSetNode
import com.klyx.nodegraph.headlessGraph
import com.klyx.nodegraph.input
import com.klyx.nodegraph.instantiate
import com.klyx.nodegraph.output
import com.klyx.nodegraph.rightOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.readByteArray
import kotlin.uuid.Uuid

val LocalExtensionManager = staticCompositionLocalOf<ExtensionManager> {
    error("No ExtensionManager provided.")
}

class ExtensionManager(
    val registry: NodeRegistry = NodeRegistry(installBuiltins = true) {
        install(KlyxSystemExtension, MetadataExtension, EditorLspExtension)
    },
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : Global {
    private var scope = CoroutineScope(dispatcher + SupervisorJob())
    private val log: KxLogger by lazy { logger() }

    val extensions: StateFlow<List<Extension>>
        field = MutableStateFlow(emptyList())
    val isLoading: StateFlow<Boolean>
        field = MutableStateFlow(false)

    // trigger -> graphs
    private val eventListeners = mutableMapOf<String, MutableList<HeadlessGraph>>()

    // extensionId -> triggers
    private val graphTriggers = mutableMapOf<String, List<String>>()

    internal val listener = ExtensionGraphExecutionListener(log)

    suspend fun addOrReplaceExtension(filePath: Path, isLocal: Boolean = true) {
        val newExt = loadSingleExtension(filePath, isLocal)

        extensions.update { list ->
            val index = list.indexOfFirst { it.filePath == filePath }

            if (index >= 0) {
                val old = list[index]
                unregisterGraph(old)

                val newList = list.toMutableList()
                newList[index] = newExt

                registerGraph(newExt)
                newList
            } else {
                registerGraph(newExt)
                list + newExt
            }
        }
    }

    suspend fun reloadExtension(filePath: Path) {
        addOrReplaceExtension(filePath, isLocal = true)
    }

    suspend fun removeExtension(extension: Extension) = removeExtension(extension.filePath)

    suspend fun removeExtension(filePath: Path) {
        val ext = extensions.value.find { it.filePath == filePath } ?: return

        unregisterGraph(ext)

        extensions.update { list ->
            list.filterNot { it.filePath == filePath }
        }

        withContext(Dispatchers.IO) {
            if (fs.exists(filePath)) {
                fs.delete(filePath)
            }
        }
    }

    fun removeById(id: String) {
        val ext = extensions.value.find { it.metadata.id == id } ?: return

        unregisterGraph(ext)

        extensions.update { list ->
            list.filterNot { it.metadata.id == id }
        }
    }

    suspend fun reload(rootDir: Path = Paths.extensionsDir) {
        isLoading.update { true }
        clear()

        val localDir = Path(rootDir, "local")

        suspend fun loadFrom(dir: Path, isLocal: Boolean) {
            if (!fs.exists(dir)) return

            fs.list(dir).forEach { file ->
                if (file.name.endsWith(".kxext")) {
                    addOrReplaceExtension(file, isLocal)
                }
            }
        }

        loadFrom(rootDir, false)
        loadFrom(localDir, true)
        isLoading.update { false }
    }

    fun clear() {
        extensions.value.forEach { unregisterGraph(it) }

        extensions.update { emptyList() }
        eventListeners.clear()
        graphTriggers.clear()

        scope.cancel()
        scope = CoroutineScope(dispatcher + SupervisorJob())
    }

    fun getByPath(path: Path): Extension? = extensions.value.find { it.filePath == path }
    fun getById(id: String): Extension? = extensions.value.find { it.metadata.id == id }
    fun isInstalled(id: String): Boolean = extensions.value.any { it.metadata.id == id }

    fun isLanguageSupported(language: String): Boolean {
        val lower = language.lowercase()
        return extensions.value.any {
            it.metadata.supportedLanguages.contains(lower)
        }
    }

    fun dispatchEvent(event: String, params: Map<String, Any?> = emptyMap()) {
        val graphs = eventListeners[event] ?: return

        for (graph in graphs) {
            scope.launch {
                try {
                    graph.trigger(event, params, listener)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.error { "Extension crash on '$event': ${e.message}" }
                }
            }
        }
    }

    fun dispatchEventForLanguage(
        language: String,
        event: String,
        params: Map<String, Any?> = emptyMap()
    ) {
        val lower = language.lowercase()
        val graphs = eventListeners[event] ?: return

        for (graph in graphs) {
            val meta = graph.getVariable<ExtensionMetadata>(
                MetadataExtension.variables.first().name
            )

            if (meta?.supportedLanguages?.contains(lower) == true) {
                scope.launch {
                    try {
                        graph.trigger(event, params, listener)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        log.error { "Extension crash on '$event': ${e.message}" }
                    }
                }
            }
        }
    }

    private fun registerGraph(ext: Extension) {
        val meta = ext.metadata
        val graph = ext.graph

        if (meta.id.startsWith("broken.")) return

        val triggers = graph.activeTriggers
            .filter { it != OnInitializeMetadataNode.triggerName }

        graphTriggers[meta.id] = triggers

        for (trigger in triggers) {
            eventListeners.getOrPut(trigger) { mutableListOf() }.add(graph)
        }
    }

    private fun unregisterGraph(ext: Extension) {
        val id = ext.metadata.id
        val graph = ext.graph

        val triggers = graphTriggers[id] ?: return

        for (trigger in triggers) {
            eventListeners[trigger]?.remove(graph)
            if (eventListeners[trigger]?.isEmpty() == true) {
                eventListeners.remove(trigger)
            }
        }

        graphTriggers.remove(id)
    }

    private suspend fun loadSingleExtension(
        filePath: Path,
        isLocal: Boolean
    ): Extension {
        val bytes = withContext(Dispatchers.IO) {
            if (!fs.exists(filePath)) {
                error("Extension file '$filePath' not found")
            }
            fs.source(filePath).buffered().use { it.readByteArray() }
        }

        val graph = headlessGraph(bytes, registry)
        graph.trigger(OnInitializeMetadataNode.triggerName, listener = listener)

        val metadata = try {
            graph.getVariable<ExtensionMetadata>(
                MetadataExtension.variables.first().name
            ) ?: error("Missing metadata")
        } catch (_: Exception) {
            ExtensionMetadata(
                id = "broken.${filePath.name}",
                name = "Needs Setup (${filePath.name})",
                author = "Unknown",
                version = "0.0.0",
                description = "Metadata missing. Please configure.",
                supportedLanguages = emptyList()
            )
        }

        return Extension(graph, metadata, filePath, isLocal)
    }
}

class ExtensionGraphExecutionListener(private val logger: KxLogger) : GraphExecutionListener {
    override fun onNodeEnter(nodeId: Uuid) {
        logger.verbose { "Node Enter: $nodeId" }
    }

    override fun onLog(message: String) {
        logger.info { message }
    }

    override fun onError(message: String) {
        logger.error { message }
    }
}

fun createInitialGraph(graphState: GraphState) {
    val initMetaNode = OnInitializeMetadataNode.instantiate(Offset(100f, 150f))
    val makeMetaNode = MakeMetadataNode.instantiate(initMetaNode.below(myHeight = 70f))
    val configNode = MetadataExtension.variables.first().createSetNode(initMetaNode.rightOf(gap = 300f))

    graphState.addNodes(initMetaNode, makeMetaNode, configNode)
    graphState.connect(initMetaNode.execOut, configNode.execIn)
    graphState.connect(
        from = makeMetaNode.output(PinType.ExtensionMetadata),
        to = configNode.input(PinType.ExtensionMetadata)
    )
}
