package com.klyx.core

import android.util.Log
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.PathUtils
import com.klyx.core.settings.SETTINGS_FILE_NAME
import com.klyx.platform.PlatformInfo
import java.io.File

actual object Environment {
    actual val AppName: String
        get() = "Klyx"

    actual val HomeDir: String
        get() = "${PathUtils.getInternalAppFilesPath()}/$AppName"

    actual val InternalHomeDir = "$HomeDir/internal"

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

    actual fun init() {
        listOf(
            HomeDir,
            InternalHomeDir,
            ExtensionsDir,
            DevExtensionsDir,
            LogsDir
        ).forEach { path ->
            if (!FileUtils.createOrExistsDir(path)) {
                Log.e("Environment", "Failed to create directory: $path")

                if (!File(path).mkdirs()) {
                    throw RuntimeException("Klyx can't run on this device: ${PlatformInfo.deviceModel} (${PlatformInfo.name} ${PlatformInfo.version})")
                }
            }
        }
    }
}
