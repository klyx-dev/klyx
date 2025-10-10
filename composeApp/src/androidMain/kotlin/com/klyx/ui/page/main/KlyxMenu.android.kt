package com.klyx.ui.page.main

import com.blankj.utilcode.util.AppUtils

internal actual fun restartApp(isKillProcess: Boolean) {
    AppUtils.relaunchApp(isKillProcess)
}
