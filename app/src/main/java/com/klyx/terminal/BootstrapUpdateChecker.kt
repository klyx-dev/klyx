package com.klyx.terminal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object BootstrapUpdateChecker {
    suspend fun latestVersion(): String = withContext(Dispatchers.IO) {
        val connection = URL("https://github.com/klyx-dev/klyx-bootstrap/releases/latest")
            .openConnection() as HttpURLConnection

        connection.instanceFollowRedirects = false

        val location = connection.getHeaderField("Location") ?: error("Missing redirect")
        location.substringAfterLast("/")
    }
}
