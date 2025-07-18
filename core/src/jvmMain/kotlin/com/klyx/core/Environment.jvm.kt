package com.klyx.core

import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object Environment {
    actual val AppName: String
        get() = TODO("Not yet implemented")
    actual val HomeDir: String
        get() = TODO("Not yet implemented")
    actual val InternalHomeDir: String
        get() = TODO("Not yet implemented")
    actual val ExtensionsDir: String
        get() = TODO("Not yet implemented")
    actual val DevExtensionsDir: String
        get() = TODO("Not yet implemented")
    actual val DeviceHomeDir: String
        get() = TODO("Not yet implemented")
    actual val SettingsFilePath: String
        get() = TODO("Not yet implemented")
    actual val InternalSettingsFilePath: String
        get() = TODO("Not yet implemented")
    actual val LogsDir: String
        get() = TODO("Not yet implemented")

}

actual fun string(
    resource: StringResource,
    vararg formatArgs: Any?
): String {
    return runBlocking { String.format(getString(resource), *formatArgs) }
}
