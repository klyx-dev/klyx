package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Code action tags are extra annotations that tweak the behavior of a code action.
 *
 * @since 3.18.0 - proposed
 */
@Serializable
@JvmInline
value class CodeActionTag private constructor(private val value: Int) {
    companion object {
        /**
         * Marks the code action as LLM-generated.
         */
        val LLMGenerated = CodeActionTag(1)
    }
}
