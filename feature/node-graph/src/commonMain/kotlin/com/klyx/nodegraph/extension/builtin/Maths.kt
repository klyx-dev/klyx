package com.klyx.nodegraph.extension.builtin

import androidx.compose.ui.graphics.Color
import com.klyx.nodegraph.EvaluateScope
import com.klyx.nodegraph.InputPin
import com.klyx.nodegraph.NodeRegistry
import com.klyx.nodegraph.OutputPin
import com.klyx.nodegraph.PinType
import com.klyx.nodegraph.PureNode
import com.klyx.nodegraph.extension.GraphExtension

private fun promoteType(a: PinType?, b: PinType?): PinType? = when {
    a == null && b == null -> null
    a == null -> b
    b == null -> a
    a == b -> a
    a is PinType.String || b is PinType.String -> PinType.String()
    a == PinType.Float || b == PinType.Float -> PinType.Float
    a is PinType.Custom -> a
    b is PinType.Custom -> b
    else -> a
}

private object AddNode : PureNode() {
    override val key = "builtin.math.add"
    override val title = "Add"
    override val category = "Math/Test"
    override val description = "Adds A + B."
    override val headerColor = Color(0xFF2E7D32)
    override val compactTitle = "A + B"

    val mathWildcard = PinType.Wildcard(listOf(PinType.Float, PinType.Integer, PinType.String()))

    override val pins = listOf(
        InputPin("A", mathWildcard),
        InputPin("B", mathWildcard),
        OutputPin("Result", mathWildcard),
    )

    override fun EvaluateScope.evaluate() {
        val a = inputs.any("A")
        val b = inputs.any("B")
        outputs["Result"] = when {
            a is Float && b is Float -> a + b
            a is Int && b is Int -> a + b
            a is Float && b is Int -> a + b.toFloat()
            a is Int && b is Float -> a.toFloat() + b
            a is String -> a + (b?.toString() ?: "")
            b is String -> (a?.toString() ?: "") + b
            else -> a // unknown type
        }
    }

    override fun inferOutputTypes(inputTypes: Map<String, PinType>): Map<String, PinType> {
        val a = inputTypes["A"]?.takeIf { it !is PinType.Wildcard }
        val b = inputTypes["B"]?.takeIf { it !is PinType.Wildcard }
        val result = promoteType(a, b)
        return mapOf("Result" to (result ?: PinType.Wildcard()))
    }
}

private object SubtractNode : PureNode() {
    override val key = "builtin.math.subtract"
    override val title = "Subtract"
    override val category = "Math"
    override val description = "Subtracts A - B. Supports Float and Integer."
    override val headerColor = Color(0xFF2E7D32)
    override val compactTitle = "A − B"

    val mathWildcard = PinType.Wildcard(listOf(PinType.Float, PinType.Integer))

    override val pins = listOf(
        InputPin("A", mathWildcard),
        InputPin("B", mathWildcard),
        OutputPin("Result", mathWildcard),
    )

    override fun EvaluateScope.evaluate() {
        val a = inputs.any("A")
        val b = inputs.any("B")
        outputs["Result"] = when (a) {
            is Float -> if (b is Float) a - b else if (b is Int) a - b.toFloat() else a
            is Int -> if (b is Int) a - b else if (b is Float) a.toFloat() - b else a
            else -> a
        }
    }

    override fun inferOutputTypes(inputTypes: Map<String, PinType>): Map<String, PinType> {
        val a = inputTypes["A"]?.takeIf { it !is PinType.Wildcard }
        val b = inputTypes["B"]?.takeIf { it !is PinType.Wildcard }
        return mapOf("Result" to (promoteType(a, b) ?: PinType.Wildcard()))
    }
}

private object MultiplyNode : PureNode() {
    override val key = "builtin.math.multiply"
    override val title = "Multiply"
    override val category = "Math"
    override val description = "Multiplies A * B. Supports Float, Integer."
    override val headerColor = Color(0xFFB71C1C)
    override val compactTitle = "A * B"

    val mathWildcard = PinType.Wildcard(listOf(PinType.Float, PinType.Integer))

    override val pins = listOf(
        InputPin("A", mathWildcard),
        InputPin("B", mathWildcard),
        OutputPin("Result", mathWildcard),
    )

    override fun EvaluateScope.evaluate() {
        val a = inputs.any("A")
        val b = inputs.any("B")
        outputs["Result"] = when (a) {
            is Float -> if (b is Float) a * b else if (b is Int) a * b.toFloat() else a
            is Int -> if (b is Int) a * b else if (b is Float) a.toFloat() * b else a
            else -> a
        }
    }

    override fun inferOutputTypes(inputTypes: Map<String, PinType>): Map<String, PinType> {
        val a = inputTypes["A"]?.takeIf { it !is PinType.Wildcard }
        val b = inputTypes["B"]?.takeIf { it !is PinType.Wildcard }
        return mapOf("Result" to (promoteType(a, b) ?: PinType.Wildcard()))
    }
}

private object DivideNode : PureNode() {
    override val key = "builtin.math.divide"
    override val title = "Divide"
    override val category = "Math"
    override val description = "Divides A / B. Supports Float, Integer."
    override val headerColor = Color(0xFFB71C1C)
    override val compactTitle = "A / B"

    val mathWildcard = PinType.Wildcard(listOf(PinType.Float, PinType.Integer))

    override val pins = listOf(
        InputPin("A", mathWildcard),
        InputPin("B", mathWildcard),
        OutputPin("Result", mathWildcard),
    )

    override fun EvaluateScope.evaluate() {
        val a = inputs.any("A")
        val b = inputs.any("B")

        outputs["Result"] = when (a) {
            is Float -> {
                when (b) {
                    is Float -> {
                        if (b == 0f) {
                            log("Warning: Attempted to divide by zero in node graph. Returning 0.0.")
                            0f
                        } else {
                            a / b
                        }
                    }

                    is Int -> {
                        if (b == 0) {
                            log("Warning: Attempted to divide by zero in node graph. Returning 0.")
                            0f
                        } else {
                            a / b.toFloat()
                        }
                    }

                    else -> a
                }
            }

            is Int -> {
                when (b) {
                    is Int -> {
                        if (b == 0) {
                            log("Warning: Attempted to divide by zero in node graph. Returning 0.")
                            0f
                        } else {
                            a / b
                        }
                    }

                    is Float -> {
                        if (b == 0f) {
                            log("Warning: Attempted to divide by zero in node graph. Returning 0.0.")
                            0f
                        } else {
                            a.toFloat() / b
                        }
                    }

                    else -> a
                }
            }

            else -> a
        }
    }

    override fun inferOutputTypes(inputTypes: Map<String, PinType>): Map<String, PinType> {
        val a = inputTypes["A"]?.takeIf { it !is PinType.Wildcard }
        val b = inputTypes["B"]?.takeIf { it !is PinType.Wildcard }
        val result = when {
            a is PinType.Custom -> a
            b is PinType.Custom -> b
            else -> promoteType(a, b)
        }
        return mapOf("Result" to (result ?: PinType.Wildcard()))
    }
}

internal object Maths : GraphExtension {
    override val name = "Maths"

    override fun install(registry: NodeRegistry) {
        registry.register(AddNode, SubtractNode, MultiplyNode, DivideNode)
    }
}
