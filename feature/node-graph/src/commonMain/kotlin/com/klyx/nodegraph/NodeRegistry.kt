package com.klyx.nodegraph

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import com.klyx.nodegraph.extension.GraphExtension
import com.klyx.nodegraph.extension.builtin.BuiltinExtension
import com.klyx.nodegraph.extension.builtin.StartNode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializer

val DefaultNodeRegistry by lazy { NodeRegistry() }

@Stable
class NodeRegistry {
    private val _nodes = mutableStateListOf<Node>()
    val nodes get() = _nodes.toList()

    private val installedExtensions = mutableListOf<GraphExtension>()

    @PublishedApi
    internal val customTypes = mutableMapOf<String, CustomTypeDefinition<out Any>>()

    @PublishedApi
    internal val enumTypes = mutableMapOf<String, PinType.Enum>()

    /** Registers a predefined Enum type so it appears in the UI variable list. */
    fun registerEnum(enumType: PinType.Enum) {
        enumTypes[enumType.typeName] = enumType
    }

    //listeners for when extensions are installed
    internal val extensionListeners = mutableListOf<(GraphExtension) -> Unit>()

    /**
     * Registers a custom type for serialization.
     * @throws UnserializableTypeException if [T] is not @Serializable.
     * @throws InvalidTypeArgumentException if [T] contains star projections.
     */
    inline fun <reified T : Any> registerCustomType(
        typeName: String = T::class.simpleName!!,
        color: Color = Color(0xFFAAAAAA)
    ) {
        try {
            val typeSerializer = serializer<T>()
            val definition = CustomTypeDefinition(typeName, typeSerializer, color)
            customTypes[typeName] = definition
        } catch (e: SerializationException) {
            throw UnserializableTypeException(typeName, e)
        } catch (e: IllegalArgumentException) {
            throw InvalidTypeArgumentException(typeName, e)
        }
    }

    /**
     * Installs one or more extensions into this registry.
     */
    fun install(vararg extensions: GraphExtension) {
        for (ext in extensions) {
            if (installedExtensions.any { it.name == ext.name }) continue
            ext.install(this)
            installedExtensions.add(ext)
            extensionListeners.forEach { it.invoke(ext) }
        }
    }

    /**
     * Uninstalls an extension
     */
    fun uninstall(extension: GraphExtension) {
        extension.uninstall(this)
        installedExtensions.remove(extension)
    }

    /** Returns true if an extension with the given name is installed. */
    fun isExtensionInstalled(name: String) = installedExtensions.any { it.name == name }
    fun isExtensionInstalled(extension: GraphExtension) = installedExtensions.contains(extension)

    /** Returns all currently installed extensions. */
    fun installedExtensions(): List<GraphExtension> = installedExtensions.toList()

    /**
     * Registers one or more nodes.
     * Throws [IllegalArgumentException] if a key is already registered.
     * Call from the main thread or synchronize externally.
     */
    fun register(vararg nodes: Node) {
        val existing = _nodes.map { it.key }.toSet()
        for (node in nodes) {
            require(node.key !in existing) {
                "NodeRegistry: key '${node.key}' is already registered. " +
                        "Use a unique key or call unregister() first."
            }
        }
        _nodes.addAll(nodes)
    }

    /**
     * Registers nodes, replacing any existing node with the same key.
     */
    fun registerOrReplace(vararg nodes: Node) {
        for (node in nodes) {
            val i = _nodes.indexOfFirst { it.key == node.key }
            if (i >= 0) _nodes[i] = node else _nodes.add(node)
        }
    }

    /**
     * Unregisters a node by key.
     */
    fun unregister(key: String) {
        _nodes.removeAll { it.key == key }
    }

    /**
     * Finds a node by its key.
     * Returns null if not found.
     */
    fun findNodeByKey(key: String): Node? = _nodes.find { it.key == key }

    /**
     * Returns all nodes grouped by category, sorted alphabetically.
     */
    fun grouped(): Map<String, List<Node>> =
        _nodes
            .groupBy { it.category }
            .mapValues { (_, nodes) -> nodes.sortedBy { it.title.lowercase() } }

    /**
     * Returns true if a node with the given key is registered.
     */
    fun contains(key: String): Boolean = _nodes.any { it.key == key }

    /**
     * Removes all registered nodes.
     */
    fun clear() {
        _nodes.clear()
    }
}

inline fun NodeRegistry(
    installBuiltins: Boolean = true,
    includeDefaultStartNode: Boolean = false,
    builder: NodeGraphBuilder.() -> Unit
): NodeRegistry {
    val registry = NodeRegistry()
    nodeGraph(registry) {
        if (installBuiltins) install(BuiltinExtension)
        if (includeDefaultStartNode) node(StartNode)
        builder()
    }
    return registry
}

class CustomTypeDefinition<T : Any>(
    val typeName: String,
    val serializer: KSerializer<T>,
    val color: Color
)
