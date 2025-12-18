package com.klyx.buildlogic

internal fun String.toPascalCase(): String =
    split("-", "_")
        .filter { it.isNotBlank() }
        .joinToString("") { part ->
            part.replaceFirstChar { it.uppercase() }
        }

internal fun String.toCamelCase(): String {
    val pascal = toPascalCase()
    return pascal.replaceFirstChar { it.lowercase() }
}
