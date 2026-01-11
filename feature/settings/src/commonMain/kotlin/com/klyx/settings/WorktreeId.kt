package com.klyx.settings

import kotlin.jvm.JvmInline

@JvmInline
value class WorktreeId(val value: ULong) {
    override fun toString() = value.toString()
}
