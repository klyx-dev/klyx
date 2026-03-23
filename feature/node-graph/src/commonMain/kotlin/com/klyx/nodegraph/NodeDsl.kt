package com.klyx.nodegraph

import androidx.compose.ui.graphics.Color
import com.klyx.nodegraph.core.StandardNodeColors

@NodeDslMarker
class NodeBuilder @PublishedApi internal constructor(
    private val key: String,
    private val title: String,
    private val category: String,
    private val description: String,
    private val headerColor: Color,
    private val compactTitle: String?,
    private val triggerName: String? = null,
) {
    private val pins = mutableListOf<Pin>()

    private var evaluateFn: (EvaluateScope.() -> Unit)? = null
    private var executeFn: (suspend FlowScope.() -> Unit)? = null
    private var inferOutputTypesFn: ((Map<String, PinType>) -> Map<String, PinType>)? = null

    /** Adds a data input pin. */
    fun input(label: String, type: PinType) {
        pins.add(Pin(label, type, PinDirection.Input))
    }

    /** Adds a data output pin. */
    fun output(label: String, type: PinType) {
        pins.add(Pin(label, type, PinDirection.Output))
    }

    /** Adds a Flow input pin (for action/event nodes). */
    fun flowInput(label: String = "Exec", showInHeaderIfPossible: Boolean = false) {
        pins.add(Pin(label, PinType.Flow, PinDirection.Input, showInHeaderIfPossible))
    }

    /** Adds a Flow output pin. */
    fun flowOutput(label: String = "Then", showInHeaderIfPossible: Boolean = false) {
        pins.add(Pin(label, PinType.Flow, PinDirection.Output, showInHeaderIfPossible))
    }

    /** Adds a Wildcard input pin. Optionally restrict to specific types. */
    fun wildcardInput(label: String, vararg allowedTypes: PinType) {
        pins.add(Pin(label, PinType.Wildcard(allowedTypes.toList()), PinDirection.Input))
    }

    /** Adds a Wildcard output pin. Optionally restrict to specific types. */
    fun wildcardOutput(label: String, vararg allowedTypes: PinType) {
        pins.add(Pin(label, PinType.Wildcard(allowedTypes.toList()), PinDirection.Output))
    }

    /**
     * Defines the pure data computation.
     *
     * Read inputs: `inputs.float("A")`, `inputs.string("Name")` etc.
     * Write outputs: `outputs["Result"] = value`
     */
    fun evaluate(block: EvaluateScope.() -> Unit) {
        evaluateFn = block
    }

    /**
     * Defines the flow side-effect.
     * Use `trigger("PinName")` to continue flow.
     * Use `log("Message")` to print to the executor's log.
     */
    fun execute(block: suspend FlowScope.() -> Unit) {
        executeFn = block
    }

    /**
     * Defines the flow side-effect, and automatically triggers the first Flow
     * output pin (if one exists) after the block completes.
     */
    fun executeSequential(block: suspend FlowScope.() -> Unit = {}) {
        executeFn = {
            block()

            val firstFlowOut = pins.firstOrNull {
                it.type == PinType.Flow && it.direction == PinDirection.Output
            }
            if (firstFlowOut != null) {
                trigger(firstFlowOut.label)
            }
        }
    }

    /**
     * Defines static output type inference for Wildcard pins.
     * Called when input connections change. return resolved output types.
     *
     * ```kotlin
     * inferOutputTypes { inputTypes ->
     *     mapOf("Result" to (inputTypes["A"] ?: PinType.Wildcard))
     * }
     * ```
     */
    fun inferOutputTypes(block: (inputTypes: Map<String, PinType>) -> Map<String, PinType>) {
        inferOutputTypesFn = block
    }

    @PublishedApi
    internal fun buildPureNode(): Node {
        val capturedPins = pins.toList()
        val capturedEval = evaluateFn
        val capturedInfer = inferOutputTypesFn
        val capturedKey = key
        val capturedTitle = title
        val capturedCategory = category
        val capturedDescription = description
        val capturedHeaderColor = headerColor
        val capturedCompactTitle = compactTitle

        return object : PureNode() {
            override val key = capturedKey
            override val title = capturedTitle
            override val category = capturedCategory
            override val description = capturedDescription
            override val headerColor = capturedHeaderColor
            override val compactTitle = capturedCompactTitle
            override val pins = capturedPins

            override fun EvaluateScope.evaluate() {
                capturedEval?.invoke(this)
            }

            override fun inferOutputTypes(inputTypes: Map<String, PinType>) =
                capturedInfer?.invoke(inputTypes) ?: super.inferOutputTypes(inputTypes)
        }
    }

    @PublishedApi
    internal fun buildActionNode(): Node {
        val capturedPins = pins.toList()
        val capturedEval = evaluateFn
        val capturedExec = executeFn
        val capturedInfer = inferOutputTypesFn
        val capturedKey = key
        val capturedTitle = title
        val capturedCategory = category
        val capturedDescription = description
        val capturedHeaderColor = headerColor
        val capturedCompactTitle = compactTitle

        return object : ActionNode() {
            override val key = capturedKey
            override val title = capturedTitle
            override val category = capturedCategory
            override val description = capturedDescription
            override val headerColor = capturedHeaderColor
            override val compactTitle = capturedCompactTitle
            override val pins = capturedPins

            override fun EvaluateScope.evaluate() {
                capturedEval?.invoke(this)
            }

            override suspend fun FlowScope.execute() {
                capturedExec?.invoke(this)
            }

            override fun inferOutputTypes(inputTypes: Map<String, PinType>) =
                capturedInfer?.invoke(inputTypes) ?: super.inferOutputTypes(inputTypes)
        }
    }

    @PublishedApi
    internal fun buildEventNode(): Node {
        val capturedPins = pins.toList()
        val capturedEval = evaluateFn
        val capturedExec = executeFn
        val capturedInfer = inferOutputTypesFn
        val capturedKey = key
        val capturedTitle = title
        val capturedCategory = category
        val capturedDescription = description
        val capturedHeaderColor = headerColor
        val capturedCompactTitle = compactTitle
        val capturedTrigger = triggerName

        return object : EventNode() {
            override val key = capturedKey
            override val title = capturedTitle
            override val category = capturedCategory
            override val description = capturedDescription
            override val headerColor = capturedHeaderColor
            override val compactTitle = capturedCompactTitle
            override val triggerName = capturedTrigger
            override val pins = capturedPins

            override fun EvaluateScope.evaluate() {
                capturedEval?.invoke(this)
            }

            override suspend fun FlowScope.execute() {
                capturedExec?.invoke(this)
            }

            override fun inferOutputTypes(inputTypes: Map<String, PinType>) =
                capturedInfer?.invoke(inputTypes) ?: super.inferOutputTypes(inputTypes)
        }
    }
}

