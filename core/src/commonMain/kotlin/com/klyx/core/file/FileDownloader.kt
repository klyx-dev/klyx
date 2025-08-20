package com.klyx.core.file

import com.klyx.core.httpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Clock
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.use

data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val percentage: Float = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toFloat() else 0f,
    val isCompleted: Boolean = bytesDownloaded == totalBytes && totalBytes > 0,
    val downloadSpeed: Long = 0, // bytes per second
    val estimatedTimeRemaining: Long = 0 // seconds
)

sealed class DownloadResult {
    data class Success(val filePath: String, val size: Long) : DownloadResult()
    data class Error(val exception: Throwable) : DownloadResult()
    data object Cancelled : DownloadResult()
}

class FileDownloader(
    private val client: HttpClient = httpClient
) : AutoCloseable {
    companion object {
        private const val DEFAULT_BUFFER_SIZE = 1024 * 8
    }

    private val fs = FileSystem.SYSTEM
    private var downloadJobs = mutableMapOf<String, Job>()

    @Suppress("CyclomaticComplexMethod")
    fun downloadWithProgress(
        url: String,
        destinationPath: String,
        bufferSize: Int = DEFAULT_BUFFER_SIZE
    ): Flow<DownloadProgress> = flow {
        var lastEmitTime: Long
        val path = destinationPath.toPath(normalize = true)

        try {
            path.parent?.let { parent ->
                if (!fs.exists(parent)) fs.createDirectories(parent)
            }

            if (fs.exists(path)) fs.delete(path)

            val response = client.get(url)
            if (response.status.isSuccess()) {
                val length = response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: -1L
                val channel = response.bodyAsChannel()

                var totalBytesRead = 0L
                val buffer = ByteArray(bufferSize)
                val startTime = Clock.System.now().toEpochMilliseconds()
                lastEmitTime = startTime

                // initial progress
                emit(DownloadProgress(0, length))

                while (!channel.isClosedForRead) {
                    val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                    if (bytesRead <= 0) break

                    val chunk = buffer.copyOf(bytesRead)
                    if (totalBytesRead == 0L) {
                        fs.write(path) { write(chunk) }
                    } else {
                        fs.appendingSink(path).buffer().use { sink -> sink.write(chunk) }
                    }

                    totalBytesRead += bytesRead

                    val currentTime = Clock.System.now().toEpochMilliseconds()
                    // emit progress every 100ms to avoid too frequent updates
                    if (currentTime - lastEmitTime >= 100 || totalBytesRead == length) {
                        val elapsed = (currentTime - startTime) / 1000.0
                        val speed = if (elapsed > 0) (totalBytesRead / elapsed).toLong() else 0L
                        val remaining = if (speed > 0 && length > 0) {
                            ((length - totalBytesRead) / speed)
                        } else 0L

                        emit(
                            DownloadProgress(
                                bytesDownloaded = totalBytesRead,
                                totalBytes = length,
                                downloadSpeed = speed,
                                estimatedTimeRemaining = remaining
                            )
                        )
                        lastEmitTime = currentTime
                    }
                }
            } else {
                @Suppress("TooGenericExceptionThrown")
                throw Exception("Failed to download file: ${response.status}")
            }
        } catch (e: Exception) {
            // clean up partial file on error
            if (fs.exists(path)) fs.delete(path)
            throw e
        }
    }.flowOn(Dispatchers.IO)


    /**
     * Downloads a file and returns the result
     */
    suspend fun download(
        url: String,
        destinationPath: String,
        onProgress: ((DownloadProgress) -> Unit)? = null
    ): DownloadResult {
        val downloadId = "$url->$destinationPath"

        return try {
            val job = coroutineScope {
                async {
                    downloadWithProgress(url, destinationPath)
                        .onEach { progress -> onProgress?.invoke(progress) }
                        .last() // wait for completion
                }
            }

            downloadJobs[downloadId] = job
            job.await()

            val path = destinationPath.toPath(true)
            DownloadResult.Success(path.toString(), fs.metadata(path).size ?: 0L)
        } catch (_: CancellationException) {
            destinationPath.toPath(true).let { path ->
                if (fs.exists(path)) fs.delete(path)
            }
            DownloadResult.Cancelled
        } catch (e: Exception) {
            destinationPath.toPath(true).let { path ->
                if (fs.exists(path)) fs.delete(path)
            }
            DownloadResult.Error(e)
        } finally {
            downloadJobs.remove(downloadId)
        }
    }

    /**
     * Downloads multiple files concurrently.
     *
     * @param downloads A list of pairs, where each pair contains the URL to download from and the destination path to save the file.
     * @param concurrency The maximum number of concurrent downloads. Defaults to 3.
     * @return A Flow that emits a Triple for each download. The Triple contains:
     * - The original index of the download in the input list.
     * - The URL of the downloaded file.
     * - The [DownloadResult] ([DownloadResult.Success], [DownloadResult.Error], or [DownloadResult.Cancelled]).
     * The downloads are processed in chunks based on the specified concurrency.
     */
    fun downloadMultiple(
        downloads: List<Pair<String, String>>,
        concurrency: Int = 3
    ): Flow<Triple<Int, String, DownloadResult>> = flow {
        downloads.mapIndexed { index, (url, path) ->
            coroutineScope {
                async {
                    val result = download(url, path)
                    Triple(index, url, result)
                }
            }
        }.chunked(concurrency).forEach { chunk ->
            chunk.awaitAll().forEach { result -> emit(result) }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Cancels a specific download
     */
    fun cancelDownload(url: String, destinationPath: String) {
        val downloadId = "$url->$destinationPath"
        downloadJobs[downloadId]?.cancel()
    }

    /**
     * Cancels all active downloads
     */
    fun cancelAllDownloads() {
        downloadJobs.values.forEach { it.cancel() }
        downloadJobs.clear()
    }

    /**
     * Gets the number of active downloads
     */
    fun getActiveDownloadsCount(): Int = downloadJobs.size

    /**
     * Closes the HTTP client and cancels all downloads
     */
    override fun close() {
        cancelAllDownloads()
        client.close()
    }
}

/**
 * Extension function for [String] to download a file from a URL to a specified path.
 *
 * @param path The destination path where the file will be saved.
 */
suspend fun String.downloadTo(
    path: String,
    onProgress: ((DownloadProgress) -> Unit)? = null
): DownloadResult {
    val downloader = FileDownloader()
    return try {
        downloader.download(this, path, onProgress)
    } finally {
        downloader.close()
    }
}

fun String.downloadToWithProgress(path: String): Flow<DownloadProgress> {
    val downloader = FileDownloader()
    return downloader.downloadWithProgress(this, path).onCompletion { downloader.close() }
}

