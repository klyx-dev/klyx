package com.klyx.editor.language

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class LanguageName(val value: String) : CharSequence by value {
    override fun toString(): String = value

    fun lspId() = when (value) {
        "Plain Text" -> "plaintext"
        else -> value.lowercase()
    }
}
