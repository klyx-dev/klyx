package com.klyx.lsp.internal

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal fun Decoder.asJsonDecoder(): JsonDecoder = this as? JsonDecoder
    ?: throw IllegalStateException(
        "This serializer can be used only with Json format." +
                "Expected Decoder to be JsonDecoder, got ${this::class}"
    )

internal fun Encoder.asJsonEncoder() = this as? JsonEncoder
    ?: throw IllegalStateException(
        "This serializer can be used only with Json format." +
                "Expected Encoder to be JsonEncoder, got ${this::class}"
    )

@IgnorableReturnValue
internal fun verify(encoder: Encoder) = encoder.asJsonEncoder()

@IgnorableReturnValue
internal fun verify(decoder: Decoder) = decoder.asJsonDecoder()

internal fun <T> tryDeserialize(
    json: Json,
    serializer: KSerializer<T>,
    element: JsonElement
): T? {
    return try {
        json.decodeFromJsonElement(serializer, element)
    } catch (_: Throwable) {
        null
    }
}

internal fun isExactMatch(element: JsonElement, descriptor: SerialDescriptor): Boolean {
    return when (element) {
        is JsonPrimitive if element.isString -> descriptor.kind == PrimitiveKind.STRING

        is JsonPrimitive if !element.isString ->
            descriptor.kind == PrimitiveKind.INT ||
                    descriptor.kind == PrimitiveKind.LONG ||
                    descriptor.kind == PrimitiveKind.DOUBLE ||
                    descriptor.kind == PrimitiveKind.FLOAT ||
                    descriptor.kind == PrimitiveKind.BOOLEAN

        is JsonObject -> descriptor.kind == StructureKind.CLASS || descriptor.kind == StructureKind.OBJECT
        is JsonArray -> descriptor.kind == StructureKind.LIST
        else -> false
    }
}
