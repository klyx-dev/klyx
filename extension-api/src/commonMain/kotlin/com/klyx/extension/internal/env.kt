package com.klyx.extension.internal

import com.github.michaelbull.result.Result
import com.klyx.core.process.systemProcess

expect suspend fun getenv(name: String): String?
expect suspend fun getenv(): Map<String, String>

expect fun makeFileExecutable(path: String): Result<Unit, String>

suspend fun findBinary(binaryName: String): String? {
    return systemProcess(arrayOf("which", binaryName))
        .output()
        .stdout
        .ifBlank { null }
}

suspend fun executeCommand(command: String, args: List<String>, env: Map<String, String>): Output {
    systemProcess(command) {
        args(args)
        environment { putAll(env) }
    }.output().let { output ->
        return Output(
            output.processInfo.exitCode,
            output.stdout,
            output.stderr
        )
    }
}

expect val userHomeDir: String?
expect val rootDir: String
