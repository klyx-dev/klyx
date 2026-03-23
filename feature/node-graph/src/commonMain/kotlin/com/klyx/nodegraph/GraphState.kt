package com.klyx.nodegraph

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.klyx.nodegraph.core.StandardNodeColors
import com.klyx.nodegraph.extension.GraphExtension
import com.klyx.nodegraph.extension.builtin.BuiltinExtension
import com.klyx.nodegraph.extension.builtin.StartNode
import com.klyx.nodegraph.util.generateId
import com.klyx.nodegraph.util.mixUuid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.uuid.Uuid

@Stable
class GraphState(val registry: NodeRegistry = DefaultNodeRegistry) {
    internal val nodes = mutableStateListOf<NodeData>()
    internal val connections = mutableStateListOf<NodeConnection>()
    internal val comments = mutableStateListOf<CommentData>()
    internal val selectedNodeIds = mutableStateSetOf<Uuid>()

    internal val variables = mutableStateListOf<GraphVariable>()
    internal val variableValues = mutableStateMapOf<Uuid, Any?>()
    internal val customEnums = mutableStateListOf<PinType.Enum>()

    internal var editorScreenOffset by mutableStateOf(Offset.Zero)

    internal var panOffset by mutableStateOf(Offset.Zero)
    internal var scale by mutableFloatStateOf(1f)

    // stores pin centre as an offset FROM the node's graph position.
    // measured once via onGloballyPositioned; valid even when node is off-screen
    internal val pinRelativeOffsets = mutableStateMapOf<Uuid, Offset>()
    internal val pinValues = mutableStateMapOf<Uuid, String>()

    internal val resolvedPinTypes = mutableStateMapOf<Uuid, PinType>()

    private var clipboard = listOf<NodeData>()
    private var clipboardConnections = listOf<NodeConnection>()
    private var clipboardPinValues = mapOf<Uuid, String>()

    var isLoading by mutableStateOf(false)
        internal set

    var canPaste by mutableStateOf(false)
        private set

    internal var liveWire by mutableStateOf<LiveWire?>(null)
        private set

    internal var wireError by mutableStateOf<String?>(null)
        private set

    internal var pendingWireSearch by mutableStateOf<PendingWireSearch?>(null)
        private set

    internal var executionJob by mutableStateOf<Job?>(null)
        private set

    internal val undoStack = UndoStack()

    private var compiledPlan: CompiledGraph? = null
    private var isDirty = true // true = needs recompile

    val isExecuting: Boolean
        get() = executionJob?.isActive == true

    init {
        registry.installedExtensions().fastForEach { injectExtensionVariables(it) }
        registry.extensionListeners.add { extension ->
            injectExtensionVariables(extension)
        }
    }

    internal fun injectExtensionVariables(extension: GraphExtension) {
        extension.variables.forEach { template ->
            addVariable(
                id = template.id,
                name = template.name,
                type = template.type,
                defaultValue = template.defaultValue,
                isSystem = template.isSystem,
                recordUndo = false
            )
        }
    }

    // mark dirty whenever structure changes
    internal fun markDirty() {
        isDirty = true

        // update the cache for every pin
        resolvedPinTypes.clear()
        val visited = mutableSetOf<Uuid>()

        allPinsSnapshot().keys.forEach { pinId ->
            visited.clear()
            resolvedPinTypes[pinId] = resolveType(pinId, visited)
        }
    }

    internal fun getOrCompile(): CompiledGraph {
        if (isDirty || compiledPlan == null) {
            compiledPlan = GraphCompiler.compile(this)
            isDirty = false
        }
        return compiledPlan!!
    }

    internal fun dismissWireSearch() {
        pendingWireSearch = null
    }

    internal fun allPinsSnapshot() = nodes.toList().flatMap { it.pins }.associateBy { it.id }

    internal var headerHeight = 43f // header + divider + top spacer
    internal var pinRowHeight = 28f
    internal var pinDotHalf = 7f // half of pin dot
    internal var nodeWidth = 224f // approx node width
    internal var nodeHeight = 120f // approx node height

    // mathematical fallback when pin has never been on screen
    private fun computedPinPos(pin: NodePin): Offset {
        val node = nodes.find { it.id == pin.nodeId } ?: return Offset.Zero
        val list = node.pins.filter { it.direction == pin.direction }
        val rowIndex = list.indexOf(pin)
        val y = node.position.y + headerHeight + rowIndex * pinRowHeight + pinRowHeight / 2f
        val x = if (pin.direction == PinDirection.Input) node.position.x + pinDotHalf
        else node.position.x + nodeWidth - pinDotHalf
        return Offset(x, y)
    }

