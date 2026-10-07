package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class WorkDoneProgressKind private constructor(private val kind: String) {
    override fun toString() = kind

    companion object {
        val Begin = WorkDoneProgressKind("begin")
        val Report = WorkDoneProgressKind("report")
        val End = WorkDoneProgressKind("end")
    }
}
