package com.klyx.nodegraph

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import kotlin.uuid.Uuid

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class NodeConnection(
    @ProtoNumber(1)
    val id: Uuid,
    @ProtoNumber(2)
    val outputPinId: Uuid,
    @ProtoNumber(3)
    val inputPinId: Uuid,
    @ProtoNumber(4)
    val isCast: Boolean = false
)
