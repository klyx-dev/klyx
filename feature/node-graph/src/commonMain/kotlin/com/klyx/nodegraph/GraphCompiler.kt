package com.klyx.nodegraph

import kotlin.uuid.Uuid

internal data class CompiledPin(
    val slot: Int,
    val label: String,
    val declaredType: PinType, // what the pin itself declares (may be Wildcard)
    val resolvedType: PinType, // effective type. never Wildcard
    val pinId: Uuid,
    val sourceType: PinType? = null // type of the connected output pin (null if unconnected)
)

internal class CompiledNode(
    val id: Uuid,
    val title: String,
    val isReroute: Boolean,
    val isAutoEntry: Boolean, // true = no flow input, fires on execute()
    val triggerName: String?, // non-null = fires on trigger(name)
    val dataInputs: Array<CompiledPin>,
    val dataOutputs: Array<CompiledPin>,
    val flowOutputs: Array<CompiledPin>,
    val inputSources: IntArray,
    val inputLabelIndex: Map<String, Int>, // label -> values array index (for InputScope)
    val outputLabelIndex: Map<String, Int>, // label -> values array index (for OutputScope)
    val flowNextNodes: Array<IntArray>,
    val compiledEval: ((EvaluateScope) -> Unit)?,
    val compiledFlow: (suspend (FlowScope) -> Unit)?,
)

internal class CompiledGraph(
    val nodes: Array<CompiledNode>,
    val defaultValues: Array<Any?>,
    val autoEntries: IntArray, // node indices with no flow input + null triggerName
    val variables: Map<Uuid, GraphVariable>,
    val variableMemory: MutableMap<Uuid, Any?>,
)

internal object GraphCompiler {

    fun compile(graph: GraphState): CompiledGraph {
        val nodeList = graph.nodes.toList()
        val connList = graph.connections.toList()
        val pinVals = graph.pinValues.toMap()

        val variablesById = graph.variables.associateBy { it.id }
        val variableMemory = graph.variableValues

        var slot = 0
        val pinSlot = HashMap<Uuid, Int>(nodeList.sumOf { it.pins.size } * 2)
        for (n in nodeList) for (p in n.pins) pinSlot[p.id] = slot++

        val values = buildDefaultValues(nodeList, pinSlot, pinVals, slot)
        val connByInput = buildConnByInput(connList)
        val connsByOutput = buildConnsByOutput(connList)
        val (pinOwner, allPinsByUuid) = buildPinMaps(nodeList)

        val compiled = Array(nodeList.size) { i ->
            val node = nodeList[i]
            val def = graph.registry.findNodeByKey(node.definitionKey)

            val resolveType = { pinId: Uuid -> graph.resolveType(pinId) }

            compileNode(
                node = node, def = def, slot = slot, pinSlot = pinSlot, values = values,
                connByInput = connByInput, connsByOutput = connsByOutput,
                pinOwner = pinOwner, allPinsByUuid = allPinsByUuid,
                variablesById = variablesById, variableMemory = variableMemory,
                resolveType = resolveType
            )
        }

        val autoEntries = compiled.indices.filter { compiled[it].isAutoEntry }.toIntArray()
        return CompiledGraph(compiled, values, autoEntries, variablesById, variableMemory)
    }

