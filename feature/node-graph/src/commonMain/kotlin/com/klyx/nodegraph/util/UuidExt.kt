package com.klyx.nodegraph.util

import kotlin.uuid.Uuid

@Suppress("NOTHING_TO_INLINE")
internal inline fun generateId() = Uuid.generateV7()

internal fun mixUuid(a: Uuid, b: Uuid): Uuid {
    val aBytes = a.toByteArray()
    val bBytes = b.toByteArray()

    val result = ByteArray(16)

    for (i in 0 until 16) {
        result[i] = (aBytes[i].toInt() xor bBytes[i].toInt()).toByte()
    }

    return Uuid.fromByteArray(result)
}
