package com.klyx.nodegraph.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object ColorSerializer : KSerializer<Color> {
    override val descriptor = PrimitiveSerialDescriptor("color", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Color) {
        encoder.encodeLong(value.value.toLong())
    }

    override fun deserialize(decoder: Decoder): Color {
        return Color(value = decoder.decodeLong().toULong())
    }
}

internal object OffsetSerializer : KSerializer<Offset> {
    override val descriptor = PrimitiveSerialDescriptor("offset", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Offset) {
        encoder.encodeLong(value.packedValue)
    }

    override fun deserialize(decoder: Decoder): Offset {
        return Offset(packedValue = decoder.decodeLong())
    }
}

internal object SizeSerializer : KSerializer<Size> {
    override val descriptor = PrimitiveSerialDescriptor("size", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Size) {
        encoder.encodeLong(value.packedValue)
    }

    override fun deserialize(decoder: Decoder): Size {
        return Size(packedValue = decoder.decodeLong())
    }
}
