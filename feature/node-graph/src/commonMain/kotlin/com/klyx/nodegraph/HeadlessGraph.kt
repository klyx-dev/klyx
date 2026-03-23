@file:OptIn(ExperimentalSerializationApi::class)

package com.klyx.nodegraph

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf

class HeadlessGraph(private val registry: NodeRegistry = DefaultNodeRegistry) {

    private val proto = ProtoBuf { encodeDefaults = false }

    @PublishedApi
    internal var compiledGraph: CompiledGraph? = null

    val activeTriggers: Set<String>
        get() = compiledGraph?.nodes
            ?.mapNotNull { it.triggerName }
            ?.filter { it.isNotEmpty() }
            ?.toSet()
            ?: emptySet()

    fun load(bytes: ByteArray) {
        val snapshot: GraphSnapshot = proto.decodeFromByteArray(bytes)
        compiledGraph = GraphCompiler.compile(snapshot, registry)
    }

    /**
     * Executes the default Start flow (any node with no flow inputs).
     */
    suspend fun execute(listener: GraphExecutionListener? = null) {
        val graph = compiledGraph ?: error("Graph not loaded! Call load() first.")
        graph.execute(listener)
    }

    /**
     * Triggers a specific event node (e.g., "event.onStart") and injects parameters.
     */
    suspend fun trigger(
        eventKey: String,
        params: Map<String, Any?> = emptyMap(),
        listener: GraphExecutionListener? = null
    ) {
        val graph = compiledGraph ?: error("Graph not loaded! Call load() first.")
        graph.trigger(eventKey, params, listener)
    }

    /**
     * Retrieves a variable's value from this graph's memory map.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getVariable(name: String): T? {
        val graph = compiledGraph ?: return null
        val variable = graph.variables.values.find { it.name == name } ?: return null
        return graph.variableMemory[variable.id] as? T
    }

    /**
     * Updates a variable's value inside this graph's memory map.
     */
    fun setVariable(name: String, value: Any?) {
        val graph = compiledGraph ?: return
        val variable = graph.variables.values.find { it.name == name } ?: return
        graph.variableMemory[variable.id] = value
    }
}

fun headlessGraph(
    bytes: ByteArray,
    registry: NodeRegistry = DefaultNodeRegistry
) = HeadlessGraph(registry).also { it.load(bytes) }
