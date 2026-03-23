@file:UseSerializers(ColorSerializer::class, OffsetSerializer::class)
@file:OptIn(ExperimentalSerializationApi::class)

package com.klyx.nodegraph

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.klyx.nodegraph.util.ColorSerializer
import com.klyx.nodegraph.util.OffsetSerializer
import com.klyx.nodegraph.util.generateId
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.protobuf.ProtoNumber
import kotlin.uuid.Uuid

@Immutable
@Serializable
data class NodeData(
    @ProtoNumber(1)
    val id: Uuid,
    @ProtoNumber(2)
    val title: String,
    @ProtoNumber(3)
    val definitionKey: String,
    @ProtoNumber(4)
    val kind: NodeKind = NodeKind.Custom,
    @ProtoNumber(5)
    val headerColor: Color = Color(0xFF2979FF),
    @ProtoNumber(6)
    val pins: List<NodePin> = emptyList(),
    @ProtoNumber(7)
    val position: Offset = Offset.Zero
) {
    companion object {
        fun from(node: Node, position: Offset): NodeData {
            val nodeId = generateId()
            val pins = node.pins.mapIndexed { i, pin ->
                NodePin(
                    id = generateId(),
                    label = pin.label,
                    type = pin.type,
                    direction = pin.direction,
                    nodeId = nodeId,
                    showInHeader = pin.showInHeaderIfPossible,
                    defaultValue = pin.defaultValue
                )
            }
            return NodeData(
                id = nodeId,
                title = node.title,
                headerColor = node.headerColor,
                pins = pins,
                position = position,
                definitionKey = node.key,
            )
        }
    }
}

fun Node.instantiate(position: Offset): NodeData = NodeData.from(this, position)
