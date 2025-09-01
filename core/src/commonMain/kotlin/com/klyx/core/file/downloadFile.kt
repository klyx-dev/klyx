package com.klyx.core.file

import com.klyx.core.httpClient
import io.ktor.client.content.ProgressListener
import io.ktor.client.plugins.onDownload
import io.ktor.client.plugins.timeout
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.asByteWriteChannel
import io.ktor.utils.io.copyTo
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.time.Duration.Companion.minutes

suspend fun downloadFile(
    url: String,
    outputPath: String,
    onDownload: ProgressListener? = null,
    onComplete: suspend () -> Unit = {}
) {
    val sink = SystemFileSystem.sink(Path(outputPath))

    httpClient.prepareGet(urlString = url) {
        val timeout = 30.minutes.inWholeMilliseconds

        timeout {
            requestTimeoutMillis = timeout
            connectTimeoutMillis = timeout
            socketTimeoutMillis = timeout
        }

        onDownload(onDownload)
    }.execute { response ->
        if (response.status.value in 200..299) {
            val channel = response.bodyAsChannel()
            channel.copyTo(sink.asByteWriteChannel())
            onComplete()
        } else {
            throw RuntimeException("Failed to download file. HTTP Status: ${response.status.value}")
        }
    }
}
