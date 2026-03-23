package com.klyx.nodegraph

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.uuid.Uuid

internal sealed interface GraphCommand {
    fun undo(state: GraphState)
    fun redo(state: GraphState)
}


internal data class AddNodeCmd(val node: NodeData) : GraphCommand {
    override fun undo(state: GraphState) = state.removeNodeInternal(node.id)
    override fun redo(state: GraphState) = state.addNodeInternal(node)
}

internal data class RemoveNodeCmd(
    val node: NodeData,
    val connections: List<NodeConnection>,
    val pinVals: Map<Uuid, String>,
) : GraphCommand {
    override fun undo(state: GraphState) {
        state.addNodeInternal(node)
        connections.forEach { state.addConnectionInternal(it) }
        pinVals.forEach { (k, v) -> state.pinValues[k] = v }
    }

    override fun redo(state: GraphState) = state.removeNodeInternal(node.id)
}

internal data class MoveNodesCmd(val moves: List<Pair<Uuid, Offset>>) : GraphCommand {
    override fun undo(state: GraphState) = moves.forEach { (id, d) -> state.translateNodeInternal(id, -d) }
    override fun redo(state: GraphState) = moves.forEach { (id, d) -> state.translateNodeInternal(id, d) }
}

internal data class AddConnectionCmd(val conn: NodeConnection) : GraphCommand {
    override fun undo(state: GraphState) = state.removeConnectionInternal(conn.id)
    override fun redo(state: GraphState) = state.addConnectionInternal(conn)
}

internal data class RemoveConnectionCmd(val conn: NodeConnection) : GraphCommand {
    override fun undo(state: GraphState) = state.addConnectionInternal(conn)
    override fun redo(state: GraphState) = state.removeConnectionInternal(conn.id)
}

internal data class InsertRerouteCmd(
    val rerouteNode: NodeData,
    val removedConn: NodeConnection,
    val newConn1: NodeConnection,
    val newConn2: NodeConnection,
) : GraphCommand {
    override fun undo(state: GraphState) {
        state.removeConnectionInternal(newConn1.id)
        state.removeConnectionInternal(newConn2.id)
        state.removeNodeInternal(rerouteNode.id)
        state.addConnectionInternal(removedConn)
    }

    override fun redo(state: GraphState) {
        state.removeConnectionInternal(removedConn.id)
        state.addNodeInternal(rerouteNode)
        state.addConnectionInternal(newConn1)
        state.addConnectionInternal(newConn2)
    }
}

internal data class AddCommentCmd(val comment: CommentData) : GraphCommand {
    override fun undo(state: GraphState) = state.removeCommentInternal(comment.id)
    override fun redo(state: GraphState) = state.addCommentInternal(comment)
}

internal data class RemoveCommentCmd(val comment: CommentData) : GraphCommand {
    override fun undo(state: GraphState) = state.addCommentInternal(comment)
    override fun redo(state: GraphState) = state.removeCommentInternal(comment.id)
}

internal data class MoveCommentCmd(val id: Uuid, val delta: Offset) : GraphCommand {
    override fun undo(state: GraphState) = state.translateCommentInternal(id, -delta)
    override fun redo(state: GraphState) = state.translateCommentInternal(id, delta)
}

internal data class ResizeCommentCmd(val id: Uuid, val oldSize: Size, val newSize: Size) : GraphCommand {
    override fun undo(state: GraphState) = state.resizeCommentInternal(id, oldSize)
    override fun redo(state: GraphState) = state.resizeCommentInternal(id, newSize)
}

internal data class DisconnectAllCmd(val conns: List<NodeConnection>) : GraphCommand {
    override fun undo(state: GraphState) = conns.forEach { state.addConnectionInternal(it) }
    override fun redo(state: GraphState) = conns.forEach { state.removeConnectionInternal(it.id) }
}

internal data class PasteCmd(
    val nodes: List<NodeData>,
    val connections: List<NodeConnection>,
    val pinValues: Map<Uuid, String>,
) : GraphCommand {
    override fun undo(state: GraphState) {
        nodes.forEach { state.removeNodeInternal(it.id) }
    }

    override fun redo(state: GraphState) {
        nodes.forEach { state.addNodeInternal(it) }
        connections.forEach { state.addConnectionInternal(it) }
        pinValues.forEach { (k, v) -> state.pinValues[k] = v }
    }
}

