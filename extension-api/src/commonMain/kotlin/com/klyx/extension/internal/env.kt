package com.klyx.extension.internal

import io.itsvks.anyhow.AnyhowResult

expect suspend fun getenv(name: String): String?
expect suspend fun getenv(): Map<String, String>

expect fun makeFileExecutable(path: String): AnyhowResult<Unit>

expect val userHomeDir: String?
expect val rootDir: String
