package com.klyx.nodegraph

import androidx.compose.ui.graphics.Color
import com.klyx.nodegraph.extension.GraphExtension
import com.klyx.nodegraph.extension.Variable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializer

inline fun nodeGraph(
    registry: NodeRegistry = DefaultNodeRegistry,
    block: NodeGraphBuilder.() -> Unit,
) = NodeGraphBuilder(registry).apply(block)

class NodeGraphBuilder @PublishedApi internal constructor(
    @PublishedApi internal val registry: NodeRegistry,
) {
    /**
     * Installs a pre-built [GraphExtension].
     *
     * ```kotlin
     * install(BuiltinExtension)
     * ```
     */
    fun install(vararg extensions: GraphExtension) {
        registry.install(*extensions)
    }

    /**
     * Defines and installs an inline extension without creating a separate class.
     *
     * ```kotlin
     * extension(name = "my-nodes", version = "1.0.0") {
     *     node(MyNode)
     *     cast(TypeA to TypeB) { value -> convert(value) }
     * }
     * ```
     */
    fun extension(
        name: String,
        version: String = "1.0.0",
        block: ExtensionBuilder.() -> Unit,
    ) {
        val builder = ExtensionBuilder(name, version).apply(block)
        registry.install(builder.build())
    }

    /**
     * Registers a single node directly without wrapping it in an extension.
     * Useful for quick prototyping. Prefer [extension] for production code.
     */
    fun node(vararg nodes: Node) {
        registry.register(*nodes)
    }

    /**
     * Registers a cast rule directly.
     */
    fun cast(
        pair: Pair<PinType, PinType>,
        fn: (Any?) -> Any?,
    ) {
        PinType.registerCast(pair.first, pair.second, fn)
    }

    /**
     * Registers bidirectional cast rules in one call.
     */
    fun cast(
        pair: Pair<PinType, PinType>,
        forward: (Any?) -> Any?,
        backward: (Any?) -> Any?,
    ) {
        PinType.registerCastBidirectional(pair.first, pair.second, forward, backward)
    }

    /**
     * Registers a custom serializable type directly to the registry using a PinType.
     *
     * ```kotlin
     * customType<Vector3>(PinType.Vector3)
     * ```
     */
    inline fun <reified T : Any> customType(customPin: PinType.Custom) {
        registry.registerCustomType<T>(customPin.typeName, customPin.color)
    }

    /**
     * Registers a custom serializable type directly to the registry using a name and color.
     *
     * ```kotlin
     * customType<PlayerData>("PlayerData", Color.Red)
     * ```
     */
    inline fun <reified T : Any> customType(
        typeName: String = T::class.simpleName!!,
        color: Color = Color(0xFFAAAAAA)
    ) {
        registry.registerCustomType<T>(typeName, color)
    }

    /**
     * Registers an Enum type directly to the registry so users can create variables of this type.
     * * ```kotlin
     * enumType(PinType.enum<BlendMode>())
     * ```
     */
    fun enumType(enumType: PinType.Enum) {
        registry.registerEnum(enumType)
    }
}

class ExtensionBuilder internal constructor(
    private val name: String,
    private val version: String
) {
    private val nodes = mutableListOf<Node>()
    private val casts = mutableListOf<Triple<PinType, PinType, (Any?) -> Any?>>()

    @PublishedApi
    internal val customTypes = mutableListOf<CustomTypeDefinition<out Any>>()

    @PublishedApi
    internal val enumTypes = mutableListOf<PinType.Enum>()

    @PublishedApi
    internal val variables = mutableListOf<Variable>()

    /** Registers one or more nodes into this extension. */
    fun node(vararg n: Node) {
        nodes.addAll(n)
    }

    /** Registers a one-directional cast rule into this extension. */
    fun cast(pair: Pair<PinType, PinType>, fn: (Any?) -> Any?) {
        casts.add(Triple(pair.first, pair.second, fn))
    }

    /** Registers bidirectional cast rules into this extension. */
    fun cast(
        pair: Pair<PinType, PinType>,
        forward: (Any?) -> Any?,
        backward: (Any?) -> Any?,
    ) {
        casts.add(Triple(pair.first, pair.second, forward))
        casts.add(Triple(pair.second, pair.first, backward))
    }

    /** Registers a custom type into this extension using a PinType. */
    inline fun <reified T : Any> customType(customPin: PinType.Custom) {
        try {
            val def = CustomTypeDefinition(customPin.typeName, serializer<T>(), customPin.color)
            customTypes.add(def)
        } catch (e: SerializationException) {
            throw UnserializableTypeException(customPin.typeName, e)
        } catch (e: IllegalArgumentException) {
            throw InvalidTypeArgumentException(customPin.typeName, e)
        }
    }

    /** Registers a custom type into this extension using a name and color. */
    inline fun <reified T : Any> customType(
        typeName: String = T::class.simpleName!!,
        color: Color = Color(0xFFAAAAAA)
    ) {
        try {
            val def = CustomTypeDefinition(typeName, serializer<T>(), color)
            customTypes.add(def)
        } catch (e: SerializationException) {
            throw UnserializableTypeException(typeName, e)
        } catch (e: IllegalArgumentException) {
            throw InvalidTypeArgumentException(typeName, e)
        }
    }

    /** Registers an Enum type so users can create variables of this type. */
    fun enumType(enumType: PinType.Enum) {
        enumTypes.add(enumType)
    }

    /** Defines a variable template required by this extension. */
    fun variable(
        name: String,
        type: PinType,
        defaultValue: Any? = null,
        isSystem: Boolean = false
    ) {
        variables.add(Variable(name, type, defaultValue, isSystem))
    }

    internal fun build(): GraphExtension {
        val capturedNodes = nodes.toList()
        val capturedCasts = casts.toList()
        val capturedTypes = customTypes.toList()
        val capturedEnums = enumTypes.toList()
        val capturedVars = variables.toList()
        val extName = name
        val extVersion = version

        return object : GraphExtension {
            override val name = extName
            override val version = extVersion
            override val variables = capturedVars

            override fun install(registry: NodeRegistry) {
                if (capturedNodes.isNotEmpty()) registry.register(*capturedNodes.toTypedArray())
                capturedCasts.forEach { (from, to, fn) -> PinType.registerCast(from, to, fn) }
                capturedTypes.forEach { def -> registry.customTypes[def.typeName] = def }
                capturedEnums.forEach { enumDef -> registry.registerEnum(enumDef) }
            }

            override fun uninstall(registry: NodeRegistry) {
                capturedCasts.forEach { (from, to, _) -> PinType.unregisterCast(from, to) }
                capturedTypes.forEach { def -> registry.customTypes.remove(def.typeName) }
                capturedEnums.forEach { enumDef -> registry.enumTypes.remove(enumDef.typeName) }
            }
        }
    }
}
