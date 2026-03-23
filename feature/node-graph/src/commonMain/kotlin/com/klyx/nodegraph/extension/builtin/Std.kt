package com.klyx.nodegraph.extension.builtin

import androidx.compose.ui.graphics.Color
import com.klyx.nodegraph.ActionNode
import com.klyx.nodegraph.FlowScope
import com.klyx.nodegraph.InputHeaderPin
import com.klyx.nodegraph.InputPin
import com.klyx.nodegraph.NodeRegistry
import com.klyx.nodegraph.OutputHeaderPin
import com.klyx.nodegraph.PinType
import com.klyx.nodegraph.extension.GraphExtension

private object PrintStringNode : ActionNode() {
    override val key = "builtin.print_string"
    override val title = "Print String"
    override val category = "Utilities"
    override val description = "Logs a string value to the output."
    override val headerColor = Color(0xFF6A1B9A)

    override val pins = listOf(
        InputHeaderPin("Exec"),
        InputPin("In String", PinType.String()),
        OutputHeaderPin("Then")
    )

    override suspend fun FlowScope.execute() {
        log(inputs.string("In String"))
        trigger("Then")
    }
}

private object PrintAny : ActionNode() {
    override val key = "builtin.print_any"
    override val title = "Print"
    override val category = "Utilities"
    override val description = "Logs any value into string to the output."
    override val headerColor = Color(0xFF6A1B9A)

    override val pins = listOf(
        InputHeaderPin("Exec"),
        InputPin("Any", PinType.Wildcard()),
        OutputHeaderPin("Then")
    )

    override suspend fun FlowScope.execute() {
        inputs.any("Any")?.toString()?.let { log(it) }
        trigger("Then")
    }
}

internal object Std : GraphExtension {
    override val name = "Standard Library"

    override fun install(registry: NodeRegistry) {
        registry.register(PrintStringNode, PrintAny)
    }
}
