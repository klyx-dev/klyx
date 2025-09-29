package com.klyx.editor.compose.text.buffer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

/**
 * TreeNode cannot be serialized directly because it contains self-references
 * which will cause a stack overflow error
 * therefore, its internal self-reference property need to be turned into serializable fields
 * and the tree structure needs to be flattened into a list
 */
@Serializable
private data class SerializableNode(
    val id: Int,
    val piece: Piece,
    val color: NodeColor,
    val sizeLeft: Int,
    val lfLeft: Int,
    val parentId: Int,
    val leftId: Int,
    val rightId: Int
)

internal object TreeNodeSerializer : KSerializer<TreeNode> {
    private const val SENTINEL_ID = -0x1
    private const val NULL_ID = -0x2

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("TreeNode") {
        element("nodes", ListSerializer(SerializableNode.serializer()).descriptor)
        element<Int>("rootId")
    }

    override fun serialize(encoder: Encoder, value: TreeNode) {
        encoder.encodeStructure(descriptor) {
            val nodeToId = mutableMapOf<TreeNode, Int>()
            val nodes = mutableListOf<TreeNode>()

            // Traverse the tree to find all nodes and assign IDs
            fun traverse(node: TreeNode) {
                if (node === Sentinel || node === NullTreeNode || node in nodeToId) {
                    return
                }
                nodeToId[node] = nodes.size
                nodes.add(node)
                traverse(node.left)
                traverse(node.right)
            }

            // Start traversal from the root node
            // note that the value node may not the root node
            val rootNode = value.root()
            traverse(rootNode)

            nodeToId[Sentinel] = SENTINEL_ID
            nodeToId[NullTreeNode] = NULL_ID

            val serializableNodes = nodes.map { node ->
                SerializableNode(
                    id = nodeToId.getValue(node),
                    piece = node.piece,
                    color = node.color,
                    sizeLeft = node.sizeLeft,
                    lfLeft = node.lfLeft,
                    parentId = nodeToId.getValue(node.parent),
                    leftId = nodeToId.getValue(node.left),
                    rightId = nodeToId.getValue(node.right)
                )
            }

            encodeSerializableElement(descriptor, 0, ListSerializer(SerializableNode.serializer()), serializableNodes)
            encodeIntElement(descriptor, 1, nodeToId[rootNode] ?: SENTINEL_ID)
        }
    }

    override fun deserialize(decoder: Decoder): TreeNode {
        return decoder.decodeStructure(descriptor) {
            var serializableNodes: List<SerializableNode> = emptyList()
            var rootId = SENTINEL_ID
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> serializableNodes = decodeSerializableElement(
                        descriptor = descriptor,
                        index = 0,
                        deserializer = ListSerializer(SerializableNode.serializer())
                    )

                    1 -> rootId = decodeIntElement(descriptor, 1)
                    CompositeDecoder.DECODE_DONE -> break
                }
            }

            val idToNode = mutableMapOf<Int, TreeNode>()
            idToNode[SENTINEL_ID] = Sentinel
            idToNode[NULL_ID] = NullTreeNode

            // First, create all TreeNode instances without setting references
            for (sNode in serializableNodes) {
                val node = TreeNode(sNode.piece, sNode.color).apply {
                    sizeLeft = sNode.sizeLeft
                    lfLeft = sNode.lfLeft
                }
                idToNode[sNode.id] = node
            }

            // Second, set the parent, left, and right references
            for (sNode in serializableNodes) {
                val node = idToNode.getValue(sNode.id)
                node.parent = idToNode.getValue(sNode.parentId)
                node.left = idToNode.getValue(sNode.leftId)
                node.right = idToNode.getValue(sNode.rightId)
            }

            idToNode[rootId] ?: Sentinel
        }
    }
}

internal object StringBuilderSerializer : KSerializer<StringBuilder> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "kotlin.text.StringBuilder",
        PrimitiveKind.STRING
    )

    override fun serialize(encoder: Encoder, value: StringBuilder) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): StringBuilder {
        return StringBuilder(decoder.decodeString())
    }
}