    internal fun reportPinScreenCentre(pinId: Uuid, rootCentre: Offset) {
        val local = rootCentre - editorScreenOffset
        val graphPos = screenToGraph(local)
        val pin = allPinsSnapshot()[pinId] ?: return
        val node = nodes.toList().find { it.id == pin.nodeId } ?: return
        pinRelativeOffsets[pinId] = graphPos - node.position
    }

    internal fun resolvePinGraphPos(pinId: Uuid): Offset? {
        val pin = allPinsSnapshot()[pinId] ?: return null
        val node = nodes.find { it.id == pin.nodeId } ?: return null
        val rel = pinRelativeOffsets[pinId]
        return if (rel != null) node.position + rel else computedPinPos(pin)
    }

    internal fun addNodeInternal(node: NodeData) {
        nodes += node
        markDirty()
    }

    internal fun removeNodeInternal(nodeId: Uuid) {
        val node = nodes.find { it.id == nodeId } ?: return
        val pinIds = node.pins.map { it.id }.toSet()
        connections.removeAll { it.outputPinId in pinIds || it.inputPinId in pinIds }
        node.pins.fastForEach {
            pinValues.remove(it.id)
            pinRelativeOffsets.remove(it.id)
        }
        nodes.remove(node)
        markDirty()
    }

    internal fun addConnectionInternal(connection: NodeConnection) {
        connections.removeAll { it.inputPinId == connection.inputPinId }
        connections += connection
        markDirty()
    }

    internal fun removeConnectionInternal(connectionId: Uuid) {
        connections.removeAll { it.id == connectionId }
        markDirty()
    }

    internal fun translateNodeInternal(nodeId: Uuid, delta: Offset) {
        val i = nodes.indexOfFirst { it.id == nodeId }
        if (i >= 0) {
            nodes[i] = nodes[i].copy(position = nodes[i].position + delta)
        }
    }

    internal fun addCommentInternal(comment: CommentData) {
        comments += comment
    }

    internal fun removeCommentInternal(id: Uuid) {
        comments.removeAll { it.id == id }
    }

    internal fun translateCommentInternal(id: Uuid, delta: Offset) {
        val i = comments.indexOfFirst { it.id == id }
        if (i >= 0) comments[i] = comments[i].copy(position = comments[i].position + delta)
    }

    internal fun resizeCommentInternal(id: Uuid, newSize: Size) {
        val i = comments.indexOfFirst { it.id == id }
        if (i >= 0) comments[i] = comments[i].copy(size = newSize)
    }

    fun addNode(node: NodeData) {
        addNodeInternal(node)
        undoStack.push(AddNodeCmd(node))
    }

    fun removeNode(nodeId: Uuid) {
        val node = nodes.find { it.id == nodeId } ?: return
        val pinIds = node.pins.map { it.id }.toSet()
        val conns = connections.filter { it.outputPinId in pinIds || it.inputPinId in pinIds }
        val vals = pinValues.entries.filter { it.key in pinIds }.associate { it.key to it.value }
        removeNodeInternal(nodeId)
        undoStack.push(RemoveNodeCmd(node, conns, vals))
    }

    fun duplicateNode(nodeId: Uuid) {
        val original = nodes.find { it.id == nodeId } ?: return
        val newId = generateId()
        val newPins = original.pins.fastMap { it.copy(id = generateId(), nodeId = newId) }
        original.pins.zip(newPins).fastForEach { (oldPin, newPin) ->
            pinValues[oldPin.id]?.let {
                pinValues[newPin.id] = it
                markDirty()
            }
        }
        val newNode = original.copy(
            id = newId,
            pins = newPins,
            position = original.position + Offset(40f, 40f),
        )
        addNodeInternal(newNode)
        undoStack.push(AddNodeCmd(newNode))
    }

    fun addConnection(outputPinId: Uuid, inputPinId: Uuid) {
        val old = connections.find { it.inputPinId == inputPinId }
        if (old != null) undoStack.push(RemoveConnectionCmd(old))

        val outType = allPinsSnapshot()[outputPinId]?.type
        val inType = allPinsSnapshot()[inputPinId]?.type
        val isCast = outType != null && inType != null && outType != inType

        val conn = NodeConnection(mixUuid(outputPinId, inputPinId), outputPinId, inputPinId, isCast)
        addConnectionInternal(conn)
        undoStack.push(AddConnectionCmd(conn))
    }

