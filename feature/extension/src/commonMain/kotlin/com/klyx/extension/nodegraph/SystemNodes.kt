package com.klyx.extension.nodegraph

import com.klyx.core.platform.ToastDuration
import com.klyx.core.platform.currentPlatform
import com.klyx.core.platform.showToast
import com.klyx.nodegraph.FlowScope
import com.klyx.nodegraph.InputHeaderPin
import com.klyx.nodegraph.InputPin
import com.klyx.nodegraph.NodeRegistry
import com.klyx.nodegraph.OutputHeaderPin
import com.klyx.nodegraph.PinType
import com.klyx.nodegraph.SequentialActionNode
import com.klyx.nodegraph.SequentialEventNode
import com.klyx.nodegraph.enum
import com.klyx.nodegraph.extension.GraphExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object ShowToastNode : SequentialActionNode() {
    override val key = "klyx.system.toast"
    override val title = "Show Toast"
    override val category = "System"
    override val description = "Show toast message."

    val durationType = PinType.enum<ToastDuration>()

    override val pins = listOf(
        InputHeaderPin("Exec"),
        InputPin("Message", PinType.String()),
        InputPin("Duration", durationType, defaultValue = ToastDuration.Short.name),
        OutputHeaderPin(defaultNextLabel)
    )

    private val platform = currentPlatform()

    override suspend fun FlowScope.performAction() {
        val message = inputs.stringOrNull("Message")
        message?.let {
            val duration = inputs.enum<ToastDuration>("Duration")
            withContext(Dispatchers.Main.immediate) { platform.showToast(it, duration) }
        }
    }
}

internal object OnAppStart : SequentialEventNode() {
    override val key = "klyx.system.on_start"
    override val title = "On App Start"
    override val category = "Klyx"
    override val description = ""
    override val triggerName = key

    override val pins = listOf(OutputHeaderPin(defaultNextLabel))
}

fun ExtensionManager.onAppStart() {
    dispatchEvent(OnAppStart.triggerName)
}

object KlyxSystemExtension : GraphExtension {
    override val name = "Klyx System"

    override fun install(registry: NodeRegistry) {
        registry.register(ShowToastNode, OnAppStart)
    }
}
