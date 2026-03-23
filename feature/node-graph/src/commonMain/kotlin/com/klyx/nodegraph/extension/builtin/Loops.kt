package com.klyx.nodegraph.extension.builtin

import androidx.compose.ui.graphics.Color
import com.klyx.nodegraph.ActionNode
import com.klyx.nodegraph.FlowScope
import com.klyx.nodegraph.InputHeaderPin
import com.klyx.nodegraph.InputPin
import com.klyx.nodegraph.NodeRegistry
import com.klyx.nodegraph.OutputPin
import com.klyx.nodegraph.PinType
import com.klyx.nodegraph.core.StandardNodeColors
import com.klyx.nodegraph.extension.GraphExtension
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.yield


internal sealed class LoopControlException : CancellationException(null)

internal class BreakSignal : LoopControlException()
internal class ContinueSignal : LoopControlException()

private const val CATEGORY = "Flow Control"

private object BreakNode : ActionNode() {
    override val key = "builtin.loop.break"
    override val title = "Break"
    override val category = CATEGORY
    override val pins = listOf(InputHeaderPin("Exec"))
    override val headerColor = StandardNodeColors.Headers.ControlFlow

    override suspend fun FlowScope.execute() {
        throw BreakSignal()
    }
}

private object ContinueNode : ActionNode() {
    override val key = "builtin.loop.continue"
    override val title = "Continue"
    override val category = CATEGORY
    override val pins = listOf(InputHeaderPin("Exec"))
    override val headerColor = StandardNodeColors.Headers.ControlFlow

    override suspend fun FlowScope.execute() {
        throw ContinueSignal()
    }
}

private object ForLoopNode : ActionNode() {
    override val key = "builtin.for_loop"
    override val title = "For Loop"
    override val category = "Flow Control"
    override val description = "Fires a loop body for each index between First and Last."
    override val headerColor = StandardNodeColors.Headers.ControlFlow

    override val pins = listOf(
        InputHeaderPin("Exec"),
        InputPin("First Index", PinType.Integer),
        InputPin("Last Index", PinType.Integer),
        OutputPin("Loop Body", PinType.Flow),
        OutputPin("Index", PinType.Integer),
        OutputPin("Completed", PinType.Flow)
    )

    override suspend fun FlowScope.execute() {
        val first = inputs.int("First Index")
        val last = inputs.int("Last Index")

        for (i in first..last) {
            updateOutput("Index", i)
            resetEvaluationCache()

            try {
                trigger("Loop Body")
            } catch (_: ContinueSignal) {
                continue
            } catch (_: BreakSignal) {
                break
            }
        }

        trigger("Completed")
    }
}

private object WhileLoopNode : ActionNode() {
    override val key = "builtin_while_loop"
    override val title = "While Loop"
    override val category = "Flow Control"
    override val description = "Loops as long as the Condition remains True."
    override val headerColor = StandardNodeColors.Headers.ControlFlow

    override val pins = listOf(
        InputHeaderPin("Exec"),
        InputPin("Condition", PinType.Boolean),
        OutputPin("Loop Body", PinType.Flow),
        OutputPin("Completed", PinType.Flow)
    )

    override suspend fun FlowScope.execute() {
        while (true) {
            resetEvaluationCache()

            if (inputs.boolean("Condition")) {
                try {
                    trigger("Loop Body")
                    yield()
                } catch (_: ContinueSignal) {
                    continue
                } catch (_: BreakSignal) {
                    break
                }
            } else {
                break
            }
        }

        trigger("Completed")
    }
}

internal object Loops : GraphExtension {
    override val name = "Loops"

    override fun install(registry: NodeRegistry) {
        registry.register(BreakNode, ContinueNode, ForLoopNode, WhileLoopNode)
    }
}