    fun removeConnectionForInput(inputPinId: Uuid) {
        val conn = connections.find { it.inputPinId == inputPinId } ?: return
        removeConnectionInternal(conn.id)
        undoStack.push(RemoveConnectionCmd(conn))
    }

    fun removeConnection(connectionId: Uuid) {
        val conn = connections.find { it.id == connectionId } ?: return
        removeConnectionInternal(connectionId)
        undoStack.push(RemoveConnectionCmd(conn))
    }

    fun addComment(comment: CommentData) {
        addCommentInternal(comment)
        undoStack.push(AddCommentCmd(comment))
    }

    fun removeComment(id: Uuid) {
        val comment = comments.find { it.id == id } ?: return
        removeCommentInternal(id)
        undoStack.push(RemoveCommentCmd(comment))
    }

    internal fun beginWireDragFromOutput(outputPinId: Uuid) {
        liveWire = LiveWire(outputPinId, true, resolvePinGraphPos(outputPinId) ?: return)
    }

    internal fun beginWireDragFromInput(inputPinId: Uuid) {
        liveWire = LiveWire(inputPinId, false, resolvePinGraphPos(inputPinId) ?: return)
    }

    internal fun detachAndBeginDrag(inputPinId: Uuid) {
        val conn = connectionForInput(inputPinId) ?: return
        connections.remove(conn)
        liveWire = LiveWire(
            anchorPinId = conn.outputPinId,
            anchorIsOutput = true,
            tipGraphPos = resolvePinGraphPos(inputPinId) ?: resolvePinGraphPos(conn.outputPinId) ?: return,
        )
        markDirty()
    }

    internal fun advanceWire(graphDelta: Offset) {
        liveWire = liveWire?.let { it.copy(tipGraphPos = it.tipGraphPos + graphDelta) }
    }

    internal fun commitWire() {
        val wire = liveWire ?: return
        liveWire = null
        val allPins = allPinsSnapshot()
        val anchor = allPins[wire.anchorPinId] ?: return
        val snap = SNAP_RADIUS_PX / scale

        // find the nearest pin regardless of compatibility first
        val nearestHit = allPins.entries
            .asSequence()
            .filter { (_, p) -> p.nodeId != anchor.nodeId }
            .mapNotNull { (id, _) -> resolvePinGraphPos(id)?.let { pos -> id to pos } }
            .filter { (_, pos) -> (pos - wire.tipGraphPos).getDistance() <= snap }
            .minByOrNull { (_, pos) -> (pos - wire.tipGraphPos).getDistance() }

        if (nearestHit == null) {
            // Released on empty canvas. show wire search popup
            pendingWireSearch = PendingWireSearch(
                anchorPinId = wire.anchorPinId,
                anchorIsOutput = wire.anchorIsOutput,
                tipGraphPos = wire.tipGraphPos,
                pinType = anchor.type,
            )
            markDirty()
            return
        }

        val (targetId, _) = nearestHit
        val target = allPins[targetId] ?: return

        if (target.direction == anchor.direction) {
            val dirName = if (anchor.direction == PinDirection.Output) "output" else "input"
            showWireError("Cannot connect two $dirName pins together.")
            return
        }

        if (target.nodeId == anchor.nodeId) {
            showWireError("Cannot connect a node to itself.")
            return
        }

        val fromResolved = resolveType(wire.anchorPinId)
        val toResolved = resolveType(nearestHit.first)

        if (!PinType.canAutoCast(fromResolved, toResolved)) {
            showWireError(incompatibleTypeMessage(anchor.type, target.type))
            return
        }

        val (outPinId, inPinId) = if (wire.anchorIsOutput) {
            wire.anchorPinId to targetId
        } else {
            targetId to wire.anchorPinId
        }
        addConnection(outPinId, inPinId)
    }

    internal fun abortWire() {
        liveWire = null
    }

