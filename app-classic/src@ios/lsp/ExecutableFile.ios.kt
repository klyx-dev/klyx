package com.klyx.lsp

import platform.Foundation.NSFileManager

internal actual fun executablePath(path: String): String? =
    path.takeIf { NSFileManager.defaultManager.isExecutableFileAtPath(it) }
