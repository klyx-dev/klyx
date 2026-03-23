package com.klyx.nodegraph.extension.builtin

import androidx.compose.ui.graphics.Color
import com.klyx.nodegraph.ActionNode
import com.klyx.nodegraph.EvaluateScope
import com.klyx.nodegraph.FlowScope
import com.klyx.nodegraph.InputHeaderPin
import com.klyx.nodegraph.InputPin
import com.klyx.nodegraph.NodeRegistry
import com.klyx.nodegraph.OutputHeaderPin
import com.klyx.nodegraph.OutputPin
import com.klyx.nodegraph.PinType
import com.klyx.nodegraph.PureNode
import com.klyx.nodegraph.SequentialEventNode
import com.klyx.nodegraph.core.StandardNodeColors
import com.klyx.nodegraph.extension.GraphExtension
import kotlinx.coroutines.delay

@PublishedApi
internal object StartNode : SequentialEventNode() {
    override val key = "builtin.start"
    override val title = "On Start"
    override val category = "Event"
    override val description = "Start execution"
    override val headerColor = Color(0xFF44FF88)

    override val pins = listOf(OutputHeaderPin(defaultNextLabel))
}

private object BranchNode : ActionNode() {
    override val key = "builtin.branch"
    override val title = "Branch"
    override val category = "Flow Control"
    override val description = "Routes execution based on a Boolean condition."
    override val headerColor = StandardNodeColors.Headers.ControlFlow

    override val pins = listOf(
        InputHeaderPin("Exec"),
        InputPin("Condition", PinType.Boolean),
        OutputPin("True", PinType.Flow),
        OutputPin("False", PinType.Flow),
    )

    override suspend fun FlowScope.execute() {
        if (inputs.boolean("Condition")) {
            trigger("True")
        } else {
            trigger("False")
        }
    }
}

private object SequenceNode : ActionNode() {
    override val key = "builtin.sequence"
    override val title = "Sequence"
    override val category = "Flow Control"
    override val description = "Executes a series of pins sequentially."
    override val headerColor = StandardNodeColors.Headers.ControlFlow

    override val pins = listOf(
        InputHeaderPin("Exec"),
        OutputPin("Then 0", PinType.Flow),
        OutputPin("Then 1", PinType.Flow)
    )

    override suspend fun FlowScope.execute() {
        trigger("Then 0")
        trigger("Then 1")
    }
}

private object DelayNode : ActionNode() {
    override val key = "builtin.delay"
    override val title = "Delay"
    override val category = "Flow Control"
    override val description = "Pauses execution for a set amount of time."
    override val headerColor = StandardNodeColors.Headers.ControlFlow

    override val pins = listOf(
        InputHeaderPin("Exec"),
        InputPin("Seconds", PinType.Float),
        OutputHeaderPin("Completed")
    )

    override suspend fun FlowScope.execute() {
        val duration = inputs.float("Seconds")
        if (duration > 0) {
            delay((duration * 1000).toLong())
        }
        trigger("Completed")
    }
}

private object AndNode : PureNode() {
    override val key = "builtin.and"
    override val title = "AND"
    override val category = "Logic"
    override val description = "Returns true if both A and B are true."
    override val headerColor = Color(0xFFCC1C1C)
    override val compactTitle = "AND"

    override val pins = listOf(
        InputPin("A", PinType.Boolean),
        InputPin("B", PinType.Boolean),
        OutputPin("Result", PinType.Boolean),
    )

    override fun EvaluateScope.evaluate() {
        outputs["Result"] = inputs.boolean("A") && inputs.boolean("B")
    }
}

private object NotNode : PureNode() {
    override val key = "builtin.not"
    override val title = "NOT"
    override val category = "Logic"
    override val description = "Inverts a Boolean value."
    override val headerColor = Color(0xFFCC1C1C)
    override val compactTitle = "NOT"

    override val pins = listOf(
        InputPin("Value", PinType.Boolean),
        OutputPin("Result", PinType.Boolean),
    )

    override fun EvaluateScope.evaluate() {
        outputs["Result"] = !inputs.boolean("Value")
    }
}

private object FloatConstantNode : PureNode() {
    override val key = "builtin.float_constant"
    override val title = "Float"
    override val category = "Constants"
    override val description = "Provides a constant Float value."
    override val headerColor = Color(0xFF88D66C)

    override val pins = listOf(
        InputPin("Value", PinType.Float),
        OutputPin("Out", PinType.Float)
    )

    override fun EvaluateScope.evaluate() {
        outputs["Out"] = inputs.float("Value")
    }
}

private object IntConstantNode : PureNode() {
    override val key = "builtin.int_constant"
    override val title = "Integer"
    override val category = "Constants"
    override val description = "Provides a constant Integer value."
    override val headerColor = Color(0xFF4FC3F7)

    override val pins = listOf(
        InputPin("Value", PinType.Integer),
        OutputPin("Out", PinType.Integer)
    )

    override fun EvaluateScope.evaluate() {
        outputs["Out"] = inputs.int("Value")
    }
}

private object StringConstantNode : PureNode() {
    override val key = "builtin.string_constant"
    override val title = "String"
    override val category = "Constants"
    override val description = "Provides a constant String value."
    override val headerColor = Color(0xFFFF8A65)

    override val pins = listOf(
        InputPin("Value", PinType.String()),
        OutputPin("Out", PinType.String())
    )

    override fun EvaluateScope.evaluate() {
        outputs["Out"] = inputs.string("Value")
    }
}

object BuiltinExtension : GraphExtension {
    override val name = "nodegraph-builtins"
    override val version = "1.0.0"

    override fun install(registry: NodeRegistry) {
        registry.install(Loops, Std, Maths, Comparisons, Strings)

        registry.register(
            BranchNode, SequenceNode, DelayNode, AndNode, NotNode,
            IntConstantNode, FloatConstantNode, StringConstantNode
        )
    }
}
