package com.klyx.extension.internal

import com.github.michaelbull.result.Result

expect fun getenv(name: String): String?
expect fun getenv(): Map<String, String>

expect fun makeFileExecutable(path: String): Result<Unit, String>

expect fun findBinary(binaryName: String): String?

expect fun executeCommand(command: String, args: Array<String>, env: Map<String, String>): Output

expect val userHomeDir: String?
