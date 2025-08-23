package com.klyx.core

import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.PathUtils
import com.klyx.core.settings.SETTINGS_FILE_NAME
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object Environment {
    actual val AppName: String
        get() = AppUtils.getAppName()

    actual val HomeDir: String
        get() = "${PathUtils.getExternalAppFilesPath()}/$AppName"

    actual val InternalHomeDir: String
        get() = "${PathUtils.getInternalAppFilesPath()}/$AppName"

    actual val ExtensionsDir: String
        get() = "$HomeDir/extensions"

    actual val DevExtensionsDir: String
        get() = "$HomeDir/dev_extensions"

    actual val DeviceHomeDir: String
        get() = PathUtils.getExternalStoragePath()

    actual val SettingsFilePath: String
        get() = "$HomeDir/$SETTINGS_FILE_NAME"

    actual val InternalSettingsFilePath: String
        get() = "$InternalHomeDir/$SETTINGS_FILE_NAME"

    actual val LogsDir: String
        get() = "$HomeDir/logs"

    init {
        listOf(
            HomeDir,
            InternalHomeDir,
            ExtensionsDir,
            DevExtensionsDir,
            LogsDir
        ).forEach(FileUtils::createOrExistsDir)
    }
}
