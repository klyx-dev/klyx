package com.klyx.core.util

import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

private val SimpleStringFormatRegex = Regex("""%(\d+)\$[ds]""")

private fun String.replaceWithArgs(args: List<String>) = SimpleStringFormatRegex.replace(this) { matchResult ->
    args[matchResult.groupValues[1].toInt() - 1]
}

fun string(resource: StringResource, vararg formatArgs: Any?) = runBlocking {
    getString(resource).replaceWithArgs(formatArgs.map { it.toString() })
}

inline val StringResource.value get() = string(this)
