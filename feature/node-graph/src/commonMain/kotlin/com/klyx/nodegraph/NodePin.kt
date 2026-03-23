@file:OptIn(ExperimentalSerializationApi::class)

package com.klyx.nodegraph

import androidx.compose.runtime.Immutable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import kotlin.uuid.Uuid

@Immutable
@Serializable
data class NodePin(
    @ProtoNumber(1)
    val id: Uuid,
    @ProtoNumber(2)
    val label: String,
    @ProtoNumber(3)
    val type: PinType,
    @ProtoNumber(4)
    val direction: PinDirection,
    @ProtoNumber(5)
    val nodeId: Uuid,
    @ProtoNumber(6)
    val showInHeader: Boolean = false,
    @ProtoNumber(7)
    val defaultValue: String? = null
)
