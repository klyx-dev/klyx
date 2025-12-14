package com.klyx.core.language

import kotlin.jvm.JvmInline

typealias LanguageId = String

@JvmInline
value class LanguageName(val value: String) : CharSequence by value {
    override fun toString(): String = value

    fun lspId() = when (value) {
        "Plain Text" -> "plaintext"
        else -> value.lowercase()
    }
}