    private fun incompatibleTypeMessage(from: PinType, to: PinType): String {
        if (to is PinType.Wildcard && !to.accepts(from)) {
            val allowed = to.allowedTypes.joinToString { it.typeName }
            return "This pin only accepts: $allowed"
        }
        if (from is PinType.Wildcard && !from.accepts(to)) {
            val allowed = from.allowedTypes.joinToString { it.typeName }
            return "This pin can only connect to: $allowed"
        }

        return when {
            from == PinType.String && to == PinType.Float -> "Cannot connect String -> Float. Add a 'String to Float' conversion node."
            from == PinType.String && to == PinType.Integer -> "Cannot connect String -> Int. Add a 'String to Int' conversion node."
            from == PinType.Flow && to != PinType.Flow -> "Exec pins can only connect to other Exec pins."
            to == PinType.Flow && from != PinType.Flow -> "Data pins cannot connect to Exec pins."
            from is PinType.Custom && to is PinType.Custom -> "Type mismatch: '${from.typeName}' is not compatible with '${to.typeName}'."
            from is PinType.Custom -> "Cannot connect '${from.typeName}' to ${to.typeName}."
            to is PinType.Custom -> "Cannot connect ${from.typeName} to '${to.typeName}'."
            else -> "Type mismatch: ${from.typeName} cannot connect to ${to.typeName}."
        }
    }

    private fun showWireError(message: String) {
        wireError = message
    }

    internal fun clearWireError() {
        wireError = null
    }

    internal fun zoomToFit(viewSize: Size) {
        if (nodes.isEmpty()) return

        val minX = nodes.minOf { it.position.x }
        val minY = nodes.minOf { it.position.y }
        val maxX = nodes.maxOf { it.position.x } + nodeWidth
        val maxY = nodes.maxOf { it.position.y } + nodeHeight

        val graphW = maxX - minX
        val graphH = maxY - minY

        val padding = 80f  // screen-pixel breathing room on each side

        val scaleX = (viewSize.width - padding * 2f) / graphW
        val scaleY = (viewSize.height - padding * 2f) / graphH

        scale = scaleX.coerceAtMost(scaleY).coerceIn(0.10f, 5f)

        // centre the bounding box in the viewport
        panOffset = Offset(
            x = viewSize.width / 2f - (minX + graphW / 2f) * scale,
            y = viewSize.height / 2f - (minY + graphH / 2f) * scale,
        )
    }

    internal fun insertRerouteNode(connection: NodeConnection, graphPosition: Offset) {
        val pinType = allPinsSnapshot()[connection.outputPinId]?.type ?: return
        val nodeId = generateId()
        val inPinId = generateId()
        val outPinId = generateId()
        val conn1Id = generateId()
        val conn2Id = generateId()

        val inPin = NodePin(inPinId, "", pinType, PinDirection.Input, nodeId)
        val outPin = NodePin(outPinId, "", pinType, PinDirection.Output, nodeId)

        val reroute = NodeData(
            id = nodeId,
            title = "",
            definitionKey = "",
            kind = NodeKind.Reroute,
            headerColor = pinType.color,
            pins = listOf(inPin, outPin),
            position = graphPosition - Offset(10f, 8f) // offset position so the diamond centre lands on the tap point
        )

        val c1 = NodeConnection(conn1Id, connection.outputPinId, inPinId)
        val c2 = NodeConnection(conn2Id, outPinId, connection.inputPinId)

        removeConnectionInternal(connection.id)
        addNodeInternal(reroute)
        addConnectionInternal(c1)
        addConnectionInternal(c2)
        undoStack.push(InsertRerouteCmd(reroute, connection, c1, c2))
    }

    internal fun addCustomEnum(name: String, entries: List<String>, color: Color = StandardNodeColors.Types.Enum) {
        val newEnum = PinType.Enum(name, entries, color)
        customEnums.add(newEnum)
        markDirty()
    }

    internal fun addCustomEnum(enum: PinType.Enum) {
        customEnums.add(enum)
        markDirty()
    }

    fun selectNode(nodeId: Uuid) {
        selectedNodeIds.clear()
        selectedNodeIds.add(nodeId)
    }

    fun toggleSelect(nodeId: Uuid) {
        if (nodeId in selectedNodeIds) selectedNodeIds.remove(nodeId)
        else selectedNodeIds.add(nodeId)
    }

    fun deselectAll() {
        selectedNodeIds.clear()
    }

    fun copySelected() {
        if (selectedNodeIds.isEmpty()) return
        val selectedNodes = nodes.filter { it.id in selectedNodeIds }
        val selectedPinIds = selectedNodes.flatMap { it.pins }.map { it.id }.toSet()
        // only copy connections where BOTH pins are in the selection
        clipboardConnections = connections.filter {
            it.outputPinId in selectedPinIds && it.inputPinId in selectedPinIds
        }
        clipboardPinValues = pinValues.entries
            .filter { it.key in selectedPinIds }
            .associate { it.key to it.value }
        clipboard = selectedNodes
        canPaste = clipboard.isNotEmpty()
    }

