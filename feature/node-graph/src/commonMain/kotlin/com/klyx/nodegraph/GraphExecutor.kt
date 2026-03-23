package com.klyx.nodegraph

import com.klyx.nodegraph.extension.builtin.LoopControlException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.uuid.Uuid

internal suspend fun CompiledGraph.execute(listener: GraphExecutionListener? = null) {
    GraphExecutor(this).run(
        onStep = { id -> listener?.onNodeEnter(id) },
        onLog = { msg -> listener?.onLog(msg) },
        onComplete = { listener?.onComplete() },
        onError = { msg -> listener?.onError(msg) },
    )
}

internal suspend fun CompiledGraph.trigger(
    name: String,
    inputs: Map<String, Any?> = emptyMap(),
    listener: GraphExecutionListener? = null,
) {
    for (node in nodes) {
        if (node.triggerName == name) {
            for ((inputLabel, inputValue) in inputs) {
                val outPin = node.dataOutputs.firstOrNull { it.label == inputLabel }
                if (outPin != null) {
                    defaultValues[outPin.slot] = inputValue
                }
            }
        }
    }

    GraphExecutor(this, triggerName = name).run(
        onStep = { id -> listener?.onNodeEnter(id) },
        onLog = { msg -> listener?.onLog(msg) },
        onComplete = { listener?.onComplete() },
        onError = { msg -> listener?.onError(msg) },
    )
}

internal class GraphExecutor(
    private val plan: CompiledGraph,
    private val triggerName: String? = null,
    val stepDelayMs: Long = 0L,
) {
    // each executor gets its own values copy. parallel triggers don't corrupt each other
    private val values = plan.defaultValues.copyOf()
    private val evaluated = BooleanArray(values.size)

    private val entryIndices: IntArray = when (triggerName) {
        null -> plan.autoEntries
        else -> plan.nodes.indices
            .filter { plan.nodes[it].triggerName == triggerName }
            .toIntArray()
    }

    private fun buildInputLabelIndex(node: CompiledNode, onLog: (String) -> Unit): Map<String, Int> {
        val result = HashMap<String, Int>(node.dataInputs.size * 2)
        for (j in node.dataInputs.indices) {
            val pin = node.dataInputs[j]
            val src = node.inputSources[j]

            if (src >= 0) {
                ensureSlot(src, onLog)
                val srcType = pin.sourceType ?: pin.resolvedType
                val dstType = pin.declaredType.takeIf { it != PinType.Wildcard } ?: pin.resolvedType

                if (srcType != dstType && PinType.canAutoCast(srcType, dstType)) {
                    values[pin.slot] = PinType.applyCast(values[src], srcType, dstType)
                    evaluated[pin.slot] = true
                    result[pin.label] = pin.slot
                } else {
                    result[pin.label] = src
                }
            } else {
                result[pin.label] = pin.slot
            }
        }
        return result
    }

    private inner class EvaluateScopeImpl(
        node: CompiledNode,
        private val onLog: (String) -> Unit
    ) : EvaluateScope {
        override val inputs = Inputs(values, buildInputLabelIndex(node, onLog))
        override val outputs = Outputs(values, node.outputLabelIndex, evaluated)

        override fun log(message: String) {
            onLog(message)
        }
    }

    private fun evaluateNode(node: CompiledNode, onLog: (String) -> Unit) {
        val proxy = node.dataOutputs.firstOrNull()?.slot ?: return
        if (evaluated[proxy]) return

        val eval = node.compiledEval ?: kotlin.run {
            for (o in node.dataOutputs) evaluated[o.slot] = true
            return
        }

        val scope = EvaluateScopeImpl(node, onLog)
        eval(scope)
    }

    private fun ensureSlot(slot: Int, onLog: (String) -> Unit) {
        if (evaluated[slot]) return
        for (n in plan.nodes) {
            for (o in n.dataOutputs) {
                if (o.slot == slot) {
                    evaluateNode(n, onLog)
                    return
                }
            }
        }
    }

    private inner class FlowScopeImpl(
        private val node: CompiledNode,
        private val onStep: suspend (Uuid) -> Unit,
        private val onLog: (String) -> Unit
    ) : FlowScope {

        override val inputs = Inputs(values, buildInputLabelIndex(node, onLog))

        override suspend fun trigger(flowPinLabel: String) {
            val outIdx = node.flowOutputs.indexOfFirst { it.label == flowPinLabel }
            if (outIdx < 0) return // flow pin not found or has no connections

            val nextList = node.flowNextNodes.getOrNull(outIdx)?.toList() ?: emptyList()
            if (nextList.isEmpty()) return

            // recursively call flow() for the next nodes.
            // if there's only one connection, run it sequentially.
            // if multiple, run them in parallel.
            if (nextList.size == 1) {
                flow(nextList[0], onStep, onLog)
            } else {
                coroutineScope {
                    nextList.map { ni ->
                        async {
                            flow(ni, onStep, onLog)
                        }
                    }.awaitAll()
                }
            }
        }

        override fun updateOutput(label: String, value: Any?) {
            val slotIdx = node.outputLabelIndex[label] ?: error("Output pin '$label' not found.")
            values[slotIdx] = value
            evaluated[slotIdx] = true
        }

        override fun resetEvaluationCache() {
            // clearing the cache forces pure nodes to re-evaluate on the next read.
            evaluated.fill(false)
        }

        override fun log(message: String) {
            onLog(message)
        }
    }

    private suspend fun flow(
        idx: Int,
        onStep: suspend (Uuid) -> Unit,
        onLog: (String) -> Unit,
    ) {
        currentCoroutineContext().ensureActive()

        val node = plan.nodes[idx]
        onStep(node.id)
        if (stepDelayMs > 0L) delay(stepDelayMs)

        evaluateNode(node, onLog)

        // if it's a flow node, let it handle its own execution routing
        if (node.compiledFlow != null) {
            val flowScope = FlowScopeImpl(node, onStep, onLog)
            node.compiledFlow.invoke(flowScope)
        }
    }

    suspend fun run(
        onStep: suspend (Uuid) -> Unit = {},
        onLog: (String) -> Unit = {},
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {},
    ) {
        evaluated.fill(false)

        if (entryIndices.isEmpty()) {
            onError(
                if (triggerName != null) "No node found for trigger: '$triggerName'"
                else "No entry node found."
            )
            return
        }

        try {
            coroutineScope {
                entryIndices.map { idx ->
                    async {
                        try {
                            flow(idx, onStep, onLog)
                        } catch (_: LoopControlException) {
                            onLog("Warning: A Break or Continue node was triggered outside of a loop. Execution of this branch was stopped.")
                        }
                    }
                }.awaitAll()
            }
        } catch (e: Exception) {
            if (e !is CancellationException) {
                onError(e.message ?: "Unknown execution error")
            } else {
                throw e
            }
        }

        onComplete()
    }
}
