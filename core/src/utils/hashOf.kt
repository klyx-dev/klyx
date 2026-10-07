package com.klyx.core.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import okio.Source
import okio.buffer

suspend fun hashOf(source: Source) = withContext(Dispatchers.IO) {
    source.buffer().readByteString().sha256().hex()
}

suspend fun hashOf(path: Path) = hashOf(FileSystem.SYSTEM.source(path))

fun hashOf(bytes: ByteArray) = bytes.toByteString().sha256().hex()
fun hashOf(string: String) = string.encodeUtf8().sha256().hex()
