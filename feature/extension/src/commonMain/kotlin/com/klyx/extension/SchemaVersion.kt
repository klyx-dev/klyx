package com.klyx.extension

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class SchemaVersion(val value: Int) {
    @Suppress("FunctionName")
    fun is_v0() = this == Zero

    override fun toString() = value.toString()

    companion object {
        val Zero = SchemaVersion(0)
    }
}
