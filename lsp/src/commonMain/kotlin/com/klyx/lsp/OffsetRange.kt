package com.klyx.lsp

import com.klyx.lsp.internal.verify
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

@Serializable(OffsetRangeSerializer::class)
data class OffsetRange(val start: Int, val end: Int)

internal object OffsetRangeSerializer : KSerializer<OffsetRange> {
    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor("OffsetRange", StructureKind.LIST)

    override fun serialize(encoder: Encoder, value: OffsetRange) {
        val encoder = verify(encoder)
        encoder.encodeJsonElement(JsonArray(listOf(JsonPrimitive(value.start), JsonPrimitive(value.end))))
    }

    override fun deserialize(decoder: Decoder): OffsetRange {
        val decoder = verify(decoder)
        val array = decoder.decodeJsonElement().jsonArray
        return OffsetRange(array[0].jsonPrimitive.int, array[1].jsonPrimitive.int)
    }
}
