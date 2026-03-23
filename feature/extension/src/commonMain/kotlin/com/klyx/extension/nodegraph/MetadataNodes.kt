package com.klyx.extension.nodegraph

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.klyx.nodegraph.EvaluateScope
import com.klyx.nodegraph.InputPin
import com.klyx.nodegraph.NodeRegistry
import com.klyx.nodegraph.OutputHeaderPin
import com.klyx.nodegraph.OutputPin
import com.klyx.nodegraph.PinType
import com.klyx.nodegraph.PureNode
import com.klyx.nodegraph.SequentialEventNode
import com.klyx.nodegraph.customPinType
import com.klyx.nodegraph.extension.GraphExtension
import com.klyx.nodegraph.extension.Variable
import kotlinx.serialization.Serializable

internal const val EXTENSION_SCHEMA_VERSION = "1.0.0"

@Immutable
@Serializable
data class ExtensionMetadata(
    val id: String = "com.author.extension",
    val name: String = "New Extension",
    val description: String = "",
    val author: String = "Unknown",
    val version: String = "1.0.0",
    val targetApi: String = "1.0",
    val repositoryUrl: String = "",
    val supportedLanguages: List<String> = emptyList()
)

inline val PinType.Companion.ExtensionMetadata
    get() = customPinType("Metadata", typeColor = Color(0xFF673AB7))

internal object MakeMetadataNode : PureNode() {
    override val key = "klyx.metadata.make"
    override val title = "Make Metadata"
    override val category = "Metadata"
    override val description =
        "Constructs a new Extension Metadata object from individual properties. The ID must be a unique package name."
    override val headerColor = Color(0xFF673AB7)

    private val defaultMeta = ExtensionMetadata()

    override val pins = listOf(
        InputPin("ID", PinType.String(), defaultValue = defaultMeta.id),
        InputPin("Name", PinType.String(), defaultValue = defaultMeta.name),
        InputPin("Description", PinType.String(maxLines = 3), defaultValue = defaultMeta.description),
        InputPin("Author", PinType.String(), defaultValue = defaultMeta.author),
        InputPin("Version", PinType.String(), defaultValue = defaultMeta.version),
        InputPin("Target API", PinType.String(), defaultValue = defaultMeta.targetApi),
        InputPin("Repository", PinType.String(), defaultValue = defaultMeta.repositoryUrl),
        InputPin("Supported Languages (comma separated)", PinType.String(maxLines = 2), defaultValue = ""),
        OutputPin("Out", PinType.ExtensionMetadata)
    )

    override fun EvaluateScope.evaluate() {
        val languagesString = inputs.string(pins[7].label)
        val languagesList = languagesString
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

        outputs["Out"] = ExtensionMetadata(
            id = inputs.stringOrNull("ID")?.takeIf { it.isNotBlank() } ?: defaultMeta.id,
            name = inputs.stringOrNull("Name")?.takeIf { it.isNotBlank() } ?: defaultMeta.name,
            description = inputs.stringOrNull("Description") ?: defaultMeta.description,
            author = inputs.stringOrNull("Author")?.takeIf { it.isNotBlank() } ?: defaultMeta.author,
            version = inputs.stringOrNull("Version")?.takeIf { it.isNotBlank() } ?: defaultMeta.version,
            targetApi = inputs.stringOrNull("Target API") ?: defaultMeta.targetApi,
            repositoryUrl = inputs.stringOrNull("Repository") ?: defaultMeta.repositoryUrl,
            supportedLanguages = languagesList
        )
    }
}

internal object BreakMetadataNode : PureNode() {
    override val key = "klyx.metadata.break"
    override val title = "Break Metadata"
    override val category = "Metadata"
    override val description = "Splits an Extension Metadata object into its individual properties for reading."
    override val headerColor = Color(0xFF673AB7)

    override val pins = listOf(
        InputPin("Metadata", PinType.ExtensionMetadata),
        OutputPin("ID", PinType.String()),
        OutputPin("Name", PinType.String()),
        OutputPin("Description", PinType.String()),
        OutputPin("Author", PinType.String()),
        OutputPin("Version", PinType.String()),
        OutputPin("Target API", PinType.String()),
        OutputPin("Repository", PinType.String()),
        OutputPin("Supported Languages", PinType.String())
    )

    override fun EvaluateScope.evaluate() {
        val meta = inputs["Metadata"] ?: ExtensionMetadata()

        outputs["ID"] = meta.id
        outputs["Name"] = meta.name
        outputs["Description"] = meta.description
        outputs["Author"] = meta.author
        outputs["Version"] = meta.version
        outputs["Target API"] = meta.targetApi
        outputs["Repository"] = meta.repositoryUrl
        outputs["Supported Languages"] = meta.supportedLanguages.joinToString(", ")
    }
}

internal object OnInitializeMetadataNode : SequentialEventNode() {
    override val key = "klyx.metadata.on_initialize"
    override val title = "On Initialize Metadata"
    override val category = "Metadata"
    override val description = "Runs immediately when the file is parsed so the engine can read extension info."
    override val headerColor = Color(0xFF673AB7)
    override val triggerName = key

    override val pins = listOf(OutputHeaderPin(defaultNextLabel))
}

object MetadataExtension : GraphExtension {

    override val name = "klyx-extension-metadata"
    override val version = EXTENSION_SCHEMA_VERSION

    override val variables = buildList {
        add(
            Variable(
                name = "Extension Config",
                type = PinType.ExtensionMetadata,
                defaultValue = ExtensionMetadata(),
                isSystem = true
            )
        )
    }

    override fun install(registry: NodeRegistry) {
        registry.registerCustomType<ExtensionMetadata>(
            typeName = PinType.ExtensionMetadata.typeName,
            color = PinType.ExtensionMetadata.color
        )

        registry.register(
            MakeMetadataNode,
            BreakMetadataNode,
            OnInitializeMetadataNode
        )
    }
}
