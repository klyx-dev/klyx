package com.klyx.lsp.python

import com.klyx.api.lsp.LanguageServerProvider
import com.klyx.api.system.StdinSource
import com.klyx.api.system.StdioDest
import com.klyx.api.system.command
import com.klyx.lsp.server.LanguageClient
import com.klyx.lsp.server.LanguageServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PythonLspProvider : LanguageServerProvider {
    override suspend fun startServer(client: LanguageClient): LanguageServer = withContext(Dispatchers.IO) {
        val process = command("pylsp")
            .stdin(StdinSource.Pipe)
            .stdout(StdioDest.Capture)
            .spawn()

        LanguageServer(
            client = client,
            stdout = process.stdout,
            stdin = process.stdin
        )
    }
}
