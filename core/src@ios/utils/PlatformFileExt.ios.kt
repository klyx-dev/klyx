package com.klyx.core.utils

import io.github.vinceglb.filekit.PlatformFile

actual fun PlatformFile.isTermuxUri() = false

actual fun PlatformFile.actualUri(): String = nsUrl.toString()

actual fun PlatformFile.isFileUri() = true