    fun compile(snapshot: GraphSnapshot, registry: NodeRegistry): CompiledGraph {
        val nodeList = snapshot.nodes
        val connList = snapshot.connections
        val pinVals = snapshot.pinValues.associate { it.pinId to it.value }

        val variablesById = snapshot.variables.associateBy { it.id }
        val variableMemory = HashMap<Uuid, Any?>()
        for (v in snapshot.variables) {
            variableMemory[v.id] = parseVariableDefault(v.defaultValue, v.type)
        }

        var slot = 0
        val pinSlot = HashMap<Uuid, Int>(nodeList.sumOf { it.pins.size } * 2)
        for (n in nodeList) for (p in n.pins) pinSlot[p.id] = slot++

        val values = buildDefaultValues(nodeList, pinSlot, pinVals, slot)
        val connByInput = buildConnByInput(connList)
        val connsByOutput = buildConnsByOutput(connList)
        val (pinOwner, allPinsByUuid) = buildPinMaps(nodeList)

        val resolveType = { pinId: Uuid ->
            val pin = allPinsByUuid[pinId]
            if (pin == null) PinType.Wildcard()
            else if (pin.type !is PinType.Wildcard) pin.type
            else {
                val srcPinId = connByInput[pinId]
                if (srcPinId != null) {
                    val srcPin = allPinsByUuid[srcPinId]
                    if (srcPin != null && srcPin.type !is PinType.Wildcard) srcPin.type else pin.type
                } else pin.type
            }
        }

        val compiled = Array(nodeList.size) { i ->
            val node = nodeList[i]
            val def = registry.findNodeByKey(node.definitionKey)

            compileNode(
                node = node, def = def, slot = slot, pinSlot = pinSlot, values = values,
                connByInput = connByInput, connsByOutput = connsByOutput,
                pinOwner = pinOwner, allPinsByUuid = allPinsByUuid,
                variablesById = variablesById, variableMemory = variableMemory,
                resolveType = resolveType
            )
        }

        val autoEntries = compiled.indices.filter { compiled[it].isAutoEntry }.toIntArray()
        return CompiledGraph(compiled, values, autoEntries, variablesById, variableMemory)
    }

    private fun compileNode(
        node: NodeData,
        def: Node?,
        slot: Int,
        pinSlot: Map<Uuid, Int>,
        values: Array<Any?>,
        connByInput: Map<Uuid, Uuid>,
        connsByOutput: Map<Uuid, List<Uuid>>,
        pinOwner: Map<Uuid, Int>,
        allPinsByUuid: Map<Uuid, NodePin>,
        variablesById: Map<Uuid, GraphVariable>,
        variableMemory: MutableMap<Uuid, Any?>,
        resolveType: (Uuid) -> PinType
    ): CompiledNode {
        val dataIn = node.pins.filter { it.type != PinType.Flow && it.direction == PinDirection.Input }.map { pin ->
            val srcPinId = connByInput[pin.id]
            val srcOutPin = srcPinId?.let { allPinsByUuid[it] }
            CompiledPin(
                slot = pinSlot[pin.id]!!,
                label = pin.label,
                declaredType = pin.type,
                resolvedType = resolveType(pin.id),
                pinId = pin.id,
                sourceType = srcOutPin?.let { resolveType(it.id) }
            )
        }.toTypedArray()

        val dataOut = node.pins.filter { it.type != PinType.Flow && it.direction == PinDirection.Output }.map { pin ->
            CompiledPin(pinSlot[pin.id]!!, pin.label, pin.type, resolveType(pin.id), pin.id)
        }.toTypedArray()

        val flowOut = node.pins.filter { it.type == PinType.Flow && it.direction == PinDirection.Output }.map { pin ->
            CompiledPin(pinSlot[pin.id]!!, pin.label, pin.type, resolveType(pin.id), pin.id)
        }.toTypedArray()

        val flowIn = node.pins.filter { it.type == PinType.Flow && it.direction == PinDirection.Input }

        val sources = IntArray(dataIn.size) { j ->
            val up = connByInput[dataIn[j].pinId]
            if (up != null) pinSlot[up]!! else -1
        }

        val flowNext = Array(flowOut.size) { j ->
            (connsByOutput[flowOut[j].pinId] ?: emptyList()).mapNotNull { pinOwner[it] }.toIntArray()
        }

        val isReroute = node.kind == NodeKind.Reroute
        val trigName = (def as? EventNode)?.triggerName ?: ""
        val isAutoEntry = flowIn.isEmpty() && flowOut.isNotEmpty() && trigName.isEmpty()

        val isGetVar = isGetVariableKey(node.definitionKey)
        val isSetVar = isSetVariableKey(node.definitionKey)
        val varId = if (isGetVar || isSetVar) variableIdFromKey(node.definitionKey) else null
        val variable = varId?.let { id -> variablesById[id] }

        val compiledEval: ((EvaluateScope) -> Unit)? = when {
            isReroute -> { _ ->
                val inSlot = sources.firstOrNull() ?: -1
                val outSlot = dataOut.firstOrNull()?.slot ?: -1
                if (inSlot >= 0 && outSlot >= 0) values[outSlot] = values.getOrNull(inSlot)
            }

            (isGetVar || isSetVar) && variable != null -> { scope ->
                if (isSetVar) variableMemory[variable.id] = scope.inputs.any(variable.name)
                scope.outputs[variable.name] =
                    variableMemory[variable.id] ?: parseVariableDefault(variable.defaultValue, variable.type)
            }

            def != null -> { scope -> with(def) { scope.evaluate() } }
            else -> null
        }

        val compiledFlow: (suspend (FlowScope) -> Unit)? = when {
            isSetVar && variable != null -> { scope ->
                variableMemory[variable.id] = scope.inputs.any(variable.name)
                for (pin in flowOut) scope.trigger(pin.label)
            }

            def?.isFlowNode == true -> { scope -> with(def) { scope.execute() } }
            else -> null
        }

        return CompiledNode(
            node.id, node.title, isReroute, isAutoEntry, trigName, dataIn, dataOut, flowOut, sources,
            dataIn.associate { it.label to it.slot }, dataOut.associate { it.label to it.slot },
            flowNext, compiledEval, compiledFlow
        )
    }

