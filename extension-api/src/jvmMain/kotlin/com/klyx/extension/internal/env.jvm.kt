package com.klyx.extension.internal

import io.itsvks.anyhow.anyhow
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission

actual fun makeFileExecutable(path: String) = anyhow {
    try {
        val path = Paths.get(path)
        val perms = Files.getPosixFilePermissions(path).toMutableSet()
        perms.add(PosixFilePermission.OWNER_EXECUTE)
        Files.setPosixFilePermissions(path, perms).let { }
    } catch (e: Exception) {
        bail("Failed to make file executable: $path - ${e.message}")
    }
}

actual suspend fun getenv(name: String): String? = System.getenv(name)
actual suspend fun getenv(): Map<String, String> = System.getenv()

actual val userHomeDir: String?
    get() = System.getProperty("user.home")

actual val rootDir: String = "/"
