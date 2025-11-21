package com.klyx.extension.internal

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission

actual fun makeFileExecutable(path: String): Result<Unit, String> {
    return try {
        val path = Paths.get(path)
        val perms = Files.getPosixFilePermissions(path).toMutableSet()
        perms.add(PosixFilePermission.OWNER_EXECUTE)
        Files.setPosixFilePermissions(path, perms)
        Ok(Unit)
    } catch (e: Exception) {
        Err("Failed to make file executable: $path - ${e.message}")
    }
}

actual fun findBinary(binaryName: String): String? {
    return null
}

actual fun getenv(name: String): String? = System.getenv(name)

actual fun getenv(): Map<String, String> = System.getenv()

actual fun executeCommand(
    command: String,
    args: Array<String>,
    env: Map<String, String>
): Output {
    TODO("Not yet implemented")
}

actual val userHomeDir: String?
    get() = System.getProperty("user.home")

actual val rootDir: String = "/"
