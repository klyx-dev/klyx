package com.klyx.nodegraph

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.klyx.nodegraph.extension.builtin.BuiltinExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.uuid.Uuid

class HeadlessGraphTest : FunSpec({

    test("resolveType traces connection to resolve List<Wildcard> to List<Float>.") {
        val registry = NodeRegistry { install(BuiltinExtension) }

        val listGetId = Uuid.random()
        val sourceId = Uuid.random()
        val sourceOutId = Uuid.random()
        val listGetListInId = Uuid.random()
        val listGetIdxInId = Uuid.random()
        val listGetElemOutId = Uuid.random()

        val nodes = listOf(
            NodeData(
                id = sourceId, title = "Source", definitionKey = "builtin.create_list",
                kind = NodeKind.Custom, headerColor = Color.Gray,
                pins = listOf(
                    NodePin(sourceOutId, "List", PinType.List(PinType.Float), PinDirection.Output, sourceId)
                ),
                position = Offset.Zero
            ),
            NodeData(
                id = listGetId, title = "List Get", definitionKey = "builtin.list_get",
                kind = NodeKind.Custom, headerColor = Color.Gray,
                pins = listOf(
                    NodePin(listGetListInId, "List", PinType.List(PinType.Wildcard()), PinDirection.Input, listGetId),
                    NodePin(listGetIdxInId, "Index", PinType.Integer, PinDirection.Input, listGetId),
                    NodePin(listGetElemOutId, "Element", PinType.Wildcard(), PinDirection.Output, listGetId),
                ),
                position = Offset.Zero
            ),
        )

        val connections = listOf(
            NodeConnection(Uuid.random(), sourceOutId, listGetListInId, false),
        )

        val snapshot = GraphSnapshot(nodes = nodes, connections = connections)
        val compiled = GraphCompiler.compile(snapshot, registry)

        val listGetNode = compiled.nodes.find { it.title == "List Get" }!!
        val elementOutput = listGetNode.dataOutputs.find { it.label == "Element" }!!
        elementOutput.resolvedType shouldBe PinType.Float
    }

    test("ForEach infers Element type from List<Float>.") {
        val registry = NodeRegistry { install(BuiltinExtension) }

        val forEachId = Uuid.random()
        val sourceId = Uuid.random()
        val sourceOutId = Uuid.random()
        val forEachListInId = Uuid.random()
        val forEachElemOutId = Uuid.random()

        val nodes = listOf(
            NodeData(
                id = sourceId, title = "Source", definitionKey = "builtin.create_list",
                kind = NodeKind.Custom, headerColor = Color.Gray,
                pins = listOf(
                    NodePin(sourceOutId, "List", PinType.List(PinType.Float), PinDirection.Output, sourceId)
                ),
                position = Offset.Zero
            ),
            NodeData(
                id = forEachId, title = "For Each", definitionKey = "builtin.for_each",
                kind = NodeKind.Custom, headerColor = Color.Gray,
                pins = listOf(
                    NodePin(Uuid.random(), "Exec", PinType.Flow, PinDirection.Input, forEachId, true),
                    NodePin(forEachListInId, "List", PinType.List(PinType.Wildcard()), PinDirection.Input, forEachId),
                    NodePin(Uuid.random(), "Loop Body", PinType.Flow, PinDirection.Output, forEachId),
                    NodePin(forEachElemOutId, "Element", PinType.Wildcard(), PinDirection.Output, forEachId),
                    NodePin(Uuid.random(), "Index", PinType.Integer, PinDirection.Output, forEachId),
                    NodePin(Uuid.random(), "Completed", PinType.Flow, PinDirection.Output, forEachId),
                ),
                position = Offset.Zero
            ),
        )

        val connections = listOf(
            NodeConnection(Uuid.random(), sourceOutId, forEachListInId, false),
        )

        val snapshot = GraphSnapshot(nodes = nodes, connections = connections)
        val compiled = GraphCompiler.compile(snapshot, registry)

        val forEachNode = compiled.nodes.find { it.title == "For Each" }!!
        val elementOutput = forEachNode.dataOutputs.find { it.label == "Element" }!!
        elementOutput.resolvedType shouldBe PinType.Float
    }

    test("ListAppend infers both input and output List element types.") {
        val registry = NodeRegistry { install(BuiltinExtension) }

        val listAppendId = Uuid.random()
        val sourceId = Uuid.random()
        val sourceOutId = Uuid.random()
        val appendListInId = Uuid.random()

        val nodes = listOf(
            NodeData(
                id = sourceId, title = "Source", definitionKey = "builtin.create_list",
                kind = NodeKind.Custom, headerColor = Color.Gray,
                pins = listOf(
                    NodePin(sourceOutId, "List", PinType.List(PinType.String()), PinDirection.Output, sourceId)
                ),
                position = Offset.Zero
            ),
            NodeData(
                id = listAppendId, title = "List Append", definitionKey = "builtin.list_append",
                kind = NodeKind.Custom, headerColor = Color.Gray,
                pins = listOf(
                    NodePin(Uuid.random(), "Exec", PinType.Flow, PinDirection.Input, listAppendId, true),
                    NodePin(appendListInId, "List", PinType.List(PinType.Wildcard()), PinDirection.Input, listAppendId),
                    NodePin(Uuid.random(), "Value", PinType.Wildcard(), PinDirection.Input, listAppendId),
                    NodePin(Uuid.random(), "Then", PinType.Flow, PinDirection.Output, listAppendId, true),
                    NodePin(
                        Uuid.random(),
                        "Result",
                        PinType.List(PinType.Wildcard()),
                        PinDirection.Output,
                        listAppendId
                    ),
                ),
                position = Offset.Zero
            ),
        )

        val connections = listOf(
            NodeConnection(Uuid.random(), sourceOutId, appendListInId, false),
        )

        val snapshot = GraphSnapshot(nodes = nodes, connections = connections)
        val compiled = GraphCompiler.compile(snapshot, registry)

        val appendNode = compiled.nodes.find { it.title == "List Append" }!!
        val resultOutput = appendNode.dataOutputs.find { it.label == "Result" }!!
        resultOutput.resolvedType shouldBe PinType.List(PinType.String())
    }

    test("CreateList + ListGet + Print execution returns correct element.") {
        val registry = NodeRegistry(includeDefaultStartNode = true)

        val startId = Uuid.random()
        val printId = Uuid.random()
        val createListId = Uuid.random()
        val listGetId = Uuid.random()
        val const10Id = Uuid.random()
        val const20Id = Uuid.random()
        val const30Id = Uuid.random()
        val const1Id = Uuid.random()

        val startFlowOutId = Uuid.random()
        val printFlowInId = Uuid.random()
        val printAnyInId = Uuid.random()
        val const10InId = Uuid.random()
        val const10OutId = Uuid.random()
        val const20InId = Uuid.random()
        val const20OutId = Uuid.random()
        val const30InId = Uuid.random()
        val const30OutId = Uuid.random()
        val const1InId = Uuid.random()
        val const1OutId = Uuid.random()
        val listOutId = Uuid.random()
        val createListElem1InId = Uuid.random()
        val createListElem2InId = Uuid.random()
        val createListElem3InId = Uuid.random()
        val listGetListInId = Uuid.random()
        val listGetIndexInId = Uuid.random()
        val listGetElemOutId = Uuid.random()

        val nodes = listOf(
            NodeData(
                id = startId, title = "On Start", definitionKey = "builtin.start",
                kind = NodeKind.Custom, headerColor = Color(0xFF44FF88),
                pins = listOf(NodePin(startFlowOutId, "Exec", PinType.Flow, PinDirection.Output, startId, true)),
                position = Offset.Zero
            ),
            NodeData(
                id = const10Id, title = "Float", definitionKey = "builtin.float_constant",
                kind = NodeKind.Custom, headerColor = Color(0xFF88D66C),
                pins = listOf(
                    NodePin(const10InId, "Value", PinType.Float, PinDirection.Input, const10Id),
                    NodePin(const10OutId, "Out", PinType.Float, PinDirection.Output, const10Id),
                ),
                position = Offset.Zero
            ),
            NodeData(
                id = const20Id, title = "Float", definitionKey = "builtin.float_constant",
                kind = NodeKind.Custom, headerColor = Color(0xFF88D66C),
                pins = listOf(
                    NodePin(const20InId, "Value", PinType.Float, PinDirection.Input, const20Id),
                    NodePin(const20OutId, "Out", PinType.Float, PinDirection.Output, const20Id),
                ),
                position = Offset.Zero
            ),
            NodeData(
                id = const30Id, title = "Float", definitionKey = "builtin.float_constant",
                kind = NodeKind.Custom, headerColor = Color(0xFF88D66C),
                pins = listOf(
                    NodePin(const30InId, "Value", PinType.Float, PinDirection.Input, const30Id),
                    NodePin(const30OutId, "Out", PinType.Float, PinDirection.Output, const30Id),
                ),
                position = Offset.Zero
            ),
            NodeData(
                id = const1Id, title = "Integer", definitionKey = "builtin.int_constant",
                kind = NodeKind.Custom, headerColor = Color(0xFF4FC3F7),
                pins = listOf(
                    NodePin(const1InId, "Value", PinType.Integer, PinDirection.Input, const1Id),
                    NodePin(const1OutId, "Out", PinType.Integer, PinDirection.Output, const1Id),
                ),
                position = Offset.Zero
            ),
            NodeData(
                id = createListId, title = "Create List", definitionKey = "builtin.create_list",
                kind = NodeKind.Custom, headerColor = Color(0xFF00695C),
                pins = listOf(
                    NodePin(createListElem1InId, "Element 1", PinType.Wildcard(), PinDirection.Input, createListId),
                    NodePin(createListElem2InId, "Element 2", PinType.Wildcard(), PinDirection.Input, createListId),
                    NodePin(createListElem3InId, "Element 3", PinType.Wildcard(), PinDirection.Input, createListId),
                    NodePin(listOutId, "List", PinType.List(PinType.Wildcard()), PinDirection.Output, createListId),
                ),
                position = Offset.Zero
            ),
            NodeData(
                id = listGetId, title = "List Get", definitionKey = "builtin.list_get",
                kind = NodeKind.Custom, headerColor = Color(0xFF2E7D32),
                pins = listOf(
                    NodePin(listGetListInId, "List", PinType.List(PinType.Wildcard()), PinDirection.Input, listGetId),
                    NodePin(listGetIndexInId, "Index", PinType.Integer, PinDirection.Input, listGetId),
                    NodePin(listGetElemOutId, "Element", PinType.Wildcard(), PinDirection.Output, listGetId),
                ),
                position = Offset.Zero
            ),
            NodeData(
                id = printId, title = "Print", definitionKey = "builtin.print_any",
                kind = NodeKind.Custom, headerColor = Color(0xFF6A1B9A),
                pins = listOf(
                    NodePin(printFlowInId, "Exec", PinType.Flow, PinDirection.Input, printId, true),
                    NodePin(printAnyInId, "Any", PinType.Wildcard(), PinDirection.Input, printId),
                    NodePin(Uuid.random(), "Then", PinType.Flow, PinDirection.Output, printId, true),
                ),
                position = Offset.Zero
            ),
        )

        val connections = listOf(
            NodeConnection(Uuid.random(), startFlowOutId, printFlowInId, false),
            NodeConnection(Uuid.random(), const10OutId, createListElem1InId, false),
            NodeConnection(Uuid.random(), const20OutId, createListElem2InId, false),
            NodeConnection(Uuid.random(), const30OutId, createListElem3InId, false),
            NodeConnection(Uuid.random(), const1OutId, listGetIndexInId, false),
            NodeConnection(Uuid.random(), listOutId, listGetListInId, false),
            NodeConnection(Uuid.random(), listGetElemOutId, printAnyInId, false),
        )

        val pinValueEntries = listOf(
            PinValueEntry(const10InId, "10.0"),
            PinValueEntry(const20InId, "20.0"),
            PinValueEntry(const30InId, "30.0"),
            PinValueEntry(const1InId, "1"),
        )

        val snapshot = GraphSnapshot(nodes = nodes, connections = connections, pinValues = pinValueEntries)
        val compiled = GraphCompiler.compile(snapshot, registry)

        var logMessages = mutableListOf<String>()
        compiled.execute(object : GraphExecutionListener {
            override fun onLog(message: String) {
                logMessages.add(message)
            }
        })

        logMessages shouldBe listOf("20.0")
    }
})
