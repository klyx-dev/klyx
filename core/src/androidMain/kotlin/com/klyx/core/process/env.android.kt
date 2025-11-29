package com.klyx.core.process

import com.klyx.core.terminal.currentUser
import com.klyx.core.withAndroidContext

actual suspend fun getenv(name: String): String? {
    return try {
        systemProcess("printenv", name)
            .output()
            .stdout
            .ifBlank { error("$name not found in environment") }
    } catch (_: Throwable) {
        System.getenv(name)
    }
}

actual suspend fun getenv(): Map<String, String> {
    return try {
        systemProcess("printenv")
            .output()
            .stdout
            .lineSequence()
            .associate {
                val (key, value) = it.split("=")
                key to value
            }
            .ifEmpty { error("No environment variables found") }
    } catch (_: Throwable) {
        System.getenv().orEmpty()
    }
}

actual val systemUserName: String
    get() = withAndroidContext { currentUser } ?: error(
        """
        |Unable to resolve the current system user.
        |This likely means the Klyx Terminal is not installed on this Android device, or the application context has not been initialized correctly.
        |Please ensure the terminal is installed and user name set.
        """.trimMargin()
    )