    fun paste() {
        if (clipboard.isEmpty()) return
        // remap all IDs so pasted nodes are independent
        val idMap = clipboard.associate { it.id to generateId() }
        val pinMap = HashMap<Uuid, Uuid>()

        val newNodes = clipboard.map { original ->
            val newId = idMap[original.id]!!
            val newPins = original.pins.mapIndexed { i, pin ->
                val newPinId = generateId()
                pinMap[pin.id] = newPinId
                pin.copy(id = newPinId, nodeId = newId)
            }
            original.copy(
                id = newId,
                pins = newPins,
                position = original.position + Offset(60f, 60f),
            )
        }

        val newConns = clipboardConnections.mapNotNull { conn ->
            val newOut = pinMap[conn.outputPinId] ?: return@mapNotNull null
            val newIn = pinMap[conn.inputPinId] ?: return@mapNotNull null
            NodeConnection(mixUuid(newOut, newIn), newOut, newIn)
        }

        val newPinValues = clipboardPinValues.entries
            .mapNotNull { (oldPinId, value) ->
                val newPinId = pinMap[oldPinId] ?: return@mapNotNull null
                newPinId to value
            }.toMap()

        newNodes.forEach { addNodeInternal(it) }
        newConns.forEach { addConnectionInternal(it) }
        newPinValues.forEach { (k, v) ->
            pinValues[k] = v
            markDirty()
        }

        undoStack.push(PasteCmd(newNodes, newConns, newPinValues))

        // select pasted nodes
        selectedNodeIds.clear()
        selectedNodeIds.addAll(newNodes.map { it.id })
    }

    fun stopExecution() {
        executionJob?.cancel()
        executionJob = null
    }

    fun executeAsync(scope: CoroutineScope, listener: GraphExecutionListener? = null) {
        if (isExecuting) stopExecution()

        executionJob = scope.launch {
            try {
                execute(listener)
            } catch (_: CancellationException) {
                listener?.onLog("Execution stopped by user.")
            } finally {
                executionJob = null
            }
        }
    }

    suspend fun execute(listener: GraphExecutionListener? = null) {
        GraphExecutor(getOrCompile()).run(
            onStep = { id -> listener?.onNodeEnter(id) },
            onLog = { msg -> listener?.onLog(msg) },
            onComplete = { listener?.onComplete() },
            onError = { msg -> listener?.onError(msg) },
        )
    }

    /**
     * Fires a named trigger. executes all [EventNode]s whose [triggerName][EventNode.triggerName] matches.
     *
     * @param name   The trigger name to fire.
     * @param inputs Values injected into the matching event node's output pins.
     *               Key = pin label, Value = the value to inject.
     */
    suspend fun trigger(
        name: String,
        inputs: Map<String, Any?> = emptyMap(),
        listener: GraphExecutionListener? = null,
    ) {
        val plan = getOrCompile()

        // inject values into the plan's defaultValues so each fresh executor starts with them
        nodes
            .filter { node ->
                (registry.findNodeByKey(node.definitionKey) as? EventNode)
                    ?.triggerName == name
            }
            .forEach { node ->
                inputs.forEach { (label, value) ->
                    node.pins
                        .firstOrNull { it.label == label && it.direction == PinDirection.Output }
                        ?.let { pin ->
                            for (n in plan.nodes) {
                                for (out in n.dataOutputs) {
                                    if (out.pinId == pin.id) {
                                        plan.defaultValues[out.slot] = value
                                    }
                                }
                            }
                        }
                }
            }

        GraphExecutor(plan, triggerName = name).run(
            onStep = { id -> listener?.onNodeEnter(id) },
            onLog = { msg -> listener?.onLog(msg) },
            onComplete = { listener?.onComplete() },
            onError = { msg -> listener?.onError(msg) },
        )
    }

    internal fun spawnAndConnect(node: Node, graphPosition: Offset, pinIndex: Int) {
        val search = pendingWireSearch ?: return
        pendingWireSearch = null

        val newNode = node.instantiate(graphPosition)
        addNode(newNode)
        val targetPin = newNode.pins.getOrNull(pinIndex) ?: return

        val (outPinId, inPinId) = if (search.anchorIsOutput) {
            search.anchorPinId to targetPin.id
        } else {
            targetPin.id to search.anchorPinId
        }
        addConnection(outPinId, inPinId)
    }

    fun graphToScreen(g: Offset): Offset = g * scale + panOffset
    fun screenToGraph(local: Offset): Offset = (local - panOffset) / scale

    internal fun viewCentreGraph(w: Float, h: Float): Offset = screenToGraph(Offset(w / 2f, h / 2f))

