package com.klyx.extension.nodegraph

import com.klyx.core.lsp.LanguageServerBinary
import com.klyx.core.process.which
import com.klyx.nodegraph.ActionNode
import com.klyx.nodegraph.EvaluateScope
import com.klyx.nodegraph.FlowScope
import com.klyx.nodegraph.InputHeaderPin
import com.klyx.nodegraph.InputPin
import com.klyx.nodegraph.NodeRegistry
import com.klyx.nodegraph.OutputFlowPin
import com.klyx.nodegraph.OutputHeaderPin
import com.klyx.nodegraph.OutputPin
import com.klyx.nodegraph.PinType
import com.klyx.nodegraph.PureNode
import com.klyx.nodegraph.SequentialActionNode
import com.klyx.nodegraph.SequentialEventNode
import com.klyx.nodegraph.customPinType
import com.klyx.nodegraph.extension.GraphExtension
import kotlinx.coroutines.CompletableDeferred
import kotlinx.io.files.Path

data class LspProvisionResult(
    val binary: LanguageServerBinary,
    val initializationOptionsJson: String,
    val workspaceConfigJson: String
)

class LspProvisionRequest(
    val languageName: String,
    val worktreePath: String
) {

    val response = CompletableDeferred<LspProvisionResult>()

    internal fun provide(
        binaryPath: String,
        args: List<String>,
        initOptionsJson: String,
        workspaceConfigJson: String
    ) {
        response.complete(
            LspProvisionResult(
                binary = LanguageServerBinary(Path(binaryPath), args, hashMapOf()),
                initializationOptionsJson = initOptionsJson,
                workspaceConfigJson = workspaceConfigJson
            )
        )
    }
}

val LspRequestType = customPinType("LspRequest")

fun ExtensionManager.onRequestLsp(languageName: String, request: LspProvisionRequest) {
    dispatchEventForLanguage(
        language = languageName,
        event = OnLspRequestedNode.triggerName,
        params = mapOf("Request" to request)
    )
}

internal object OnLspRequestedNode : SequentialEventNode() {
    override val key = "editor.event.onLspRequested"
    override val title = "On LSP Requested"
    override val category = "Editor LSP"
    override val description =
        "Triggered when the editor needs a Language Server Protocol (LSP) to provide code intelligence for a specific language."

    override val triggerName = "editor.requestLsp"

    override val pins = listOf(
        OutputHeaderPin(defaultNextLabel),
        OutputPin("Request", LspRequestType)
    )
}

internal object ProvideLspNode : SequentialActionNode(defaultNextLabel = "Out") {
    override val key = "editor.action.provideLsp"
    override val title = "Provide LSP Binary"
    override val category = "Editor LSP"
    override val description =
        "Fulfills an LSP Provision Request by sending the binary path, arguments, and JSON configuration back to the editor."

    override val pins = listOf(
        InputHeaderPin("Exec"),
        InputPin("Request", LspRequestType),
        InputPin("Binary Path", PinType.String()),
        InputPin("Arguments (Comma Separated)", PinType.String(), defaultValue = ""),
        InputPin("Init Options (JSON)", PinType.String(maxLines = -1), defaultValue = "{}"),
        InputPin("Workspace Config (JSON)", PinType.String(maxLines = -1), defaultValue = "{}"),
        OutputHeaderPin(defaultNextLabel)
    )

    override suspend fun FlowScope.performAction() {
        val request: LspProvisionRequest = inputs["Request"] ?: return

        val path = inputs.string("Binary Path")
        if (path.isEmpty()) return

        val argsString = inputs.string(pins[3].label)
        val initOptions = inputs.string(pins[4].label).ifEmpty { "{}" }
        val workspaceConfig = inputs.string(pins[5].label).ifEmpty { "{}" }

        val args = argsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        request.provide(
            binaryPath = path,
            args = args,
            initOptionsJson = initOptions,
            workspaceConfigJson = workspaceConfig
        )
    }
}

private object GetLspRequestInfoNode : PureNode() {
    override val key = "editor.utils.getLspRequestInfo"
    override val title = "Get LSP Request Info"
    override val category = "Editor LSP"
    override val description =
        "Extracts the target language name and the current worktree path from an LSP Provision Request."

    override val pins = listOf(
        InputPin("Request", LspRequestType),
        OutputPin("Language", PinType.String()),
        OutputPin("Worktree Path", PinType.String())
    )

    override fun EvaluateScope.evaluate() {
        val request = inputs.get<LspProvisionRequest>("Request")
        outputs["Language"] = request?.languageName ?: ""
        outputs["Worktree Path"] = request?.worktreePath ?: ""
    }
}

internal object WorktreeWhichNode : ActionNode() {
    override val key = "editor.worktree.which"
    override val title = "Which"
    override val category = "Editor LSP"
    override val description =
        "Searches the system PATH for the given binary name and returns its absolute path if found."

    override val pins = listOf(
        InputHeaderPin("Exec"),
        InputPin("Binary Name", PinType.String()),
        OutputFlowPin("Found"),
        OutputFlowPin("Not Found"),
        OutputPin("Path", PinType.String())
    )

    override suspend fun FlowScope.execute() {
        inputs.stringOrNull(pins[1].label)?.let { binaryName ->
            which(binaryName)
                .onSuccess {
                    updateOutput("Path", it.toString())
                    trigger("Found")
                }
                .onFailure {
                    updateOutput("Path", "")
                    trigger("Not Found")
                }
        } ?: trigger("Not Found")
    }
}

object EditorLspExtension : GraphExtension {
    override val name = "Klyx LSP Extension"
    override val version = EXTENSION_SCHEMA_VERSION

    override fun install(registry: NodeRegistry) {
        registry.register(
            OnLspRequestedNode,
            ProvideLspNode,
            GetLspRequestInfoNode,
            WorktreeWhichNode
        )
    }
}
