package com.klyx.core

import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.PathUtils

object Env {
    val APP_NAME = AppUtils.getAppName()

    val APP_HOME_DIR: String = "${PathUtils.getExternalAppFilesPath()}/$APP_NAME"
    val INTERNAL_APP_HOME_DIR: String = "${PathUtils.getInternalAppFilesPath()}/$APP_NAME"
    val EXTENSIONS_DIR = "$APP_HOME_DIR/extensions"
    val DEV_EXTENSIONS_DIR = "$APP_HOME_DIR/dev_extensions"

    val DEVICE_HOME_DIR: String = PathUtils.getExternalStoragePath()

    init {
        FileUtils.createOrExistsDir(APP_HOME_DIR)
        FileUtils.createOrExistsDir(INTERNAL_APP_HOME_DIR)
        FileUtils.createOrExistsDir(EXTENSIONS_DIR)
        FileUtils.createOrExistsDir(DEV_EXTENSIONS_DIR)
    }
}
