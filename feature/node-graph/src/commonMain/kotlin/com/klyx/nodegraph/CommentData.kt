@file:UseSerializers(ColorSerializer::class, OffsetSerializer::class, SizeSerializer::class)
@file:OptIn(ExperimentalSerializationApi::class)

package com.klyx.nodegraph

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.klyx.nodegraph.util.ColorSerializer
import com.klyx.nodegraph.util.OffsetSerializer
import com.klyx.nodegraph.util.SizeSerializer
import com.klyx.nodegraph.util.generateId
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.protobuf.ProtoNumber
import kotlin.uuid.Uuid

@Immutable
@Serializable
data class CommentData(
    @ProtoNumber(1)
    val id: Uuid = generateId(),
    @ProtoNumber(2)
    val title: String = "Comment",
    @ProtoNumber(3)
    val body: String = "",
    @ProtoNumber(4)
    val color: Color = Color(0x5588D66CL),
    @ProtoNumber(5)
    val position: Offset = Offset.Zero, // graph space
    @ProtoNumber(6)
    val size: Size = Size(300f, 200f), // graph space
)
