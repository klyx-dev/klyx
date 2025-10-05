package com.klyx

import com.klyx.core.ContextHolder
import com.klyx.core.requestStoragePermission

actual fun requestFileAccessPermission() {
    ContextHolder.currentActivityOrNull()?.requestStoragePermission()
}
