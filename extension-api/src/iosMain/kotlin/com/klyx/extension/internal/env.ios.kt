package com.klyx.extension.internal

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.matthewnelson.kmp.process.Process
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.Foundation.NSProcessInfo
import platform.posix.S_IXUSR
import platform.posix.chmod

actual fun getenv(): Map<String, String> {
    return NSProcessInfo.processInfo.environment
        .mapKeys { (key, _) -> key.toString() }
        .mapValues { (_, value) -> value.toString() }
}

@OptIn(ExperimentalForeignApi::class)
actual fun getenv(name: String) = platform.posix.getenv(name)?.toKString()

actual fun makeFileExecutable(path: String): Result<Unit, String> {
    val res = chmod(path, S_IXUSR.toUShort())
    return if (res == 0) {
        Ok(Unit)
    } else {
        Err("Failed to make file executable: $path")
    }
}

actual fun findBinary(binaryName: String): String? {
    return null
}

actual fun executeCommand(
    command: String,
    args: Array<String>,
    env: Map<String, String>
) = Process.Builder(command = command)
    .args(args.asList())
    .environment { putAll(env) }
    .useSpawn { p ->
        val stdout = StringBuilder()
        val stderr = StringBuilder()

        p.stderrFeed { stderr.append(it) }
        p.stdoutFeed { stdout.append(it) }

        val exitCode = p.waitFor()

        Output(
            status = exitCode,
            stdout = stdout.toString(),
            stderr = stderr.toString()
        )
    }

actual val userHomeDir: String?
    get() = TODO("Not yet implemented")

actual val rootDir: String
    get() = TODO("Not yet implemented")
