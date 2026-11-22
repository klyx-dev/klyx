package com.klyx.lsp

import com.klyx.lsp.capabilities.ServerCapabilities
import com.klyx.lsp.types.fold
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class DiagnosticProviderTest : FunSpec({
    test("deserializes rust-analyzer pull diagnostic capability") {
        val capabilities = Json.decodeFromString<ServerCapabilities>(
            """
            {
              "diagnosticProvider": {
                "identifier": "rust-analyzer",
                "interFileDependencies": true,
                "workspaceDiagnostics": false
              }
            }
            """.trimIndent(),
        )

        capabilities.diagnosticProvider?.fold(
            left = { it.identifier },
            right = { it.identifier },
        ) shouldBe "rust-analyzer"
    }
})
