package com.klyx.core.serializers.path

import kotlinx.io.files.Path
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object PathListSerializer : KSerializer<List<Path>> by ListSerializer(PathSerializer)

object PathSerializer : KSerializer<Path> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlinx.io.files.Path", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Path) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Path {
        return Path(decoder.decodeString())
    }
}

