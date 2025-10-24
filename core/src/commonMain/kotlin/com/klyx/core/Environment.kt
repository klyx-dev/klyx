package com.klyx.core

import com.klyx.core.file.KxFile
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

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

    fun init()
}

fun Environment.defaultLogsFile(): KxFile {
    val file1 = KxFile(DeviceHomeDir, "klyx/app_logs.txt").also { runCatching { it.createNewFile() } }
    val file2 = KxFile(HomeDir, "app_logs.txt")
    return if (file1.canWrite) file1 else file2
}

private val SimpleStringFormatRegex = Regex("""%(\d+)\$[ds]""")
internal fun String.replaceWithArgs(args: List<String>) = SimpleStringFormatRegex.replace(this) { matchResult ->
    args[matchResult.groupValues[1].toInt() - 1]
}

fun string(resource: StringResource, vararg formatArgs: Any?) = runBlocking {
    getString(resource).replaceWithArgs(formatArgs.map { it.toString() })
}

inline val StringResource.value get() = string(this)
