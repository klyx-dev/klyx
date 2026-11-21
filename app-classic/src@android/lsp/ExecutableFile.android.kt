package com.klyx.lsp

import java.io.File

internal actual fun executablePath(path: String): String? =
    File(path).takeIf { it.isFile && it.canExecute() }?.absolutePath
