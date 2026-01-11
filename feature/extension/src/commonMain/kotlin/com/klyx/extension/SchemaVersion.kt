package com.klyx.extension

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class SchemaVersion(val value: Int) : Comparable<SchemaVersion> {
    @Suppress("FunctionName")
    fun is_v0() = this == Zero

    operator fun rangeTo(other: SchemaVersion) = object : ClosedRange<SchemaVersion> {
        override val start = this@SchemaVersion
        override val endInclusive = other
    }

    operator fun rangeUntil(other: SchemaVersion) = object : OpenEndRange<SchemaVersion> {
        override val start = this@SchemaVersion
        override val endExclusive = other
    }

    override fun toString() = value.toString()

    override fun compareTo(other: SchemaVersion) = value.compareTo(other.value)

    companion object {
        val Zero = SchemaVersion(0)
    }
}
