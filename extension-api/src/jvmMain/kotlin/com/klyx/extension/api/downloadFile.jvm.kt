package com.klyx.extension.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.GZIPInputStream

actual suspend fun decompressGzip(data: ByteArray) = withContext(Dispatchers.IO) {
    GZIPInputStream(data.inputStream()).use { it.readBytes() }
}
