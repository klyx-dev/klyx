package com.klyx.lsp.server

import com.klyx.lsp.Diagnostic
import com.klyx.lsp.DidOpenTextDocumentParams
import com.klyx.lsp.DocumentDiagnosticParams
import com.klyx.lsp.InitializeParams
import com.klyx.lsp.InitializedParams
import com.klyx.lsp.LogMessageParams
import com.klyx.lsp.MessageActionItem
import com.klyx.lsp.PublishDiagnosticsParams
import com.klyx.lsp.ShowMessageParams
import com.klyx.lsp.ShowMessageRequestParams
import com.klyx.lsp.TextDocumentIdentifier
import com.klyx.lsp.TextDocumentItem
import com.klyx.lsp.capabilities.defaultClientCapabilities
import com.klyx.lsp.process.launchLspProcess
import com.klyx.lsp.types.LSPArray
import com.klyx.lsp.types.LSPObject
import com.klyx.lsp.types.OneOf
import com.klyx.lsp.types.fold
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldNotBe
import io.matthewnelson.kmp.process.Process
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

class ProcessTransportTest : FunSpec({
    test("test rust-analyzer LSP communication") {
        val client = object : LanguageClient {
            override suspend fun publishDiagnostics(params: PublishDiagnosticsParams) {}
            override suspend fun showMessage(params: ShowMessageParams) {}
            override suspend fun showMessageRequest(params: ShowMessageRequestParams): MessageActionItem? = null
            override suspend fun logMessage(params: LogMessageParams) {}
            override suspend fun telemetryEvent(params: OneOf<LSPObject, LSPArray>) {}
        }

        val builder = Process.Builder(command = "rust-analyzer")
        val scope = CoroutineScope(Dispatchers.Default)
        val lspServer = launchLspProcess(builder, client, scope)

        val initResult = lspServer.initialize(
            InitializeParams(
                capabilities = defaultClientCapabilities(),
                rootUri = null,
            )
        )
        initResult.capabilities shouldNotBe null

        lspServer.initialized(InitializedParams)

        val uri = "file:///tmp/test.rs"
        lspServer.textDocument.didOpen(
            DidOpenTextDocumentParams(
                textDocument = TextDocumentItem(
                    uri = uri,
                    languageId = "rust",
                    version = 1,
                    text = "fn main() {\n    let value: i32 = \"wrong\";\n}\n",
                )
            )
        )

        val report = withTimeoutOrNull(15_000.milliseconds) {
            lspServer.textDocument.diagnostic(
                DocumentDiagnosticParams(
                    textDocument = TextDocumentIdentifier(uri),
                    identifier = "rust-analyzer",
                )
            )
        }
        report shouldNotBe null
        val diagnostics: List<Diagnostic> = report!!.fold(
            left = { it.items },
            right = { emptyList() },
        )
        println("rust-analyzer returned ${diagnostics.size} diagnostics")
        lspServer.close()
    }
})
