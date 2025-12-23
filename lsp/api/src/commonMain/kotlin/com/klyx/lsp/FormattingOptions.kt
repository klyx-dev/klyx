package com.klyx.lsp

import com.klyx.lsp.internal.verify
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Value-object describing what options formatting should use.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#formattingOptions)
 */
@Serializable(FormattingOptionsSerializer::class)
data class FormattingOptions(
    /**
     * Size of a tab in spaces.
     */
    var tabSize: UInt,

    /**
     * Prefer spaces over tabs.
     */
    var insertSpaces: Boolean,

    /**
     * Trim trailing whitespace on a line.
     *
     * @since 3.15.0
     */
    var trimTrailingWhitespace: Boolean? = null,

    /**
     * Insert a newline character at the end of the file if one does not exist.
     *
     * @since 3.15.0
     */
    var insertFinalNewline: Boolean? = null,

    /**
     * Trim all newlines after the final newline at the end of the file.
     *
     * @since 3.15.0
     */
    var trimFinalNewlines: Boolean? = null,

    /**
     * Signature for further properties.
     */
    val additionalProperties: Map<String, JsonPrimitive> = emptyMap()
) {
    /**
     * Gets an additional property value as a Boolean, or null if not present or not a boolean.
     */
    fun getBooleanProperty(key: String) = additionalProperties[key]?.booleanOrNull

    /**
     * Gets an additional property value as an Int, or null if not present or not an integer.
     */
    fun getIntProperty(key: String) = additionalProperties[key]?.intOrNull

    /**
     * Gets an additional property value as a String, or null if not present or not a string.
     */
    fun getStringProperty(key: String) = additionalProperties[key]?.contentOrNull

    /**
     * Creates a copy with an additional boolean property.
     */
    fun withBooleanProperty(key: String, value: Boolean): FormattingOptions =
        copy(additionalProperties = additionalProperties + (key to JsonPrimitive(value)))

    /**
     * Creates a copy with an additional integer property.
     */
    fun withIntProperty(key: String, value: Int): FormattingOptions =
        copy(additionalProperties = additionalProperties + (key to JsonPrimitive(value)))

    /**
     * Creates a copy with an additional string property.
     */
    fun withStringProperty(key: String, value: String): FormattingOptions =
        copy(additionalProperties = additionalProperties + (key to JsonPrimitive(value)))
}

internal object FormattingOptionsSerializer : KSerializer<FormattingOptions> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("FormattingOptions")

    override fun serialize(encoder: Encoder, value: FormattingOptions) {
        val encoder = verify(encoder)

        val jsonObject = buildJsonObject {
            put("tabSize", value.tabSize.toInt())
            put("insertSpaces", value.insertSpaces)

            value.trimTrailingWhitespace?.let { put("trimTrailingWhitespace", it) }
            value.insertFinalNewline?.let { put("insertFinalNewline", it) }
            value.trimFinalNewlines?.let { put("trimFinalNewlines", it) }

            for ((key, jsonValue) in value.additionalProperties) {
                put(key, jsonValue)
            }
        }
        encoder.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): FormattingOptions {
        val decoder = verify(decoder)
        val jsonObject = decoder.decodeJsonElement().jsonObject

        val tabSize = requireNotNull(jsonObject["tabSize"]?.jsonPrimitive?.int?.toUInt()) {
            "Missing required field: tabSize"
        }
        val insertSpaces = requireNotNull(jsonObject["insertSpaces"]?.jsonPrimitive?.boolean) {
            "Missing required field: insertSpaces"
        }

        val trimTrailingWhitespace = jsonObject["trimTrailingWhitespace"]?.jsonPrimitive?.booleanOrNull
        val insertFinalNewline = jsonObject["insertFinalNewline"]?.jsonPrimitive?.booleanOrNull
        val trimFinalNewlines = jsonObject["trimFinalNewlines"]?.jsonPrimitive?.booleanOrNull

        val knownFields = setOf(
            "tabSize",
            "insertSpaces",
            "trimTrailingWhitespace",
            "insertFinalNewline",
            "trimFinalNewlines"
        )

        val additionalProperties = jsonObject
            .filterKeys { it !in knownFields }
            .mapValues { (_, value) ->
                when {
                    value is JsonPrimitive -> value
                    else -> JsonPrimitive(value.toString())
                }
            }

        return FormattingOptions(
            tabSize = tabSize,
            insertSpaces = insertSpaces,
            trimTrailingWhitespace = trimTrailingWhitespace,
            insertFinalNewline = insertFinalNewline,
            trimFinalNewlines = trimFinalNewlines,
            additionalProperties = additionalProperties
        )
    }
}
