package com.klyx.core.file

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

suspend fun HttpClient.downloadFile(
    url: String,
    outputPath: String,
    onProgress: (downloaded: Long, total: Long?) -> Unit = { _, _ -> }
) {
    withContext(Dispatchers.IO) {
        val response = get(url)
        val contentLength = response.contentLength()
        val channel = response.bodyAsChannel()

        val sink = SystemFileSystem.sink(Path(outputPath)).buffered()

        var downloaded = 0L
        val buffer = ByteArray(8 * 1024)

        while (!channel.isClosedForRead) {
            val read = channel.readAvailable(buffer, 0, buffer.size)
            if (read == -1) break
            sink.write(buffer, 0, read)
            downloaded += read
            onProgress(downloaded, contentLength)
        }

        sink.flush()
        sink.close()
    }
}
