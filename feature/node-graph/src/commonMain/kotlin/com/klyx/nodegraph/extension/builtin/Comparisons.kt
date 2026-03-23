package com.klyx.nodegraph.extension.builtin

import androidx.compose.ui.graphics.Color
import com.klyx.nodegraph.EvaluateScope
import com.klyx.nodegraph.InputPin
import com.klyx.nodegraph.NodeRegistry
import com.klyx.nodegraph.OutputPin
import com.klyx.nodegraph.PinType
import com.klyx.nodegraph.PureNode
import com.klyx.nodegraph.extension.GraphExtension

private object EqualNode : PureNode() {
    override val key = "builtin.equal"
    override val title = "Equal"
    override val category = "Comparison"
    override val description = "Returns true if A == B."
    override val headerColor = Color(0xFF37474F)
    override val compactTitle = "A == B"

    override val pins = listOf(
        InputPin("A", PinType.Wildcard()),
        InputPin("B", PinType.Wildcard()),
        OutputPin("Result", PinType.Boolean),
    )

    override fun EvaluateScope.evaluate() {
        outputs["Result"] = inputs.any("A") == inputs.any("B")
    }
}

private object GreaterThanNode : PureNode() {
    override val key = "builtin.greater_than"
    override val title = "Greater Than"
    override val category = "Comparison"
    override val description = "Returns true if A > B."
    override val headerColor = Color(0xFF37474F)
    override val compactTitle = ">"

    override val pins = listOf(
        InputPin("A", PinType.Wildcard(listOf(PinType.Float, PinType.Integer))),
        InputPin("B", PinType.Wildcard(listOf(PinType.Float, PinType.Integer))),
        OutputPin("Result", PinType.Boolean),
    )

    override fun EvaluateScope.evaluate() {
        val a = inputs.any("A")
        val b = inputs.any("B")
        outputs["Result"] = when (a) {
            is Float -> if (b is Float) a > b else if (b is Int) a > b.toFloat() else false
            is Int -> if (b is Int) a > b else if (b is Float) a.toFloat() > b else false
            else -> false
        }
    }
}

private object GreaterThanOrEqual : PureNode() {
    override val key = "builtin.greater_than_or_equal"
    override val title = "Greater Than or Equal"
    override val category = "Comparison"
    override val description = "Returns true if A >= B."
    override val headerColor = Color(0xFF37474F)
    override val compactTitle = "A >= B"

    override val pins = listOf(
        InputPin("A", PinType.Wildcard(listOf(PinType.Float, PinType.Integer))),
        InputPin("B", PinType.Wildcard(listOf(PinType.Float, PinType.Integer))),
        OutputPin("Result", PinType.Boolean),
    )

    override fun EvaluateScope.evaluate() {
        val a = inputs.any("A")
        val b = inputs.any("B")
        outputs["Result"] = when (a) {
            is Float -> if (b is Float) a >= b else if (b is Int) a >= b.toFloat() else false
            is Int -> if (b is Int) a >= b else if (b is Float) a.toFloat() >= b else false
            else -> false
        }
    }
}

private object LessThanNode : PureNode() {
    override val key = "builtin.less_than"
    override val title = "Less Than"
    override val category = "Comparison"
    override val description = "Returns true if A < B."
    override val headerColor = Color(0xFF37474F)
    override val compactTitle = "A < B"

    override val pins = listOf(
        InputPin("A", PinType.Wildcard(listOf(PinType.Float, PinType.Integer))),
        InputPin("B", PinType.Wildcard(listOf(PinType.Float, PinType.Integer))),
        OutputPin("Result", PinType.Boolean),
    )

    override fun EvaluateScope.evaluate() {
        val a = inputs.any("A")
        val b = inputs.any("B")
        outputs["Result"] = when (a) {
            is Float -> if (b is Float) a < b else if (b is Int) a < b.toFloat() else false
            is Int -> if (b is Int) a < b else if (b is Float) a.toFloat() < b else false
            else -> false
        }
    }
}

private object LessThanOrEqual : PureNode() {
    override val key = "builtin.less_than_or_equal"
    override val title = "Less Than or Equal"
    override val category = "Comparison"
    override val description = "Returns true if A <= B."
    override val headerColor = Color(0xFF37474F)
    override val compactTitle = "A <= B"

    override val pins = listOf(
        InputPin("A", PinType.Wildcard(listOf(PinType.Float, PinType.Integer))),
        InputPin("B", PinType.Wildcard(listOf(PinType.Float, PinType.Integer))),
        OutputPin("Result", PinType.Boolean),
    )

    override fun EvaluateScope.evaluate() {
        val a = inputs.any("A")
        val b = inputs.any("B")
        outputs["Result"] = when (a) {
            is Float -> if (b is Float) a <= b else if (b is Int) a <= b.toFloat() else false
            is Int -> if (b is Int) a <= b else if (b is Float) a.toFloat() <= b else false
            else -> false
        }
    }
}

internal object Comparisons : GraphExtension {
    override val name = "Comparisons"

    override fun install(registry: NodeRegistry) {
        registry.register(
            EqualNode, GreaterThanNode, LessThanNode,
            GreaterThanOrEqual, LessThanOrEqual
        )
    }
}
