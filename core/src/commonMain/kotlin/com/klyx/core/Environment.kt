package com.klyx.core

import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object Environment {
    val AppName: String
    val HomeDir: String
    val InternalHomeDir: String

    val ExtensionsDir: String
    val DevExtensionsDir: String

    val DeviceHomeDir: String
    val SettingsFilePath: String
    val InternalSettingsFilePath: String
    val LogsDir: String
}

private val SimpleStringFormatRegex = Regex("""%(\d+)\$[ds]""")
internal fun String.replaceWithArgs(args: List<String>) = SimpleStringFormatRegex.replace(this) { matchResult ->
    args[matchResult.groupValues[1].toInt() - 1]
}

fun string(resource: StringResource, vararg formatArgs: Any?) = runBlocking {
    getString(resource).replaceWithArgs(formatArgs.map { it.toString() })
}
