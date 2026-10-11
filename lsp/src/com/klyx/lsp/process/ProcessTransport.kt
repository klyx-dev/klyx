package com.klyx.lsp.process

import com.klyx.lsp.server.LanguageClient
import com.klyx.lsp.server.LanguageServer
import com.klyx.lsp.server.createLanguageServer
import com.klyx.lsp.transport.LspServerConnection
import io.matthewnelson.kmp.process.Process
import io.matthewnelson.kmp.process.Stdio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.io.RawSink
import kotlinx.io.RawSource

data class ProcessStreams(val source: RawSource, val sink: RawSink)

expect fun Process.asRawStreams(): ProcessStreams

class LspProcessServer(
    val process: Process,
    val server: LanguageServer,
) : LspServerConnection, LanguageServer by server {
    override fun close() {
        try {
            process.destroy()
        } catch (_: Throwable) {
        }
    }
}

suspend fun launchLspProcess(
    builder: Process.Builder,
    client: LanguageClient,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
): LspProcessServer {
    val process = builder
        .stdin(Stdio.Pipe)
        .stdout(Stdio.Pipe)
        .stderr(Stdio.Pipe)
        .createProcessAsync()

    (val source, val sink) = process.asRawStreams()
    val server = createLanguageServer(client, source, sink)
    return LspProcessServer(process, server)
}
