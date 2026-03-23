@file:Suppress("FunctionName", "NOTHING_TO_INLINE")
@file:OptIn(ExperimentalSerializationApi::class)

package com.klyx.nodegraph

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class Pin(
    @ProtoNumber(1)
    val label: String,
    @ProtoNumber(2)
    val type: PinType,
    @ProtoNumber(3)
    val direction: PinDirection,
    @ProtoNumber(4)
    val showInHeaderIfPossible: Boolean = false,
    @ProtoNumber(5)
    val defaultValue: String? = null
)

fun InputPin(
    label: String,
    type: PinType,
    defaultValue: String? = null
) = Pin(label, type, PinDirection.Input, false, defaultValue)

fun OutputPin(label: String, type: PinType) = Pin(label, type, PinDirection.Output)
fun OutputFlowPin(label: String) = Pin(label, PinType.Flow, PinDirection.Output)

fun HeaderPin(label: String, direction: PinDirection) = Pin(label, PinType.Flow, direction, true)

inline fun OutputHeaderPin(label: String = "Then") = HeaderPin(label, PinDirection.Output)
inline fun InputHeaderPin(label: String = "Exec") = HeaderPin(label, PinDirection.Input)
