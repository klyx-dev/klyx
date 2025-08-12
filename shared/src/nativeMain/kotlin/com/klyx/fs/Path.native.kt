package com.klyx.fs

import platform.posix.X_OK
import platform.posix.access

actual fun String.canExecute(): Boolean {
    return access(this, X_OK) == 0
}
