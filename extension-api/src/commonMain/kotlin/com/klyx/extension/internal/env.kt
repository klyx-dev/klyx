package com.klyx.extension.internal

import com.github.michaelbull.result.Result

expect suspend fun getenv(name: String): String?
expect suspend fun getenv(): Map<String, String>

expect fun makeFileExecutable(path: String): Result<Unit, String>

expect val userHomeDir: String?
expect val rootDir: String
