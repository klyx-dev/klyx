package com.klyx.core.file

import com.klyx.core.httpClient
import io.ktor.client.content.ProgressListener
import io.ktor.client.plugins.onDownload
import io.ktor.client.plugins.timeout
import io.ktor.client.request.head
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.asByteWriteChannel
import io.ktor.utils.io.copyTo
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.time.Duration.Companion.minutes

suspend inline fun downloadFile(
    url: String,
    outputPath: String,
    onDownload: ProgressListener? = null,
    noinline onComplete: suspend () -> Unit = {}
) {
    val sink = SystemFileSystem.sink(Path(outputPath))

    httpClient.prepareGet(urlString = url) {
        val timeout = 10.minutes.inWholeMilliseconds

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
            throw RuntimeException("Failed to download file. HTTP Status: ${response.status}")
        }
    }
}

data class DownloadableFile(val url: String, val outputPath: String) {
    suspend inline fun download(
        noinline onComplete: suspend () -> Unit = {},
        onDownload: ProgressListener? = null
    ) = withContext(Dispatchers.IO) {
        downloadFile(url, outputPath, onDownload, onComplete)
    }
}

private fun DownloadableFile.asKxFile() = KxFile(outputPath)

suspend fun Collection<DownloadableFile>.downloadAll(
    concurrency: Int = 4,
    onFileProgress: suspend (file: KxFile, sent: Long, total: Long?) -> Unit = { _, _, _ -> },
    onTotalProgress: suspend (bytesDownloaded: Long, totalBytes: Long?) -> Unit = { _, _ -> },
    onComplete: suspend (KxFile) -> Unit = {},
    onError: suspend (file: KxFile, exception: Throwable) -> Unit = { _, _ -> },
    onAllComplete: suspend () -> Unit = {}
) = supervisorScope {
    val semaphore = Semaphore(concurrency)
    val totalBytes = mapNotNull { getContentLength(it.url) }.sum()
    val downloaded = atomic(0L)

    map { file ->
        launch {
            semaphore.withPermit {
                try {
                    file.download(
                        onDownload = { sent, total ->
                            onFileProgress(file.asKxFile(), sent, total)
                            downloaded.addAndGet(sent)
                            onTotalProgress(downloaded.value, totalBytes.takeIf { it > 0 })
                        },
                        onComplete = { onComplete(file.asKxFile()) }
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    onError(file.asKxFile(), e)
                }
            }
        }
    }.joinAll()

    onAllComplete()
}

suspend fun getContentLength(url: String) = runCatching { httpClient.head(url).contentLength() }.getOrNull()