@DslMarker
annotation class NodeDslMarker

/**
 * Creates a pure node (no Flow pins, purely functional computation).
 *
 * ```kotlin
 * val sqrtNode = pureNode(
 *     key = "my.sqrt",
 *     title = "Square Root",
 * ) {
 *     input("Value", PinType.Float)
 *     output("Result", PinType.Float)
 *
 *     evaluate {
 *         outputs["Result"] = kotlin.math.sqrt(inputs.float("Value"))
 *     }
 * }
 * ```
 */
inline fun pureNode(
    key: String,
    title: String,
    category: String = "Custom",
    description: String = "",
    headerColor: Color = StandardNodeColors.Headers.Pure,
    compactTitle: String? = null,
    block: NodeBuilder.() -> Unit,
): Node = NodeBuilder(key, title, category, description, headerColor, compactTitle)
    .apply(block)
    .buildPureNode()

/**
 * Creates an action node (has Flow input + output, produces side effects).
 *
 * ```kotlin
 * val logNode = actionNode(
 *     key = "my.log",
 *     title = "Log Message",
 * ) {
 *     flowInput("Exec")
 *     input("Message", PinType.String)
 *     flowOutput("Then")
 *
 *     execute {
 *         log(inputs.string("Message"))
 *         trigger("Then")
 *     }
 * }
 * ```
 */
inline fun actionNode(
    key: String,
    title: String,
    category: String = "Custom",
    description: String = "",
    headerColor: Color = StandardNodeColors.Headers.Action,
    compactTitle: String? = null,
    block: NodeBuilder.() -> Unit,
): Node = NodeBuilder(key, title, category, description, headerColor, compactTitle)
    .apply(block)
    .buildActionNode()

/**
 * Creates an event node (entry point, fired by [GraphState.trigger]).
 *
 * ```kotlin
 * val onLoginNode = eventNode(
 *     key = "my.on_login",
 *     title = "On User Login",
 *     triggerName = "onLogin",
 * ) {
 *     flowOutput("Exec")
 *     output("Username", PinType.String)
 *     output("UserId", PinType.Integer)
 *
 *     execute {
 *         log("User logged in")
 *         trigger("Exec")
 *     }
 * }
 * ```
 */
inline fun eventNode(
    key: String,
    title: String,
    triggerName: String? = null,
    category: String = "Events",
    description: String = "",
    headerColor: Color = StandardNodeColors.Headers.Event,
    compactTitle: String? = null,
    block: NodeBuilder.() -> Unit,
): Node = NodeBuilder(key, title, category, description, headerColor, compactTitle, triggerName)
    .apply(block)
    .buildEventNode()

/**
 * Inline pure node registration inside [nodeGraph] DSL.
 *
 * ```kotlin
 * nodeGraph {
 *     pureNode(key = "my.sqrt", title = "Square Root") {
 *         input("Value", PinType.Float)
 *         output("Result", PinType.Float)
 *         evaluate {
 *             outputs["Result"] = kotlin.math.sqrt(inputs.float("Value"))
 *         }
 *     }
 * }
 * ```
 */
inline fun NodeGraphBuilder.pureNode(
    key: String,
    title: String,
    category: String = "Custom",
    description: String = "",
    headerColor: Color = StandardNodeColors.Headers.Pure,
    compactTitle: String? = null,
    block: NodeBuilder.() -> Unit,
) = node(com.klyx.nodegraph.pureNode(key, title, category, description, headerColor, compactTitle, block))

fun NodeGraphBuilder.actionNode(
    key: String,
    title: String,
    category: String = "Custom",
    description: String = "",
    headerColor: Color = StandardNodeColors.Headers.Action,
    compactTitle: String? = null,
    block: NodeBuilder.() -> Unit,
) = node(com.klyx.nodegraph.actionNode(key, title, category, description, headerColor, compactTitle, block))

fun NodeGraphBuilder.eventNode(
    key: String,
    title: String,
    triggerName: String = "",
    category: String = "Events",
    description: String = "",
    headerColor: Color = StandardNodeColors.Headers.Event,
    compactTitle: String? = null,
    block: NodeBuilder.() -> Unit,
) = node(com.klyx.nodegraph.eventNode(key, title, triggerName, category, description, headerColor, compactTitle, block))
