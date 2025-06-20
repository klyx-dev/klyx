package com.klyx.core

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
