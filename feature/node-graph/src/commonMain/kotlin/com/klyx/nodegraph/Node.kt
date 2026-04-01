package com.klyx.nodegraph

import androidx.compose.ui.graphics.Color
import com.klyx.nodegraph.core.StandardNodeColors
import kotlin.jvm.JvmName

abstract class Node {
    abstract val key: String
    abstract val title: String
    abstract val category: String
    open val description: String = ""
    abstract val headerColor: Color
    abstract val pins: List<Pin>

    open val compactTitle: String? = null

    /**
     * Pure data computation.
     * Read inputs with typed accessors: inputs.float("A"), inputs.string("Name")
     * Write outputs with: outputs.set("Result", value)
     */
    open fun EvaluateScope.evaluate() {}

    /**
     * Flow side-effect.
     * Return the output Flow pin label to follow, or null to follow all.
     */
    open suspend fun FlowScope.execute() {}

    /**
     * Called when any input pin's resolved type changes.
     * Return a map of `(output pin label -> inferred PinType)`.
     *
     * Default: returns empty map (output types stay as declared).
     *
     * Example: Add node returns same type as A:
     * ```kotlin
     * override fun inferOutputTypes(inputTypes: Map<String, PinType>) =
     *     mapOf("Result" to (inputTypes["A"] ?: PinType.Wildcard))
     * ```
     */
    open fun inferOutputTypes(inputTypes: Map<String, PinType>): Map<String, PinType> = emptyMap()

    /**
     * Whether this node has any side effects during flow execution.
     * Pure nodes (no Flow pins) that only implement [evaluate] should return false.
     * The compiler uses this to skip the flow handler entirely for pure nodes.
     */
    open val isFlowNode get() = pins.any { it.type == PinType.Flow }
}

/** Any node with no Flow pins. pure functional computation. */
abstract class PureNode : Node() {
    final override val isFlowNode = false
    override val headerColor = StandardNodeColors.Headers.Pure

    // evaluate() is the only method that matters for pure nodes
    final override suspend fun FlowScope.execute() {}
}

/** Any node with Flow input + Flow output. action/statement node. */
abstract class ActionNode : Node() {
    final override val isFlowNode = true
    override val headerColor = StandardNodeColors.Headers.Action

    abstract override suspend fun FlowScope.execute()
}

/**
 * A standard Action node that performs an action and immediately triggers
 * its primary Flow output pin (defaults to "Then").
 */
abstract class SequentialActionNode(
    protected val defaultNextLabel: String = "Then"
) : ActionNode() {

    abstract suspend fun FlowScope.performAction()

    final override suspend fun FlowScope.execute() {
        performAction()
        trigger(defaultNextLabel)
    }
}

abstract class EventNode : Node() {
    final override val isFlowNode = true

    override val headerColor = StandardNodeColors.Headers.Event

    /**
     * Unique trigger name.
     * null = fires automatically at graph start.
     */
    open val triggerName: String? = null
}

abstract class SequentialEventNode(protected val defaultNextLabel: String = "Exec") : EventNode() {
    open suspend fun FlowScope.performAction() {}

    final override suspend fun FlowScope.execute() {
        performAction()
        trigger(defaultNextLabel)
    }
}

interface EvaluateScope {
    val inputs: Inputs
    val outputs: Outputs

    fun log(message: String)
}

interface FlowScope {
    val inputs: Inputs

    /** Triggers the next flow node(s) connected to the specified Flow output pin. */
    suspend fun trigger(flowPinLabel: String)

    /** Directly writes a value to an output pin slot and marks it as evaluated. */
    fun updateOutput(label: String, value: Any?)

    /**
     * Clears the evaluation cache.
     * This forces downstream pure nodes to recalculate using the new data.
     */
    fun resetEvaluationCache()

    fun log(message: String)
}

class Inputs internal constructor(
    private val values: Array<Any?>,
    private val labelIndex: Map<String, Int>, // label -> slot index
) {
    fun float(label: String): Float = (values[slot(label)] as? Number)?.toFloat() ?: 0f
    fun int(label: String): Int = (values[slot(label)] as? Number)?.toInt() ?: 0
    fun boolean(label: String): Boolean = values[slot(label)] as? Boolean ?: false
    fun string(label: String): String = values[slot(label)] as? String ?: ""
    fun stringOrNull(label: String): String? = values[slot(label)] as? String
    fun any(label: String): Any? = values[slot(label)]

    @Suppress("NOTHING_TO_INLINE")
    @JvmName("_get")
    inline operator fun <T> get(label: String): T? = custom(label)

    @Suppress("UNCHECKED_CAST")
    fun <T> custom(label: String): T? = values[slot(label)] as? T

    inline fun <reified T : Enum<T>> enum(pinName: String): T {
        return when (val raw = any(pinName)) {
            is T -> raw
            is String -> enumValueOf<T>(raw)
            else -> error("Failed to parse Enum for pin '$pinName'. Found: $raw")
        }
    }

    private fun slot(label: String): Int =
        labelIndex[label] ?: error("Pin '$label' not found. Available: ${labelIndex.keys}")

    override fun toString(): String {
        return labelIndex.entries.joinToString()
    }
}

class Outputs internal constructor(
    private val values: Array<Any?>,
    private val labelIndex: Map<String, Int>,
    private val evaluated: BooleanArray,
) {
    operator fun set(label: String, value: Any?) {
        val idx = labelIndex[label] ?: error("Output pin '$label' not found. Available: ${labelIndex.keys}")
        values[idx] = value
        evaluated[idx] = true
    }

    operator fun get(label: String): Any? {
        val idx = labelIndex[label] ?: error("Output pin '$label' not found.")
        return values[idx]
    }

    fun isEvaluated(label: String): Boolean = evaluated[labelIndex[label] ?: -1]
}