    internal fun clearVariables() {
        variables.clear()
        variableValues.clear()
    }

    // samples a cubic bezier at parameter t (0..1)
    private fun sampleBezier(start: Offset, end: Offset, t: Float): Offset {
        val tangent = abs(end.x - start.x).coerceAtLeast(80f) * 0.5f
        val cp1 = Offset(start.x + tangent, start.y)
        val cp2 = Offset(end.x - tangent, end.y)
        val mt = 1f - t
        return Offset(
            x = mt * mt * mt * start.x + 3 * mt * mt * t * cp1.x + 3 * mt * t * t * cp2.x + t * t * t * end.x,
            y = mt * mt * mt * start.y + 3 * mt * mt * t * cp1.y + 3 * mt * t * t * cp2.y + t * t * t * end.y,
        )
    }

    fun findWireAtScreenPosition(screenPosition: Offset, hitThresholdPx: Float = 22f): WireHit? {
        var bestDist = hitThresholdPx
        var bestHit: WireHit? = null

        for (connection in connections.toList()) {
            val start = graphToScreen(resolvePinGraphPos(connection.outputPinId) ?: continue)
            val end = graphToScreen(resolvePinGraphPos(connection.inputPinId) ?: continue)
            for (i in 0..60) {
                val pt = sampleBezier(start, end, i / 60f)
                val dist = (pt - screenPosition).getDistance()
                if (dist < bestDist) {
                    bestDist = dist
                    bestHit = WireHit(connection, screenToGraph(pt))
                }
            }
        }
        return bestHit
    }

    fun isOutputPinConnected(pinId: Uuid) = connections.any { it.outputPinId == pinId }
    fun isInputPinConnected(pinId: Uuid) = connections.any { it.inputPinId == pinId }

    fun connectionForInput(pinId: Uuid) = connections.find { it.inputPinId == pinId }

    fun setPinValue(pinId: Uuid, value: String) {
        pinValues[pinId] = value
        markDirty()
    }

    fun clearAll() {
        nodes.clear()
        connections.clear()
        pinValues.clear()
        comments.clear()
        pinRelativeOffsets.clear()
        clearVariables()
        liveWire = null
        panOffset = Offset.Zero
        scale = 1f
    }

    companion object {
        const val SNAP_RADIUS_PX = 44f
    }
}

internal fun GraphState.resolveType(pinId: Uuid, visited: MutableSet<Uuid> = mutableSetOf()): PinType {
    // return a generic wildcard if we hit a cyclic loop or a missing pin
    if (!visited.add(pinId)) return PinType.Wildcard()
    val pin = allPinsSnapshot()[pinId] ?: return PinType.Wildcard()

    if (pin.type !is PinType.Wildcard) return pin.type

    // Wildcard output. infer from node's inferOutputTypes
    if (pin.direction == PinDirection.Output) {
        return inferredOutputType(pin, visited)
    }

    // Wildcard input. trace to connected output pin
    if (pin.direction == PinDirection.Input) {
        val srcId = connections.find { it.inputPinId == pinId }?.outputPinId
        if (srcId != null) return resolveType(srcId, visited)
    }

    return pin.type
}

private fun GraphState.inferredOutputType(
    outPin: NodePin,
    visited: MutableSet<Uuid>,
): PinType {
    val node = nodes.find { it.id == outPin.nodeId } ?: return outPin.type
    val def = registry.findNodeByKey(node.definitionKey) ?: return outPin.type

    val inputTypes = node.pins
        .filter { it.type != PinType.Flow && it.direction == PinDirection.Input }
        .associate { it.label to resolveType(it.id, visited) }

    return def.inferOutputTypes(inputTypes)[outPin.label] ?: outPin.type
}

data class WireHit(val connection: NodeConnection, val graphPosition: Offset)

@Suppress("ParamsComparedByRef")
@Composable
fun rememberGraphState(
    initialBytes: ByteArray? = null,
    installBuiltins: Boolean = true,
    includeDefaultStartNode: Boolean = false,
    builder: NodeGraphBuilder.() -> Unit = {},
): GraphState = remember(installBuiltins, includeDefaultStartNode) {
    val registry = NodeRegistry()
    nodeGraph(registry) {
        if (installBuiltins) install(BuiltinExtension)
        if (includeDefaultStartNode) node(StartNode)
        builder()
    }
    GraphState(registry).also { state ->
        initialBytes?.let { state.restoreFromBytes(it) }
    }
}
