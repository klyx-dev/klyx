package com.klyx.lsp.server

import com.klyx.lsp.server.internal.createLanguageServerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.io.RawSink
import kotlinx.io.RawSource

fun createLanguageServer(client: LanguageClient, `in`: RawSource, out: RawSink): LanguageServer {
    val scope = CoroutineScope(Dispatchers.Default)
    val impl = createLanguageServerImpl(scope, client, `in`, out)
    return impl
}