    private fun buildDefaultValues(
        nodeList: List<NodeData>,
        pinSlot: Map<Uuid, Int>,
        pinVals: Map<Uuid, String>,
        slotCount: Int
    ): Array<Any?> {
        val values = arrayOfNulls<Any>(slotCount)
        for (n in nodeList) {
            for (p in n.pins) {
                val s = pinSlot[p.id]!!
                val raw = pinVals[p.id] ?: p.defaultValue
                values[s] = if (raw != null) {
                    when (p.type) {
                        PinType.Float -> raw.toFloatOrNull() ?: 0f
                        PinType.Integer -> raw.toIntOrNull() ?: 0
                        PinType.Boolean -> raw.trim().lowercase() == "true"
                        is PinType.String -> raw
                        is PinType.Enum -> raw
                        PinType.Flow -> null
                        is PinType.Wildcard -> raw
                        is PinType.Custom -> raw
                    }
                } else {
                    when (p.type) {
                        PinType.Float -> 0f
                        PinType.Integer -> 0
                        PinType.Boolean -> false
                        is PinType.String -> null
                        is PinType.Enum -> p.type.entries.firstOrNull()
                        PinType.Flow -> null
                        is PinType.Wildcard -> null
                        is PinType.Custom -> null
                    }
                }
            }
        }
        return values
    }

    private fun buildConnByInput(connList: List<NodeConnection>) =
        connList.associate { it.inputPinId to it.outputPinId }

    private fun buildConnsByOutput(connList: List<NodeConnection>): Map<Uuid, List<Uuid>> {
        val map = HashMap<Uuid, MutableList<Uuid>>()
        for (c in connList) map.getOrPut(c.outputPinId) { mutableListOf() }.add(c.inputPinId)
        return map
    }

    private fun buildPinMaps(nodeList: List<NodeData>): Pair<Map<Uuid, Int>, Map<Uuid, NodePin>> {
        val pinOwner = HashMap<Uuid, Int>()
        val allPinsByUuid = HashMap<Uuid, NodePin>()
        for ((i, n) in nodeList.withIndex()) {
            for (p in n.pins) {
                pinOwner[p.id] = i
                allPinsByUuid[p.id] = p
            }
        }
        return pinOwner to allPinsByUuid
    }
}
