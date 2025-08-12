package com.klyx.fs

import java.io.File

actual fun String.canExecute(): Boolean {
    return File(this).canExecute()
}
