package com.klyx.api.language

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

inline fun buildQuery(block: QueryScope.() -> Unit): String {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return QueryScope().apply(block).build()
}

class QueryScope {
    private val lines = mutableListOf<String>()

    fun capture(pattern: String, name: String) {
        lines.add("($pattern) @$name")
    }

    fun string(text: String, name: String) {
        lines.add("\"$text\" @$name")
    }

    fun strings(vararg texts: String, name: String) {
        if (texts.isEmpty()) return
        if (texts.size == 1) {
            string(texts[0], name)
            return
        }
        lines.add("[\n" + texts.joinToString("\n") { "  \"$it\"" } + "\n] @$name")
    }

    fun anonymousNode(type: String, name: String) {
        lines.add("($type) @$name")
    }

    fun namedNode(type: String, field: String? = null, name: String) {
        val prefix = if (field != null) "$field: " else ""
        lines.add("(${prefix}$type) @$name")
    }

    fun raw(line: String) {
        lines.add(line)
    }

    fun comment(text: String) {
        lines.add(";; $text")
    }

    fun build(): String = lines.joinToString("\n")
}
