package com.klyx.core

import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import java.nio.file.Paths

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object Environment {
    private val userHome by lazy { System.getProperty("user.home") }

    actual val AppName = "Klyx"
    actual val HomeDir by lazy { Paths.get(userHome, ".klyx").toString() }
    actual val InternalHomeDir by lazy { Paths.get(HomeDir, "internal").toString() }
    actual val ExtensionsDir by lazy { Paths.get(HomeDir, "extensions").toString() }
    actual val DevExtensionsDir by lazy { Paths.get(HomeDir, "dev-extensions").toString() }
    actual val DeviceHomeDir by lazy { Paths.get(userHome).toString() }
    actual val SettingsFilePath by lazy { Paths.get(HomeDir, "settings.json").toString() }

    actual val InternalSettingsFilePath by lazy {
        Paths.get(InternalHomeDir, "settings.json").toString()
    }

    actual val LogsDir by lazy { Paths.get(HomeDir, "logs").toString() }
}

actual fun string(
    resource: StringResource,
    vararg formatArgs: Any?
): String {
    return runBlocking { String.format(getString(resource), *formatArgs) }
}
