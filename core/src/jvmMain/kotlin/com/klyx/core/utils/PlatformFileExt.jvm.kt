package com.klyx.core.utils

import io.github.vinceglb.filekit.PlatformFile

actual fun PlatformFile.isTermuxUri() = false

actual fun PlatformFile.actualUri() = file.toURI().toString()

actual fun PlatformFile.isFileUri() = true

actual suspend fun PlatformFile.tryGetActualFile(): PlatformFile? = this
