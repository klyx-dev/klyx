@file:OptIn(ExperimentalSerializationApi::class)

package com.klyx.nodegraph

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import kotlin.uuid.Uuid

@Serializable
internal data class GraphSnapshot(
    @ProtoNumber(1) val version: Int = GraphSerializer.CURRENT_VERSION,
    @ProtoNumber(2) val nodes: List<NodeData> = emptyList(),
    @ProtoNumber(3) val connections: List<NodeConnection> = emptyList(),
    @ProtoNumber(4) val pinValues: List<PinValueEntry> = emptyList(),
    @ProtoNumber(5) val comments: List<CommentData> = emptyList(),
    @ProtoNumber(6) val viewport: ViewportState = ViewportState(),
    @ProtoNumber(7) val variables: List<GraphVariable> = emptyList(),
    @ProtoNumber(8) val variableValues: List<VariableValueEntry> = emptyList(),
    @ProtoNumber(10) val customEnums: List<PinType.Enum> = emptyList()
)

@Serializable
internal data class VariableValueEntry(
    @ProtoNumber(1) val variableId: String,
    @ProtoNumber(2) val value: String = "",
    @ProtoNumber(3) val customPayload: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as VariableValueEntry

        if (variableId != other.variableId) return false
        if (value != other.value) return false
        if (!customPayload.contentEquals(other.customPayload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = variableId.hashCode()
        result = 31 * result + value.hashCode()
        result = 31 * result + (customPayload?.contentHashCode() ?: 0)
        return result
    }
}

@Serializable
internal data class PinValueEntry(
    @ProtoNumber(1) val pinId: Uuid,
    @ProtoNumber(2) val value: String,
)

@Serializable
internal data class ViewportState(
    @ProtoNumber(1) val panX: Float = 0f,
    @ProtoNumber(2) val panY: Float = 0f,
    @ProtoNumber(3) val scale: Float = 1f,
)

internal object GraphSerializer {

    const val CURRENT_VERSION = 1

    private val proto = ProtoBuf { encodeDefaults = false }

    fun encode(state: GraphState, density: Float): ByteArray {
        val snapshot = GraphSnapshot(
            nodes = state.nodes.map {
                it.copy(position = Offset(it.position.x / density, it.position.y / density))
            },
            connections = state.connections.toList(),
            pinValues = state.pinValues.entries.map { (k, v) -> PinValueEntry(k, v) },
            comments = state.comments.map {
                it.copy(
                    position = Offset(it.position.x / density, it.position.y / density),
                    size = Size(
                        it.size.width / density,
                        it.size.height / density
                    )
                )
            },
            viewport = ViewportState(
                panX = state.panOffset.x / density,
                panY = state.panOffset.y / density,
                scale = state.scale,
            ),
            variables = state.variables.toList(),
            variableValues = state.variables.mapNotNull { v ->
                val liveValue = state.variableValues[v.id] ?: return@mapNotNull null

                if (v.type is PinType.Custom) {
                    @Suppress("UNCHECKED_CAST")
                    val def = state.registry.customTypes[v.type.typeName] as? CustomTypeDefinition<Any>
                        ?: return@mapNotNull null

                    val bytes = proto.encodeToByteArray(def.serializer, liveValue)
                    VariableValueEntry(variableId = v.id.toString(), customPayload = bytes)
                } else {
                    VariableValueEntry(variableId = v.id.toString(), value = liveValue.toString())
                }
            },
            customEnums = state.customEnums.toList()
        )
        return proto.encodeToByteArray(snapshot)
    }

    fun decode(bytes: ByteArray, state: GraphState, density: Float) {
        state.isLoading = true
        val snapshot: GraphSnapshot = proto.decodeFromByteArray(bytes)
        state.clearAll()

        val liveValues = snapshot.variableValues.associateBy { Uuid.parse(it.variableId) }

        snapshot.variables.forEach { v ->
            val actualType = if (v.type is PinType.Custom) {
                val def = state.registry.customTypes[v.type.typeName]
                if (def != null) PinType.Custom(def.typeName, def.color) else v.type
            } else v.type

            val patchedVar = v.copy(type = actualType)
            state.variables += patchedVar

            val entry = liveValues[patchedVar.id]
            if (entry != null) {
                if (patchedVar.type is PinType.Custom && entry.customPayload != null) {
                    @Suppress("UNCHECKED_CAST")
                    val def = state.registry.customTypes[patchedVar.type.typeName] as? CustomTypeDefinition<Any>
                    if (def != null) {
                        val obj = proto.decodeFromByteArray(def.serializer, entry.customPayload)
                        state.variableValues[patchedVar.id] = obj
                    }
                } else {
                    state.variableValues[patchedVar.id] = parseVariableDefault(entry.value, patchedVar.type)
                }
            } else {
                state.variableValues[patchedVar.id] = parseVariableDefault(patchedVar.defaultValue, patchedVar.type)
            }
        }

        snapshot.nodes.forEach { node ->
            val patchedPins = node.pins.map { pin ->
                if (pin.type is PinType.Custom) {
                    val def = state.registry.customTypes[pin.type.typeName]
                    if (def != null) pin.copy(type = PinType.Custom(def.typeName, def.color)) else pin
                } else pin
            }

            val isVarNode = isGetVariableKey(node.definitionKey) || isSetVariableKey(node.definitionKey)
            val patchedHeaderColor = if (isVarNode) {
                val varId = variableIdFromKey(node.definitionKey)
                val variable = state.variables.find { it.id == varId }
                variable?.type?.color ?: node.headerColor
            } else {
                node.headerColor
            }

            state.addNodeInternal(
                node.copy(
                    position = Offset(node.position.x * density, node.position.y * density),
                    pins = patchedPins,
                    headerColor = patchedHeaderColor
                )
            )
        }
        snapshot.customEnums.forEach(state::addCustomEnum)
        snapshot.connections.forEach(state::addConnectionInternal)
        snapshot.pinValues.forEach { state.pinValues[it.pinId] = it.value }
        snapshot.comments.forEach { comment ->
            state.addCommentInternal(
                comment.copy(
                    position = Offset(comment.position.x * density, comment.position.y * density),
                    size = Size(
                        comment.size.width * density,
                        comment.size.height * density
                    )
                )
            )
        }

        state.panOffset = Offset(
            x = snapshot.viewport.panX * density,
            y = snapshot.viewport.panY * density
        )
        state.scale = snapshot.viewport.scale

        state.registry.installedExtensions().forEach { ext ->
            state.injectExtensionVariables(ext)
        }

        state.markDirty()
        state.isLoading = false
    }
}

fun GraphState.toBytes(): ByteArray = GraphSerializer.encode(this, density.density)
fun GraphState.restoreFromBytes(bytes: ByteArray) = GraphSerializer.decode(bytes, this, density.density)
